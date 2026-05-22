import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Three-column Swing view for the blackboard. Implements {@link BlackboardObserver} so lists stay in
 * sync when the model changes. Layout matches the original assignment structure (one column per
 * entity); lists use single selection and safe selection callbacks.
 */
public class GUI extends JFrame implements BlackboardObserver {

    private final Blackboard blackboard;

    private final DefaultListModel<Project> projectModel = new DefaultListModel<>();
    private final DefaultListModel<Story> storyModel = new DefaultListModel<>();
    private final DefaultListModel<Task> taskModel = new DefaultListModel<>();

    private final JList<Project> projectList = new JList<>(projectModel);
    private final JList<Story> storyList = new JList<>(storyModel);
    private final JList<Task> taskList = new JList<>(taskModel);

    /**
     * @param blackboard shared model; typically created once in {@link Main#main(String[])} and passed here.
     */
    public GUI(Blackboard blackboard) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                 | UnsupportedLookAndFeelException ignored) {
            // Keep default LAF if the platform one is not available.
        }

        this.blackboard = Objects.requireNonNull(blackboard, "blackboard");
        this.blackboard.addObserver(this);

        projectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        storyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        setTitle("Blackboard GUI");
        setSize(800, 400);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new GridLayout(1, 3));

        add(createProjectPanel());
        add(createStoryPanel());
        add(createTaskPanel());

        projectList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshStories();
            }
        });
        storyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshTasks();
            }
        });

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createProjectPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JButton addButton = new JButton("Add Project");
        JButton deleteButton = new JButton("Delete Project");

        addButton.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Project name:");
            if (name != null && !name.trim().isEmpty()) {
                blackboard.createProject(name.trim());
            }
        });

        deleteButton.addActionListener(e -> {
            Project selectedProject = projectList.getSelectedValue();

            if (selectedProject == null) {
                JOptionPane.showMessageDialog(this, "Select a project first.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Delete project: " + selectedProject.getName() + "?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                blackboard.deleteProjectById(selectedProject.getId());
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);

        panel.add(new JLabel("Projects", SwingConstants.CENTER), BorderLayout.NORTH);
        panel.add(new JScrollPane(projectList), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createStoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JButton addButton = new JButton("Add Story");
        JButton deleteButton = new JButton("Delete Story");

        addButton.addActionListener(e -> {
            Project selectedProject = projectList.getSelectedValue();

            if (selectedProject == null) {
                JOptionPane.showMessageDialog(this, "Select a project first.");
                return;
            }

            String title = JOptionPane.showInputDialog(this, "Story title:");
            if (title != null && !title.trim().isEmpty()) {
                selectedProject.createStory(title.trim());
            }
        });

        deleteButton.addActionListener(e -> {
            Project selectedProject = projectList.getSelectedValue();
            Story selectedStory = storyList.getSelectedValue();

            if (selectedProject == null || selectedStory == null) {
                JOptionPane.showMessageDialog(this, "Select a story first.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Delete story: " + selectedStory.getTitle() + "?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                selectedProject.deleteStoryById(selectedStory.getId());
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);

        panel.add(new JLabel("Stories", SwingConstants.CENTER), BorderLayout.NORTH);
        panel.add(new JScrollPane(storyList), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createTaskPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JButton addButton = new JButton("Add Task");
        JButton deleteButton = new JButton("Delete Task");

        addButton.addActionListener(e -> {
            Story selectedStory = storyList.getSelectedValue();

            if (selectedStory == null) {
                JOptionPane.showMessageDialog(this, "Select a story first.");
                return;
            }

            String title = JOptionPane.showInputDialog(this, "Task title:");
            if (title != null && !title.trim().isEmpty()) {
                selectedStory.createTask(title.trim());
            }
        });

        deleteButton.addActionListener(e -> {
            Story selectedStory = storyList.getSelectedValue();
            Task selectedTask = taskList.getSelectedValue();

            if (selectedStory == null || selectedTask == null) {
                JOptionPane.showMessageDialog(this, "Select a task first.");
                return;
            }

            selectedStory.deleteTaskById(selectedTask.getId());
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);

        panel.add(new JLabel("Tasks", SwingConstants.CENTER), BorderLayout.NORTH);
        panel.add(new JScrollPane(taskList), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    @Override
    public void blackboardChanged() {
        refreshProjects();
        refreshStories();
        refreshTasks();
    }

    private void refreshProjects() {
        Project selected = projectList.getSelectedValue();

        projectModel.clear();
        for (Project project : blackboard.getProjects()) {
            projectModel.addElement(project);
        }

        if (selected != null) {
            projectList.setSelectedValue(selected, true);
        }
    }

    private void refreshStories() {
        Project selectedProject = projectList.getSelectedValue();
        Story selectedStory = storyList.getSelectedValue();

        storyModel.clear();

        if (selectedProject != null) {
            for (Story story : selectedProject.getStories()) {
                storyModel.addElement(story);
            }
        }

        if (selectedStory != null) {
            storyList.setSelectedValue(selectedStory, true);
        }
    }

    private void refreshTasks() {
        Story selectedStory = storyList.getSelectedValue();
        Task selectedTask = taskList.getSelectedValue();

        taskModel.clear();

        if (selectedStory != null) {
            for (Task task : selectedStory.getTasks()) {
                taskModel.addElement(task);
            }
        }

        if (selectedTask != null) {
            taskList.setSelectedValue(selectedTask, true);
        }
    }
}
