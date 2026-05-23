import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Groups {@link Task} items under a narrative title. Notifies the shared {@link Blackboard} when
 * tasks are created or removed so observers can refresh.
 */
public class Story {

    private final int id;
    private final String title;
    private final List<Task> tasks = new ArrayList<>();
    private final Blackboard blackboard;
    private int nextTaskId = 1;

    public Story(int id, String title, Blackboard blackboard) {
        if (id < 0) {
            throw new IllegalArgumentException("id must not be negative");
        }
        this.id = id;
        this.title = validateTitle(title);
        this.blackboard = Objects.requireNonNull(blackboard, "blackboard");
    }

    private static String validateTitle(String title) {
        Objects.requireNonNull(title, "title");
        String trimmed = title.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        return trimmed;
    }

    public Task createTask(String title) {
        Task task = new Task(nextTaskId++, title);
        tasks.add(task);
        blackboard.notifyObservers();
        return task;
    }

    /** Used when importing tasks from Taiga. @author Joseph Carl Santos */
    void importTask(Task task) {
        tasks.add(task);
        nextTaskId = Math.max(nextTaskId, task.getId() + 1);
    }

    public Task getTaskById(int taskId) {
        return tasks.stream()
                .filter(task -> task.getId() == taskId)
                .findFirst()
                .orElse(null);
    }

    public boolean deleteTaskById(int taskId) {
        boolean removed = tasks.removeIf(task -> task.getId() == taskId);
        if (removed) {
            blackboard.notifyObservers();
        }
        return removed;
    }

    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return "#" + id + " " + title;
    }
}
