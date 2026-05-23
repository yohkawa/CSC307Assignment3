import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Panel for managing project, user story, and task list. It also handles
 * add/delete actions and hides story/task panels when not in use.
 *
 * @author Eman Castilo Hernandez
 * @version 1.0
 */
public final class WorkspacePanel extends JPanel {

    private static final int NO_SELECTION = -1;

    private final Blackboard blackboard;
    private final Consumer<String> statusUpdater;
    private final Runnable selectionChanged;

    private final DefaultListModel<Project> projectModel = new DefaultListModel<>();
    private final DefaultListModel<Story> storyModel = new DefaultListModel<>();
    private final DefaultListModel<Task> taskModel = new DefaultListModel<>();

    private final JList<Project> projectList = new JList<>(projectModel);
    private final JList<Story> storyList = new JList<>(storyModel);
    private final JList<Task> taskList = new JList<>(taskModel);

    private final JButton addStoryButton = new JButton("Add Story");
    private final JButton deleteStoryButton = new JButton("Delete Story");
    private final JButton addTaskButton = new JButton("Add Task");
    private final JButton deleteTaskButton = new JButton("Delete Task");

    private JPanel storyColumn;
    private JPanel taskColumn;

    private boolean refreshing;

    public WorkspacePanel(Blackboard blackboard, Consumer<String> statusUpdater, Runnable selectionChanged) {
        super(new GridBagLayout());

        this.blackboard = Objects.requireNonNull(blackboard, "blackboard");
        this.statusUpdater = Objects.requireNonNull(statusUpdater, "statusUpdater");
        this.selectionChanged = Objects.requireNonNull(selectionChanged, "selectionChanged");

        setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        configureLists();
        wireSelectionListeners();

        JPanel projectColumn = createProjectColumn();
        storyColumn = createStoryColumn();
        taskColumn = createTaskColumn();

        addColumn(projectColumn, 0);
        addColumn(storyColumn, 1);
        addColumn(taskColumn, 2);

        updateActionStates();
    }

    public Project getSelectedProject() {
        return projectList.getSelectedValue();
    }

    public Story getSelectedStory() {
        return storyList.getSelectedValue();
    }

    public Task getSelectedTask() {
        return taskList.getSelectedValue();
    }

    /**
     * Refreshes all list contents from the Blackboard while preserving selection by local ID.
     */
    public void refreshFromBlackboard() {
        int selectedProjectId = getSelectedProjectId();
        int selectedStoryId = getSelectedStoryId();
        int selectedTaskId = getSelectedTaskId();

        refreshing = true;
        refreshProjects(selectedProjectId);
        refreshStories(selectedStoryId);
        refreshTasks(selectedTaskId);
        refreshing = false;

        updateActionStates();
    }

