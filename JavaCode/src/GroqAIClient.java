import javiergs.tulip.groq.GroqClient;
import javiergs.tulip.groq.GroqConfig;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Groq AI client using the TULIP Groq API.
 */
public final class GroqAIClient {

    private static final String CONFIG_FILE = "config.properties";

    private GroqClient groqClient;
    private String model;

    public void connect() {
        try {
            Properties properties = loadProperties();

            String apiKey = require(properties, "GROQ_API_KEY");
            String baseUrl = readBaseUrl(properties);
            model = require(properties, "GROQ_MODEL");

            GroqConfig config = new GroqConfig(apiKey, baseUrl, model);
            groqClient = new GroqClient(config);
        } catch (Exception ex) {
            throw new RuntimeException("Could not connect to Groq: " + ex.getMessage(), ex);
        }
    }

    public boolean isConnected() {
        return groqClient != null;
    }

    public String getModel() {
        return model;
    }

    public String testConnection() {
        ensureConnected();
        return groqClient.chat("Reply with one short sentence confirming that Groq is connected.");
    }

    public String reviewStory(Story story, String userPrompt) {
        ensureConnected();

        if (story == null) {
            throw new RuntimeException("Select a story before asking Groq for feedback.");
        }

        String prompt = buildPrompt(story, userPrompt);
        return groqClient.chat(prompt);
    }

    private String buildPrompt(Story story, String userPrompt) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a software engineering assistant inside a Java desktop planner.\n");
        prompt.append("Review the selected user story and help improve the software engineering artifact.\n\n");

        prompt.append("User request:\n");
        if (userPrompt == null || userPrompt.isBlank()) {
            prompt.append("Review this story for INVEST quality, acceptance criteria, and useful development tasks.\n");
        } else {
            prompt.append(userPrompt.trim()).append("\n");
        }

        prompt.append("\nStory from local Blackboard:\n");
        prompt.append("Story ID: ").append(story.getId()).append("\n");
        prompt.append("Story title: ").append(story.getTitle()).append("\n");

        prompt.append("\nCurrent tasks:\n");
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

        prompt.append("\nReturn your answer with these sections:\n");
        prompt.append("1. INVEST Review\n");
        prompt.append("2. Acceptance Criteria Suggestions\n");
        prompt.append("3. Suggested Development Tasks\n");
        prompt.append("4. Missing Details or Risks\n");

        return prompt.toString();
    }

    private void ensureConnected() {
        if (!isConnected()) {
            throw new RuntimeException("Connect to Groq before using the AI assistant.");
        }
    }

    private Properties loadProperties() throws Exception {
        Properties properties = new Properties();

        InputStream inputStream = GroqAIClient.class
                .getClassLoader()
                .getResourceAsStream(CONFIG_FILE);

        if (inputStream == null) {
            String[] possiblePaths = {
                    CONFIG_FILE,
                    "src/" + CONFIG_FILE,
                    "src/main/resources/" + CONFIG_FILE,
                    "JavaCode/src/" + CONFIG_FILE,
                    "JavaCode/src/main/resources/" + CONFIG_FILE
            };

            for (String possiblePath : possiblePaths) {
                Path path = Path.of(possiblePath);
                if (Files.exists(path)) {
                    inputStream = Files.newInputStream(path);
                    break;
                }
            }
        }

        if (inputStream == null) {
            throw new FileNotFoundException(
                    "Could not find config.properties. Put it in JavaCode/src/config.properties " +
                            "or JavaCode/src/main/resources/config.properties."
            );
        }

        try (InputStream in = inputStream) {
            properties.load(in);
        }

        return properties;
    }

    private String readBaseUrl(Properties properties) {
        String baseUrl = properties.getProperty("GROQ_BASE_URL");

        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = properties.getProperty("GROQ_SERVER");
        }

        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("Missing property: GROQ_BASE_URL");
        }

        return baseUrl.trim();
    }

    private String require(Properties properties, String key) {
        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing property: " + key);
        }

        return value.trim();
    }
}