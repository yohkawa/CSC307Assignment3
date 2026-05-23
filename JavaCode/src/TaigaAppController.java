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
import java.util.ArrayList;
import java.util.List;

/**
 * Login to Taiga, import by project slug. Optional second slug adds another project.
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
            taigaClient.login(credentials.username(), credentials.password());
            loggedInUser = credentials.username();
        }

        SlugInput slugs = promptForSlugs(parent);
        if (slugs == null) {
            return;
        }

        boolean replaceExisting = blackboard.getProjects().isEmpty();
        setWaitCursor(parent, true);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                List<TaigaClient.TaigaProjectData> imported = new ArrayList<>();
                imported.add(taigaClient.fetchProjectBySlug(slugs.primary()));

                if (slugs.additional() != null) {
                    imported.add(taigaClient.fetchProjectBySlug(slugs.additional()));
                }

                if (replaceExisting) {
                    blackboard.syncFromTaiga(imported.get(0));
                    for (int i = 1; i < imported.size(); i++) {
                        blackboard.importFromTaiga(imported.get(i));
                    }
                } else {
                    for (TaigaClient.TaigaProjectData project : imported) {
                        blackboard.importFromTaiga(project);
                    }
                }

                StringBuilder summary = new StringBuilder();
                for (TaigaClient.TaigaProjectData project : imported) {
                    if (summary.length() > 0) {
                        summary.append("\n");
                    }
                    summary.append(project.name())
                            .append(" (")
                            .append(project.stories().size())
                            .append(" stories)");
                }
                return summary.toString();
            }

            @Override
            protected void done() {
                setWaitCursor(parent, false);
                try {
                    String summary = get();
                    JOptionPane.showMessageDialog(
                            parent,
                            "Taiga import done.\n" + summary,
                            "Taiga Connected",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                } catch (Exception ex) {
                    showError(parent, ex.getMessage() == null ? ex.toString() : ex.getMessage());
                }
            }
        }.execute();
    }

    private SlugInput promptForSlugs(Component parent) {
        JTextField primarySlug = new JTextField(24);
        JTextField additionalSlug = new JTextField(24);

        JPanel panel = new JPanel(new GridLayout(4, 1, 6, 6));
        panel.add(new JLabel("Taiga project slug (from URL …/project/<slug>):"));
        panel.add(primarySlug);
        panel.add(new JLabel("Import another slug too (optional):"));
        panel.add(additionalSlug);

        int choice = JOptionPane.showConfirmDialog(
                parent,
                panel,
                taigaClient.isLoggedIn() ? "Import Taiga Project" : "Select Taiga Project",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }

        String primary = primarySlug.getText().trim();
        if (primary.isEmpty()) {
            throw new RuntimeException("Project slug is required.");
        }

        String additional = additionalSlug.getText().trim();
        if (additional.isEmpty()) {
            additional = null;
        } else if (additional.equals(primary)) {
            throw new RuntimeException("Additional slug must be different from the first.");
        }

        return new SlugInput(primary, additional);
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

    private record SlugInput(String primary, String additional) {
    }

    private record LoginCredentials(String username, String password) {
    }
}
