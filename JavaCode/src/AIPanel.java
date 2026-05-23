import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * UI area reserved for Groq story suggestions.
 *
 * It sends Groq connection and AI review requests to {@link AppController}.
 *
 *  @author Eman Castilo Hernandez
 *  @version 1.0
 */
public final class AIPanel extends JPanel {

    private final Supplier<AppController> controllerSupplier;
    private final Supplier<Story> selectedStorySupplier;
    private final Consumer<String> statusUpdater;

    private final JTextArea promptArea = new JTextArea();
    private final JTextArea resultArea = new JTextArea();

    private final JButton reviewButton = new JButton("Review Selected Story");

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
        reviewButton.setEnabled(selectedStory != null);
    }

    private void configureTextAreas() {
        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);
        promptArea.setRows(5);
        promptArea.setText("Review this story for quality and suggest clearer acceptance criteria.");

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
        try {
            Story selectedStory = selectedStorySupplier.get();
            String prompt = promptArea.getText().trim();

            String result = controllerSupplier.get().reviewStoryWithAI(selectedStory, prompt);

            resultArea.setText(result);
            resultArea.setCaretPosition(0);
            statusUpdater.accept("AI review completed.");
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "AI review failed",
                    JOptionPane.ERROR_MESSAGE
            );

            statusUpdater.accept("AI review failed.");
        }
    }
}