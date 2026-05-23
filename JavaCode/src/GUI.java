import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Main Swing window for the project planner.
 *
 * This class connects the main GUI panels and listens for Blackboard changes.
 *
 * @author Eman Castilo Hernandez
 * @version 1.0
 */
public class GUI extends JFrame implements BlackboardObserver {

    private final Blackboard blackboard;
    private final WorkspacePanel workspacePanel;
    private final InfoPanel infoPanel;
    private final AIPanel aiPanel;
    private final JLabel statusLabel = new JLabel("Ready");

    private AppController appController = new AppController() { };

    /**
     * @param blackboard shared model; typically created once in {@link Main#main(String[])}
     */
    public GUI(Blackboard blackboard) {
        installLookAndFeel();

        this.blackboard = Objects.requireNonNull(blackboard, "blackboard");
        this.blackboard.addObserver(this);

        configureFrame();

        this.workspacePanel = new WorkspacePanel(blackboard, this::setStatus, this::refreshContextPanels);
        this.infoPanel = new InfoPanel();
        this.aiPanel = new AIPanel(() -> appController, workspacePanel::getSelectedStory, this::setStatus);

        add(createToolbar(), BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);

        blackboardChanged();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void setAppController(AppController appController) {
        this.appController = Objects.requireNonNull(appController, "appController");
    }

    private void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                 | UnsupportedLookAndFeelException ignored) {
            // Keep the default look and feel if the system one is unavailable.
        }
    }

    private void configureFrame() {
        setTitle("CSC 307 Project Planner");
        setSize(1100, 650);
        setMinimumSize(new Dimension(900, 500));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
    }

    private JComponent createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Taiga API integration — Joseph Carl Santos
        JButton connectTaigaButton = new JButton("Connect to Taiga");
        JButton refreshButton = new JButton("Refresh View");

        connectTaigaButton.addActionListener(e -> runTaigaConnection());
        refreshButton.addActionListener(e -> blackboardChanged());

        toolbar.add(connectTaigaButton);
        toolbar.add(refreshButton);

        return toolbar;
    }

    private JComponent createMainContent() {
        JTabbedPane rightTabs = new JTabbedPane();
        rightTabs.addTab("Info", infoPanel);
        rightTabs.addTab("AI Panel", aiPanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, workspacePanel, rightTabs);
        splitPane.setResizeWeight(0.65);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        return splitPane;
    }

    private JComponent createStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        panel.add(statusLabel, BorderLayout.WEST);
        return panel;
    }

    private void runTaigaConnection() {
        setStatus("Connecting to Taiga...");
        appController.connectToTaiga(this, blackboard);
        // TaigaAppController finishes async; refresh when blackboard notifies observers.
    }

    @Override
    public void blackboardChanged() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::blackboardChanged);
            return;
        }

        workspacePanel.refreshFromBlackboard();
        refreshContextPanels();
    }

    private void refreshContextPanels() {
        Project selectedProject = workspacePanel.getSelectedProject();
        Story selectedStory = workspacePanel.getSelectedStory();
        Task selectedTask = workspacePanel.getSelectedTask();

        infoPanel.display(blackboard, selectedProject, selectedStory, selectedTask);
        aiPanel.updateSelection(selectedStory);
    }

    private void showError(String title, RuntimeException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), title, JOptionPane.ERROR_MESSAGE);
        setStatus(title + ".");
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    @Override
    public void dispose() {
        blackboard.removeObserver(this);
        super.dispose();
    }
}
