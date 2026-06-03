import java.util.Objects;

/**
 * Leaf node in the hierarchy: a concrete action item under a {@link Story}.
 */
public final class Task {

    private final int id;
    private final String title;
    private TaskStatus status;

    public Task(int id, String title) {
        this(id, title, TaskStatus.INCOMPLETE);
    }

    public Task(int id, String title, TaskStatus status) {
        if (id < 0) {
            throw new IllegalArgumentException("id must not be negative");
        }
        this.id = id;
        this.title = validateTitle(title);
        this.status = Objects.requireNonNull(status, "status");
    }

    private static String validateTitle(String title) {
        Objects.requireNonNull(title, "title");
        String trimmed = title.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        return trimmed;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public boolean isComplete() {
        return status.isComplete();
    }

    @Override
    public String toString() {
        return "#" + id + " " + title + " [" + status + "]";
    }
}