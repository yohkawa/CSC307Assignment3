import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * GUI calls this interface when the user clicks the Taiga or AI buttons.
 *
 * @author Eman Castilo Hernandez
 * @version 1.0
 */

public interface AppController {

    /**
     * Taiga API connection and sync. Implemented by {@link TaigaAppController}.
     *
     * @author Joseph Carl Santos
     */
    default void connectToTaiga(Component parent, Blackboard blackboard) {
        JOptionPane.showMessageDialog(
                parent,
                "Taiga connection hook is ready. Another teammate can connect the real Taiga API here.",
                "Taiga Integration",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    default void connectToGroq(Component parent) {
        JOptionPane.showMessageDialog(
                parent,
                "Groq connection hook is ready. Another teammate can connect the real Groq API here.",
                "Groq Integration",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    default String reviewStoryWithAI(Story story, String userPrompt) {
        if (story == null) {
            return "Select a story before asking the AI assistant for story feedback.";
        }

        return "Groq AI hook is ready for story #" + story.getId()
                + " (" + story.getTitle() + ").\n"
                + "Prompt:\n" + userPrompt + "\n\n"
                + "Another teammate can connect this method to the real Groq API.";
    }
}
