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
 * Taiga integration via Tulip ({@code Tulip-Examples-main/MainTaiga.java}).
 * After login, all Taiga projects are imported automatically.
 *
 * @author Joseph Carl Santos
 * @version 1.0
 */
public final class TaigaAppController implements AppController {

    private final TaigaClient taigaClient = new TaigaClient();
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
                        showError(parent, message(ex));
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
                    showError(parent, message(ex));
                }
            }
        }.execute();
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

    private static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Taiga connection failed", JOptionPane.ERROR_MESSAGE);
    }

    private record LoginCredentials(String username, String password) {
    }
}
