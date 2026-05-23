import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Main Swing window for the project planner.
 *
 * This class connects the main GUI panels and listens for Blackboard changes.
 *
 * @author Eman Castilo Hernandez
 * @version 1.1
 */
public class GUI extends JFrame implements BlackboardObserver {

    private final Blackboard blackboard;
    private final WorkspacePanel workspacePanel;
    private final InfoPanel infoPanel;
    private final AIPanel aiPanel;
    private final JLabel statusLabel = new JLabel("Ready");

    private AppController appController = new AppController() { };

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

        JButton connectTaigaButton = new JButton("Connect to Taiga");
        JButton connectGroqButton = new JButton("Connect to Groq");
        JButton refreshButton = new JButton("Refresh View");

        connectTaigaButton.addActionListener(e -> runTaigaConnection());
        connectGroqButton.addActionListener(e -> runGroqConnection());
        refreshButton.addActionListener(e -> blackboardChanged());

        toolbar.add(connectTaigaButton);
        toolbar.add(connectGroqButton);
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
    }

    private void runGroqConnection() {
        setStatus("Connecting to Groq...");
        appController.connectToGroq(this);
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

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    @Override
    public void dispose() {
        blackboard.removeObserver(this);
        super.dispose();
    }
}