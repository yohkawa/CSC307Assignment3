import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Shows a summary of the currently selected local data.
 *
 * @author Eman Castilo Hernandez
 * @version 1.0
 */
public final class InfoPanel extends JPanel {

    private final Blackboard blackboard;
    private final JTextArea infoArea = new JTextArea();

    public InfoPanel(Blackboard blackboard) {
        super(new BorderLayout(4, 4));

        this.blackboard = Objects.requireNonNull(blackboard, "blackboard");

        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);

        add(new JScrollPane(infoArea), BorderLayout.CENTER);
    }

    public void updateSelection(Project selectedProject, Story selectedStory, Task selectedTask) {
        StringBuilder info = new StringBuilder();

        info.append("Blackboard Summary\n");
        info.append("Projects loaded: ").append(blackboard.getProjects().size()).append("\n\n");

        if (selectedProject != null) {
            info.append("Selected Project\n");
            info.append("ID: ").append(selectedProject.getId()).append("\n");
            info.append("Name: ").append(selectedProject.getName()).append("\n");
            info.append("Stories: ").append(selectedProject.getStories().size()).append("\n\n");
        }

        if (selectedStory != null) {
            info.append("Selected Story\n");
            info.append("ID: ").append(selectedStory.getId()).append("\n");
            info.append("Title: ").append(selectedStory.getTitle()).append("\n");
            info.append("Tasks: ").append(selectedStory.getTasks().size()).append("\n\n");
        }

        if (selectedTask != null) {
            info.append("Selected Task\n");
            info.append("ID: ").append(selectedTask.getId()).append("\n");
            info.append("Title: ").append(selectedTask.getTitle()).append("\n\n");
        }

        infoArea.setText(info.toString());
        infoArea.setCaretPosition(0);
    }
}