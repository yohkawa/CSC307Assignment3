import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Central repository (“blackboard”) for all {@link Project}s. Assigns project IDs and notifies
 * {@link BlackboardObserver}s after any structural change so views stay synchronized.
 */
public class Blackboard {

    private final List<Project> projects = new ArrayList<>();
    private final List<BlackboardObserver> observers = new ArrayList<>();
    private int nextProjectId = 1;

    /**
     * Creates a project with a non-blank name and notifies observers.
     *
     * @param name display name (trimmed); must not be null or blank
     * @return the new project
     */
    public Project createProject(String name) {
        Project project = new Project(nextProjectId++, name, this);
        projects.add(project);
        notifyObservers();
        return project;
    }

    public List<Project> getProjects() {
        return Collections.unmodifiableList(projects);
    }

    /**
     * @param id project identifier
     * @return the project, or {@code null} if none match
     */
    public Project getProjectById(int id) {
        return projects.stream()
                .filter(project -> project.getId() == id)
                .findFirst()
                .orElse(null);
    }

    /**
     * Removes the project with the given id, if present.
     *
     * @return {@code true} if a project was removed
     */
    public boolean deleteProjectById(int id) {
        boolean removed = projects.removeIf(project -> project.getId() == id);
        if (removed) {
            notifyObservers();
        }
        return removed;
    }

    public void addObserver(BlackboardObserver observer) {
        observers.add(Objects.requireNonNull(observer, "observer"));
    }

    public void removeObserver(BlackboardObserver observer) {
        observers.remove(Objects.requireNonNull(observer, "observer"));
    }

    /**
     * Notifies all observers on the calling thread. Uses a snapshot of the listener list so
     * add/remove during notification does not break iteration.
     */
    public void notifyObservers() {
        for (BlackboardObserver observer : List.copyOf(observers)) {
            observer.blackboardChanged();
        }
    }
}
