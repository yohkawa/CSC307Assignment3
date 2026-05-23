import javax.swing.SwingUtilities;

import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * Application entry and optional command-line explorer for the blackboard hierarchy. The default
 * {@link #main} is the composition root: it constructs a single {@link Blackboard} and passes it
 * into the Swing {@link GUI}. The CLI is available via {@link #run()} when you construct
 * {@code Main} with the same shared instance.
 */
public final class Main {

    private static final String MENU_PROMPT = "Choose: ";

    private static final int MENU_EXIT = 0;
    private static final int MENU_CREATE_PROJECT = 1;
    private static final int MENU_OPEN_PROJECT = 2;
    private static final int MENU_LIST_PROJECTS = 3;

    private static final int PROJECT_BACK = 0;
    private static final int PROJECT_ADD_STORY = 1;
    private static final int PROJECT_OPEN_STORY = 2;
    private static final int PROJECT_LIST_STORIES = 3;

    private static final int STORY_BACK = 0;
    private static final int STORY_ADD_TASK = 1;
    private static final int STORY_LIST_TASKS = 2;
    private static final int STORY_DELETE_TASK = 3;

    private final Scanner scanner = new Scanner(System.in);
    private final Blackboard blackboard;

    public Main(Blackboard blackboard) {
        this.blackboard = Objects.requireNonNull(blackboard, "blackboard");
    }

    public static void main(String[] args) {
        Blackboard blackboard = new Blackboard();
        SwingUtilities.invokeLater(() -> {
            GUI gui = new GUI(blackboard);
            // Taiga API — Joseph Carl Santos
            gui.setAppController(new TaigaAppController());
        });
    }

    /**
     * Runs the interactive CLI on the calling thread (blocking). Useful when the GUI is not used.
     */
    public void run() {
        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readInt(MENU_PROMPT);
            if (choice == MENU_CREATE_PROJECT) {
                createProject();
            } else if (choice == MENU_OPEN_PROJECT) {
                openProject();
            } else if (choice == MENU_LIST_PROJECTS) {
                listAllProjects();
            } else if (choice == MENU_EXIT) {
                running = false;
            } else {
                System.out.println("Invalid choice.");
            }
        }
        System.out.println("Bye.");
    }

    private void printMainMenu() {
        System.out.println("\n=== Blackboard CLI ===");
        System.out.println(MENU_CREATE_PROJECT + ") Create project");
        System.out.println(MENU_OPEN_PROJECT + ") Open project");
        System.out.println(MENU_LIST_PROJECTS + ") List projects");
        System.out.println(MENU_EXIT + ") Exit");
    }

    private void createProject() {
        String name = readNonEmpty("Project name: ");
        Project project = blackboard.createProject(name);
        System.out.println("Created project #" + project.getId() + ": " + project.getName());
    }

    private void openProject() {
        listAllProjects();
        if (blackboard.getProjects().isEmpty()) {
            return;
        }

        int projectId = readInt("Project id: ");
        Project project = blackboard.getProjectById(projectId);
        if (project == null) {
            System.out.println("Project not found.");
            return;
        }

        projectMenu(project);
    }

    private void projectMenu(Project project) {
        boolean inProject = true;
        while (inProject) {
            System.out.println("\n--- Project: " + project.getName() + " ---");
            System.out.println(PROJECT_ADD_STORY + ") Add story");
            System.out.println(PROJECT_OPEN_STORY + ") Open story");
            System.out.println(PROJECT_LIST_STORIES + ") List stories");
            System.out.println(PROJECT_BACK + ") Back");

            int choice = readInt(MENU_PROMPT);
            if (choice == PROJECT_ADD_STORY) {
                addStory(project);
            } else if (choice == PROJECT_OPEN_STORY) {
                openStory(project);
            } else if (choice == PROJECT_LIST_STORIES) {
                listStories(project);
            } else if (choice == PROJECT_BACK) {
                inProject = false;
            } else {
                System.out.println("Invalid choice.");
            }
        }
    }

    private void addStory(Project project) {
        String title = readNonEmpty("Story title: ");
        Story story = project.createStory(title);
        System.out.println("Created story #" + story.getId() + ": " + story.getTitle());
    }

    private void openStory(Project project) {
        listStories(project);
        if (project.getStories().isEmpty()) {
            return;
        }

        int storyId = readInt("Story id: ");
        Story story = project.getStoryById(storyId);
        if (story == null) {
            System.out.println("Story not found.");
            return;
        }

        storyMenu(story);
    }

    private void storyMenu(Story story) {
        boolean inStory = true;
        while (inStory) {
            System.out.println("\n--- Story: " + story.getTitle() + " ---");
            System.out.println(STORY_ADD_TASK + ") Add task");
            System.out.println(STORY_LIST_TASKS + ") List tasks");
            System.out.println(STORY_DELETE_TASK + ") Delete task");
            System.out.println(STORY_BACK + ") Back");

            int choice = readInt(MENU_PROMPT);
            if (choice == STORY_ADD_TASK) {
                addTask(story);
            } else if (choice == STORY_LIST_TASKS) {
                listTasks(story);
            } else if (choice == STORY_DELETE_TASK) {
                deleteTask(story);
            } else if (choice == STORY_BACK) {
                inStory = false;
            } else {
                System.out.println("Invalid choice.");
            }
        }
    }

    private void addTask(Story story) {
        String title = readNonEmpty("Task title: ");
        Task task = story.createTask(title);
        System.out.println("Created task #" + task.getId() + ": " + task.getTitle());
    }

    private void deleteTask(Story story) {
        listTasks(story);
        if (story.getTasks().isEmpty()) {
            return;
        }

        int taskId = readInt("Task id to delete: ");
        boolean deleted = story.deleteTaskById(taskId);

        if (deleted) {
            System.out.println("Task deleted.");
        } else {
            System.out.println("Task not found.");
        }
    }

    private void listAllProjects() {
        List<Project> projects = blackboard.getProjects();
        if (projects.isEmpty()) {
            System.out.println("No projects yet.");
            return;
        }

        System.out.println("\nProjects:");
        for (Project project : projects) {
            System.out.println("  #" + project.getId() + " " + project.getName());
        }
    }

    private void listStories(Project project) {
        List<Story> stories = project.getStories();
        if (stories.isEmpty()) {
            System.out.println("No stories yet.");
            return;
        }

        System.out.println("\nStories:");
        for (Story story : stories) {
            System.out.println("  #" + story.getId() + " " + story.getTitle());
        }
    }

    private void listTasks(Story story) {
        List<Task> tasks = story.getTasks();
        if (tasks.isEmpty()) {
            System.out.println("No tasks yet.");
            return;
        }

        System.out.println("\nTasks:");
        for (Task task : tasks) {
            System.out.println("  #" + task.getId() + " " + task.getTitle());
        }
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String raw = scanner.nextLine().trim();
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException ignored) {
                System.out.println("Enter a number.");
            }
        }
    }

    private String readNonEmpty(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("Value cannot be empty.");
        }
    }
}