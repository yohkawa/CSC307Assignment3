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

    private AppController appController = new AppController() { };

    private WorkspacePanel workspacePanel;
    private InfoPanel infoPanel;
    private AIPanel aiPanel;

    private final JLabel statusLabel = new JLabel("Ready");

    public GUI(Blackboard blackboard) {
        installLookAndFeel();

        this.blackboard = Objects.requireNonNull(blackboard, "blackboard");
        this.blackboard.addObserver(this);

        configureFrame();
        createPanels();

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
            // Keep default look and feel if the system one is unavailable.
        }
    }

    private void configureFrame() {
        setTitle("CSC 307 Project Planner");
        setSize(1100, 650);
        setMinimumSize(new Dimension(900, 500));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
    }

    private void createPanels() {
        workspacePanel = new WorkspacePanel(
                blackboard,
                this::setStatus,
                this::updateSelectionPanels
        );

        infoPanel = new InfoPanel(blackboard);

        aiPanel = new AIPanel(
                () -> appController,
                workspacePanel::getSelectedStory,
                this::setStatus
        );
    }

    private JComponent createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());

        JLabel titleLabel = new JLabel("CSC 307 Project Planner");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton connectTaigaButton = new JButton("Connect to Taiga");
        JButton connectGroqButton = new JButton("Connect to Groq");
        JButton refreshButton = new JButton("Refresh View");

        connectTaigaButton.addActionListener(e -> runTaigaConnection());
        connectGroqButton.addActionListener(e -> runGroqConnection());
        refreshButton.addActionListener(e -> blackboardChanged());

        buttonPanel.add(connectTaigaButton);
        buttonPanel.add(connectGroqButton);
        buttonPanel.add(refreshButton);

        toolbar.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        toolbar.add(titleLabel, BorderLayout.WEST);
        toolbar.add(buttonPanel, BorderLayout.EAST);

        return toolbar;
    }

    private JComponent createMainContent() {
        JTabbedPane rightTabs = new JTabbedPane();
        rightTabs.addTab("Info", infoPanel);
        rightTabs.addTab("AI", aiPanel);

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
        try {
            appController.connectToTaiga(this, blackboard);
            setStatus("Taiga connection action completed.");
            blackboardChanged();
        } catch (RuntimeException ex) {
            showError("Taiga connection failed", ex);
        }
    }

    private void runGroqConnection() {
        try {
            appController.connectToGroq(this);
            setStatus("Groq connection action completed.");
        } catch (RuntimeException ex) {
            showError("Groq connection failed", ex);
        }
    }

    @Override
    public void blackboardChanged() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::blackboardChanged);
            return;
        }

        workspacePanel.refreshFromBlackboard();
        updateSelectionPanels();
    }

    private void updateSelectionPanels() {
        Project selectedProject = workspacePanel.getSelectedProject();
        Story selectedStory = workspacePanel.getSelectedStory();
        Task selectedTask = workspacePanel.getSelectedTask();

        infoPanel.updateSelection(selectedProject, selectedStory, selectedTask);
        aiPanel.updateSelection(selectedStory);
    }

    private void showError(String title, RuntimeException ex) {
        JOptionPane.showMessageDialog(
                this,
                ex.getMessage(),
                title,
                JOptionPane.ERROR_MESSAGE
        );

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