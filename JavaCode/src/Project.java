import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A planning container holding {@link Story} items. Projects live on the {@link Blackboard} and
 * propagate change notifications upward for observer updates.
 */
public class Project {

    private final int id;
    private final String name;
    private final List<Story> stories = new ArrayList<>();
    private final Blackboard blackboard;
    private int nextStoryId = 1;

    public Project(int id, String name, Blackboard blackboard) {
        if (id < 0) {
            throw new IllegalArgumentException("id must not be negative");
        }
        this.id = id;
        this.name = validateName(name);
        this.blackboard = Objects.requireNonNull(blackboard, "blackboard");
    }

    private static String validateName(String name) {
        Objects.requireNonNull(name, "name");
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return trimmed;
    }

    public Story createStory(String title) {
        Story story = new Story(nextStoryId++, title, blackboard);
        stories.add(story);
        blackboard.notifyObservers();
        return story;
    }

    public Story getStoryById(int storyId) {
        return stories.stream()
                .filter(story -> story.getId() == storyId)
                .findFirst()
                .orElse(null);
    }

    public boolean deleteStoryById(int storyId) {
        boolean removed = stories.removeIf(story -> story.getId() == storyId);
        if (removed) {
            blackboard.notifyObservers();
        }
        return removed;
    }

    public List<Story> getStories() {
        return Collections.unmodifiableList(stories);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "#" + id + " " + name;
    }
}
