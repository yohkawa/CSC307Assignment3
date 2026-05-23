import javiergs.tulip.groq.GroqClient;
import javiergs.tulip.groq.GroqConfig;
import javiergs.tulip.groq.GroqMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Small application-facing adapter around the TULIP Groq classes.
 *
 * This keeps Groq-specific API code out of the Swing panels so the UI still
 * talks through the controller layer instead of directly calling an API client.
 *
 * @author Yuto Ohkawa
 * @version 1.0
 */
public final class GroqAIClient {

    private static final String DEFAULT_BASE_URL = "https://api.groq.com/openai/v1";
    private static final String DEFAULT_MODEL = "llama-3.3-70b-versatile";

    private GroqClient client;
    private String model = DEFAULT_MODEL;

    public void connect(String apiKey, String modelName) {
        String cleanedKey = validateApiKey(apiKey);
        String cleanedModel = isBlank(modelName) ? DEFAULT_MODEL : modelName.trim();

        GroqConfig config = new GroqConfig(cleanedKey, DEFAULT_BASE_URL, cleanedModel);
        this.client = new GroqClient(config);
        this.model = cleanedModel;
    }

    public boolean isConnected() {
        return client != null;
    }

    public String getModel() {
        return model;
    }

    /**
     * Sends a tiny prompt so the UI can confirm that the key/model work now,
     * not only when the user later reviews a story.
     */
    public String testConnection() {
        ensureConnected();

        List<GroqMessage> messages = List.of(
                GroqMessage.system("You are a concise connection test assistant."),
                GroqMessage.user("Reply with one short sentence confirming that Groq is connected.")
        );

        return client.chat(messages, 0.2, 80);
    }

    /**
     * Reviews one local user story and its current local tasks.
     */
    public String reviewStory(Story story, String userPrompt) {
        ensureConnected();
        Objects.requireNonNull(story, "story");

        List<GroqMessage> messages = new ArrayList<>();
        messages.add(GroqMessage.system(systemPrompt()));
        messages.add(GroqMessage.user(buildStoryReviewPrompt(story, userPrompt)));

        return client.chat(messages, 0.2, 1400);
    }

    private String systemPrompt() {
        return "You are an AI assistant inside a Java desktop software engineering planner. "
                + "Help improve agile artifacts. Be specific, practical, and concise. "
                + "When reviewing a user story, use INVEST, acceptance criteria quality, and development task clarity. "
                + "Do not claim that remote Taiga data was changed; this application is read/local-edit oriented.";
    }

    private String buildStoryReviewPrompt(Story story, String userPrompt) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("User request:\n");
        prompt.append(isBlank(userPrompt)
                ? "Review this story for INVEST quality and suggest better acceptance criteria."
                : userPrompt.trim());
        prompt.append("\n\nLocal story data from the Blackboard:\n");
        prompt.append("Story ID: ").append(story.getId()).append("\n");
        prompt.append("Story title: ").append(story.getTitle()).append("\n");
        prompt.append("Current tasks:\n");

        if (story.getTasks().isEmpty()) {
            prompt.append("- No tasks currently listed.\n");
        } else {
            for (Task task : story.getTasks()) {
                prompt.append("- #")
                        .append(task.getId())
                        .append(" ")
                        .append(task.getTitle())
                        .append("\n");
            }
        }

        prompt.append("\nReturn your answer using these headings:\n");
        prompt.append("1. INVEST Review\n");
        prompt.append("2. Acceptance Criteria Suggestions\n");
        prompt.append("3. Suggested Development Tasks\n");
        prompt.append("4. Main Risks or Missing Details\n");
        return prompt.toString();
    }

    private void ensureConnected() {
        if (!isConnected()) {
            throw new IllegalStateException("Connect to Groq before using the AI assistant.");
        }
    }

    private static String validateApiKey(String apiKey) {
        if (isBlank(apiKey)) {
            throw new IllegalArgumentException("Groq API key is required.");
        }
        return apiKey.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}