    private void configureLists() {
        projectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        storyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    private void wireSelectionListeners() {
        projectList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !refreshing) {
                refreshStories(NO_SELECTION);
                refreshTasks(NO_SELECTION);
                updateActionStates();
                selectionChanged.run();
            }
        });

        storyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !refreshing) {
                refreshTasks(NO_SELECTION);
                updateActionStates();
                selectionChanged.run();
            }
        });

        taskList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !refreshing) {
                updateActionStates();
                selectionChanged.run();
            }
        });
    }

    private void addColumn(JPanel column, int gridX) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = gridX;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 4, 0, 4);

        add(column, constraints);
    }

    private JPanel createProjectColumn() {
        JPanel panel = createColumnPanel("Projects");

        JButton addProjectButton = new JButton("Add Project");
        JButton deleteProjectButton = new JButton("Delete Project");

        addProjectButton.addActionListener(e -> addProject());
        deleteProjectButton.addActionListener(e -> deleteSelectedProject());

        panel.add(new JScrollPane(projectList), BorderLayout.CENTER);
        panel.add(createButtonRow(addProjectButton, deleteProjectButton), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createStoryColumn() {
        JPanel panel = createColumnPanel("User Stories");

        addStoryButton.addActionListener(e -> addStory());
        deleteStoryButton.addActionListener(e -> deleteSelectedStory());

        panel.add(new JScrollPane(storyList), BorderLayout.CENTER);
        panel.add(createButtonRow(addStoryButton, deleteStoryButton), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createTaskColumn() {
        JPanel panel = createColumnPanel("Tasks");

        addTaskButton.addActionListener(e -> addTask());
        deleteTaskButton.addActionListener(e -> deleteSelectedTask());

        panel.add(new JScrollPane(taskList), BorderLayout.CENTER);
        panel.add(createButtonRow(addTaskButton, deleteTaskButton), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createColumnPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }

    private JPanel createButtonRow(JButton first, JButton second) {
        JPanel row = new JPanel(new GridLayout(1, 2, 4, 4));
        row.add(first);
        row.add(second);
        return row;
    }

    private void addProject() {
        String name = JOptionPane.showInputDialog(this, "Project name:");
        if (isBlank(name)) {
            return;
        }

        blackboard.createProject(name.trim());
        statusUpdater.accept("Project added: " + name.trim());
    }

    private void deleteSelectedProject() {
        Project selectedProject = getSelectedProject();
        if (selectedProject == null) {
            showSelectionMessage("Select a project first.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete project: " + selectedProject.getName() + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION && blackboard.deleteProjectById(selectedProject.getId())) {
            statusUpdater.accept("Project deleted.");
        }
    }

    private void addStory() {
        Project selectedProject = getSelectedProject();
        if (selectedProject == null) {
            showSelectionMessage("Select a project first.");
            return;
        }

        String title = JOptionPane.showInputDialog(this, "Story title:");
        if (isBlank(title)) {
            return;
        }

        selectedProject.createStory(title.trim());
        statusUpdater.accept("Story added to " + selectedProject.getName() + ".");
    }

    private void deleteSelectedStory() {
        Project selectedProject = getSelectedProject();
        Story selectedStory = getSelectedStory();

        if (selectedProject == null || selectedStory == null) {
            showSelectionMessage("Select a story first.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete story: " + selectedStory.getTitle() + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION && selectedProject.deleteStoryById(selectedStory.getId())) {
            statusUpdater.accept("Story deleted.");
        }
    }

    private void addTask() {
        Story selectedStory = getSelectedStory();
        if (selectedStory == null) {
            showSelectionMessage("Select a story first.");
            return;
        }

        String title = JOptionPane.showInputDialog(this, "Task title:");
        if (isBlank(title)) {
            return;
        }

        selectedStory.createTask(title.trim());
        statusUpdater.accept("Task added to " + selectedStory.getTitle() + ".");
    }

    private void deleteSelectedTask() {
        Story selectedStory = getSelectedStory();
        Task selectedTask = getSelectedTask();

        if (selectedStory == null || selectedTask == null) {
            showSelectionMessage("Select a task first.");
            return;
        }

        if (selectedStory.deleteTaskById(selectedTask.getId())) {
            statusUpdater.accept("Task deleted.");
        }
    }

    private void refreshProjects(int selectedProjectId) {
        projectModel.clear();

        for (Project project : blackboard.getProjects()) {
            projectModel.addElement(project);
        }

        selectProjectById(selectedProjectId);
    }

    private void refreshStories(int selectedStoryId) {
        Project selectedProject = getSelectedProject();
        storyModel.clear();

        if (selectedProject != null) {
            for (Story story : selectedProject.getStories()) {
                storyModel.addElement(story);
            }
        }

        selectStoryById(selectedStoryId);
    }

    private void refreshTasks(int selectedTaskId) {
        Story selectedStory = getSelectedStory();
        taskModel.clear();

        if (selectedStory != null) {
            for (Task task : selectedStory.getTasks()) {
                taskModel.addElement(task);
            }
        }

        selectTaskById(selectedTaskId);
    }

    private void updateActionStates() {
        boolean hasProject = getSelectedProject() != null;
        boolean hasStory = getSelectedStory() != null;
        boolean hasTask = getSelectedTask() != null;

        addStoryButton.setEnabled(hasProject);
        deleteStoryButton.setEnabled(hasStory);
        addTaskButton.setEnabled(hasStory);
        deleteTaskButton.setEnabled(hasTask);

        updateColumnVisibility();
    }

    private void updateColumnVisibility() {
        boolean hasProject = getSelectedProject() != null;
        boolean hasStory = getSelectedStory() != null;

        if (storyColumn != null) {
            storyColumn.setVisible(hasProject);
        }

        if (taskColumn != null) {
            taskColumn.setVisible(hasStory);
        }

        revalidate();
        repaint();
    }

    private int getSelectedProjectId() {
        Project project = getSelectedProject();
        return project == null ? NO_SELECTION : project.getId();
    }

    private int getSelectedStoryId() {
        Story story = getSelectedStory();
        return story == null ? NO_SELECTION : story.getId();
    }

    private int getSelectedTaskId() {
        Task task = getSelectedTask();
        return task == null ? NO_SELECTION : task.getId();
    }

    private void selectProjectById(int projectId) {
        if (projectId == NO_SELECTION) {
            projectList.clearSelection();
            return;
        }

        for (int i = 0; i < projectModel.size(); i++) {
            if (projectModel.getElementAt(i).getId() == projectId) {
                projectList.setSelectedIndex(i);
                return;
            }
        }

        projectList.clearSelection();
    }

    private void selectStoryById(int storyId) {
        if (storyId == NO_SELECTION) {
            storyList.clearSelection();
            return;
        }

        for (int i = 0; i < storyModel.size(); i++) {
            if (storyModel.getElementAt(i).getId() == storyId) {
                storyList.setSelectedIndex(i);
                return;
            }
        }

        storyList.clearSelection();
    }

    private void selectTaskById(int taskId) {
        if (taskId == NO_SELECTION) {
            taskList.clearSelection();
            return;
        }

        for (int i = 0; i < taskModel.size(); i++) {
            if (taskModel.getElementAt(i).getId() == taskId) {
                taskList.setSelectedIndex(i);
                return;
            }
        }

        taskList.clearSelection();
    }

    private void showSelectionMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Selection Required", JOptionPane.INFORMATION_MESSAGE);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}