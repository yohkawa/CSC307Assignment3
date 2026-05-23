import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javiergs.tulip.taiga.TaigaProject;
import javiergs.tulip.taiga.TaigaUserStory;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Taiga helper using Tulip ({@code Tulip-Examples-main/MainTaiga.java}) for login,
 * projects, and user stories; task-to-story links come from the Taiga API JSON
 * ({@code user_story} field — not exposed on Tulip {@code TaigaTask}).
 *
 * @author Joseph Carl Santos
 * @version 1.0
 */
public final class TaigaClient {

    private static final String DEFAULT_HOST = "https://api.taiga.io";
    private static final String API = DEFAULT_HOST + "/api/v1";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final javiergs.tulip.taiga.TaigaClient tulip;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private boolean loggedIn;

    public TaigaClient() {
        this(DEFAULT_HOST);
    }

    public TaigaClient(String host) {
        tulip = new javiergs.tulip.taiga.TaigaClient(host);
    }

    public void login(String username, String password) throws Exception {
        tulip.login(username, password);
        loggedIn = true;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public List<TaigaProjectOption> listProjects() throws Exception {
        ensureLoggedIn();
        List<TaigaProjectOption> options = new ArrayList<>();
        for (TaigaProject project : tulip.getMyProjects()) {
            options.add(new TaigaProjectOption(project.getId(), project.getName()));
        }
        return options;
    }

    public List<TaigaProjectData> fetchAllMyProjects() throws Exception {
        List<TaigaProjectData> loaded = new ArrayList<>();
        for (TaigaProjectOption option : listProjects()) {
            loaded.add(fetchProject(option.id()));
        }
        return loaded;
    }

    /**
     * Same flow as {@code MainTaiga}: {@code getStories(projectId)} then tasks for that project,
     * with each task placed under its user story.
     */
    public TaigaProjectData fetchProject(long projectId) throws Exception {
        ensureLoggedIn();
        TaigaProject project = findProjectById(projectId);

        Map<Long, TaigaStoryData> storiesById = new LinkedHashMap<>();
        for (TaigaUserStory userStory : tulip.getStories(projectId)) {
            long storyId = userStory.getId();
            storiesById.put(storyId, new TaigaStoryData(storyId, formatUserStory(userStory)));
        }

        attachTasksToStories(projectId, storiesById);

        return new TaigaProjectData(
                projectId,
                project.getName(),
                new ArrayList<>(storiesById.values())
        );
    }

    private void attachTasksToStories(long projectId, Map<Long, TaigaStoryData> storiesById) throws Exception {
        String tasksJson = get(
                "/tasks?project=" + projectId,
                readTulipAuthToken()
        );

        JsonNode root = JSON.readTree(tasksJson);
        if (!root.isArray()) {
            return;
        }

        for (JsonNode row : root) {
            if (!row.has("user_story") || row.get("user_story").isNull()) {
                continue;
            }

            long storyId = row.get("user_story").asLong();
            TaigaStoryData story = storiesById.get(storyId);
            if (story == null) {
                continue;
            }

            long taskId = row.get("id").asLong();
            String subject = row.has("subject") ? row.get("subject").asText("").trim() : "";
            if (subject.isEmpty()) {
                subject = "Task";
            }
            int ref = row.has("ref") && !row.get("ref").isNull() ? row.get("ref").asInt() : 0;
            String title = ref > 0 ? "#" + ref + " " + subject : subject;

            story.tasks().add(new TaigaTaskData(taskId, title));
        }
    }

    private String readTulipAuthToken() throws Exception {
        Field authField = tulip.getClass().getDeclaredField("authToken");
        authField.setAccessible(true);
        Object token = authField.get(tulip);
        if (token == null || String.valueOf(token).isBlank()) {
            throw new IllegalStateException("Not logged in to Taiga.");
        }
        return String.valueOf(token);
    }

    private String get(String pathWithQuery, String authToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API + pathWithQuery))
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + authToken)
                .header("Accept", "application/json")
                .header("x-disable-pagination", "True")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("Taiga API error (" + response.statusCode() + "): " + response.body());
        }
        return response.body();
    }

    private TaigaProject findProjectById(long projectId) throws Exception {
        for (TaigaProject project : tulip.getMyProjects()) {
            if (project.getId() == projectId) {
                return project;
            }
        }
        throw new RuntimeException("Project not found: " + projectId);
    }

    private static String formatUserStory(TaigaUserStory userStory) {
        String subject = userStory.getSubject();
        if (subject == null || subject.isBlank()) {
            subject = "Story";
        }
        return "#" + userStory.getRef() + " " + subject.trim();
    }

    private void ensureLoggedIn() {
        if (!loggedIn) {
            throw new IllegalStateException("Not logged in to Taiga.");
        }
    }

    public record TaigaProjectOption(long id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }

    public record TaigaProjectData(long id, String name, List<TaigaStoryData> stories) {
    }

    public record TaigaStoryData(long id, String title, List<TaigaTaskData> tasks) {
        TaigaStoryData(long id, String title) {
            this(id, title, new ArrayList<>());
        }
    }

    public record TaigaTaskData(long id, String title) {
    }
}
