/**
 * Observer of {@link Blackboard} mutations. Implementations refresh UI or other derived state when
 * projects, stories, or tasks change.
 */
@FunctionalInterface
public interface BlackboardObserver {

    /**
     * Called after the blackboard or any nested project/story/task collection has changed.
     */
    void blackboardChanged();
}
