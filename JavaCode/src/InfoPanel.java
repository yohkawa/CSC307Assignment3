import javax.swing.*;
import java.awt.*;

/**
 * Shows a summary of the currently selected local data.
 *
 * @author Eman Castilo Hernandez
 * @version 1.0
 */
public final class InfoPanel extends JPanel {

    private final JTextArea textArea = new JTextArea();

    public InfoPanel() {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        add(new JScrollPane(textArea), BorderLayout.CENTER);
    }

    public void display(Blackboard blackboard, Project selectedProject, Story selectedStory, Task selectedTask) {
        StringBuilder text = new StringBuilder();
        text.append("Blackboard Summary\n");
        text.append("Projects loaded: ").append(blackboard.getProjects().size()).append("\n\n");

        if (selectedProject != null) {
            text.append("Selected Project\n");
            text.append("ID: ").append(selectedProject.getId()).append("\n");
            text.append("Name: ").append(selectedProject.getName()).append("\n");
            text.append("Stories: ").append(selectedProject.getStories().size()).append("\n\n");
        }

        if (selectedStory != null) {
            text.append("Selected Story\n");
            text.append("ID: ").append(selectedStory.getId()).append("\n");
            text.append("Title: ").append(selectedStory.getTitle()).append("\n");
            text.append("Tasks: ").append(selectedStory.getTasks().size()).append("\n\n");
        }

        if (selectedTask != null) {
            text.append("Selected Task\n");
            text.append("ID: ").append(selectedTask.getId()).append("\n");
            text.append("Title: ").append(selectedTask.getTitle()).append("\n\n");
        }

        textArea.setText(text.toString());
        textArea.setCaretPosition(0);
    }
}
