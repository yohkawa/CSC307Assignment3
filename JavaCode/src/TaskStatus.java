/**
 * Available workflow states for a task in the task board.
 */
public enum TaskStatus {
    INCOMPLETE("Incomplete"),
    IN_PROGRESS("In Progress"),
    COMPLETE("Complete"),
    BLOCKED("Blocked");

    private final String displayName;

    TaskStatus(String displayName) {
        this.displayName = displayName;
    }

    public boolean isComplete() {
        return this == COMPLETE;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Maps free-form local or Taiga status text into the closest local status.
     */
    public static TaskStatus fromText(String statusText) {
        if (statusText == null || statusText.isBlank()) {
            return INCOMPLETE;
        }

        String normalized = statusText.trim().toLowerCase();
        if (normalized.contains("incomplete") || normalized.contains("open") || normalized.contains("new")) {
            return INCOMPLETE;
        }
        if (normalized.contains("progress") || normalized.contains("doing") || normalized.contains("started")) {
            return IN_PROGRESS;
        }
        if (normalized.contains("block") || normalized.contains("imped")) {
            return BLOCKED;
        }
        if (normalized.contains("complete") || normalized.contains("done") || normalized.contains("closed")) {
            return COMPLETE;
        }
        return INCOMPLETE;
    }
}