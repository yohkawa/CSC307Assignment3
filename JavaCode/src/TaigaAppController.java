import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.Window;

/**
 * Taiga and Groq controller.
 *
 * Taiga loads data into the Blackboard.
 * Groq reviews selected local Blackboard stories using TULIP Groq.
 *
 * @author Joseph Carl Santos
 * @version 1.1
 */
public final class TaigaAppController implements AppController {

    private final TaigaClient taigaClient = new TaigaClient();
    private final GroqAIClient groqAIClient = new GroqAIClient();

    private String loggedInUser;

    @Override
    public void connectToTaiga(Component parent, Blackboard blackboard) {
        if (!taigaClient.isLoggedIn()) {
            LoginCredentials credentials = promptForLogin(parent);
            if (credentials == null) {
                return;
            }

            setWaitCursor(parent, true);
            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    taigaClient.login(credentials.username(), credentials.password());
                    loggedInUser = credentials.username();
                    return importAllProjects(blackboard);
                }

                @Override
                protected void done() {
                    setWaitCursor(parent, false);
                    try {
                        showSuccess(parent, get());
                    } catch (Exception ex) {
                        showError(parent, "Taiga connection failed", message(ex));
                    }
                }
            }.execute();
            return;
        }

        setWaitCursor(parent, true);
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return importAllProjects(blackboard);
            }

            @Override
            protected void done() {
                setWaitCursor(parent, false);
                try {
                    showSuccess(parent, get());
                } catch (Exception ex) {
                    showError(parent, "Taiga connection failed", message(ex));
                }
            }
        }.execute();
    }

    @Override
    public void connectToGroq(Component parent) {
        setWaitCursor(parent, true);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                groqAIClient.connect();
                String reply = groqAIClient.testConnection();
                return "Connected to Groq using model: " + groqAIClient.getModel() + "\n\n" + reply;
            }

            @Override
            protected void done() {
                setWaitCursor(parent, false);
                try {
                    JOptionPane.showMessageDialog(
                            parent,
                            get(),
                            "Groq Connected",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                } catch (Exception ex) {
                    showError(parent, "Groq connection failed", message(ex));
                }
            }
        }.execute();
    }

    @Override
    public String reviewStoryWithAI(Story story, String userPrompt) {
        return groqAIClient.reviewStory(story, userPrompt);
    }

    private String importAllProjects(Blackboard blackboard) throws Exception {
        var projects = taigaClient.fetchAllMyProjects();
        if (projects.isEmpty()) {
            throw new RuntimeException("No Taiga projects found for this account.");
        }

        blackboard.syncAllFromTaiga(projects);

        StringBuilder summary = new StringBuilder();
        summary.append("Imported ").append(projects.size()).append(" project(s):\n");
        for (TaigaClient.TaigaProjectData project : projects) {
            summary.append("  • ")
                    .append(project.name())
                    .append(" (")
                    .append(project.stories().size())
                    .append(" stories)\n");
        }
        return summary.toString().trim();
    }

    private LoginCredentials promptForLogin(Component parent) {
        JTextField usernameField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);

        JPanel panel = new JPanel(new GridLayout(2, 2, 6, 6));
        panel.add(new JLabel("Username or email:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        int choice = JOptionPane.showConfirmDialog(
                parent,
                panel,
                "Login to Taiga",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            throw new RuntimeException("Username and password are required.");
        }

        return new LoginCredentials(username, password);
    }

    private void showSuccess(Component parent, String summary) {
        String userLine = loggedInUser == null ? "" : "Logged in as " + loggedInUser + ".\n";
        JOptionPane.showMessageDialog(
                parent,
                userLine + summary,
                "Taiga Connected",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private static String message(Exception ex) {
        Throwable cause = ex.getCause();
        if (cause != null && cause.getMessage() != null) {
            return cause.getMessage();
        }

        return ex.getMessage() == null ? ex.toString() : ex.getMessage();
    }

    private static void setWaitCursor(Component parent, boolean waiting) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        if (window != null) {
            window.setCursor(waiting
                    ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                    : Cursor.getDefaultCursor());
        }
    }

    private static void showError(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private record LoginCredentials(String username, String password) {
    }
}