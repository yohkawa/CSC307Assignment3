import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Groq AI assistant panel for reviewing selected Blackboard stories.
 *
 * It sends AI requests to {@link AppController}; it does not talk directly to
 * another GUI component or directly instantiate API clients.
 *
 * @author Eman Castilo Hernandez
 * @version 1.1
 */
public final class AIPanel extends JPanel {

    private final Supplier<AppController> controllerSupplier;
    private final Supplier<Story> selectedStorySupplier;
    private final Consumer<String> statusUpdater;

    private final JTextArea promptArea = new JTextArea();
    private final JTextArea resultArea = new JTextArea();

    private final JButton reviewButton = new JButton("Review Selected Story with Groq");

    private boolean busy;

    public AIPanel(
            Supplier<AppController> controllerSupplier,
            Supplier<Story> selectedStorySupplier,
            Consumer<String> statusUpdater
    ) {
        super(new BorderLayout(4, 4));

        this.controllerSupplier = Objects.requireNonNull(controllerSupplier, "controllerSupplier");
        this.selectedStorySupplier = Objects.requireNonNull(selectedStorySupplier, "selectedStorySupplier");
        this.statusUpdater = Objects.requireNonNull(statusUpdater, "statusUpdater");

        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        configureTextAreas();

        reviewButton.addActionListener(e -> runReview());

        add(createPromptSection(), BorderLayout.NORTH);
        add(new JScrollPane(resultArea), BorderLayout.CENTER);

        updateSelection(null);
    }

    public void updateSelection(Story selectedStory) {
        reviewButton.setEnabled(!busy && selectedStory != null);
    }

    private void configureTextAreas() {
        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);
        promptArea.setRows(5);
        promptArea.setText("Review this story for INVEST quality. Suggest clearer acceptance criteria and development tasks.");

        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
    }

    private JPanel createPromptSection() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));

        panel.add(new JLabel("Prompt for selected story:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(promptArea), BorderLayout.CENTER);
        panel.add(createButtonRow(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createButtonRow() {
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonRow.add(reviewButton);
        return buttonRow;
    }

    private void runReview() {
        Story selectedStory = selectedStorySupplier.get();
        if (selectedStory == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Select a story before asking Groq for feedback.",
                    "No story selected",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        String prompt = promptArea.getText().trim();
        setBusy(true);
        resultArea.setText("Asking Groq to review story #" + selectedStory.getId() + "...\n");
        statusUpdater.accept("Groq review running...");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return controllerSupplier.get().reviewStoryWithAI(selectedStory, prompt);
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    resultArea.setText(result);
                    resultArea.setCaretPosition(0);
                    statusUpdater.accept("Groq AI review completed.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showFailure(ex);
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    showFailure(cause);
                } finally {
                    setBusy(false);
                    updateSelection(selectedStorySupplier.get());
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        reviewButton.setEnabled(!busy && selectedStorySupplier.get() != null);
        setCursor(busy
                ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                : Cursor.getDefaultCursor());
    }

    private void showFailure(Throwable ex) {
        String message = ex.getMessage() == null ? ex.toString() : ex.getMessage();
        resultArea.setText("Groq review failed.\n\n" + message);
        JOptionPane.showMessageDialog(
                this,
                message,
                "AI review failed",
                JOptionPane.ERROR_MESSAGE
        );
        statusUpdater.accept("Groq AI review failed.");
    }
}