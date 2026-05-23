import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Taiga REST client. Uses JDK {@link HttpClient} and Gson.
 *
 * @author Joseph Carl Santos
 * @version 1.0
 */
public final class TaigaClient {

    private static final String API = "https://api.taiga.io/api/v1";
    private static final Gson GSON = new Gson();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private String authToken;

    public void login(String username, String password) {
        JsonObject body = new JsonObject();
        body.addProperty("type", "normal");
        body.addProperty("username", username);
        body.addProperty("password", password);

        JsonObject response = JsonParser.parseString(post("/auth", body, null)).getAsJsonObject();
        authToken = textOrNull(response, "auth_token");
        if (authToken == null || authToken.isBlank()) {
            throw new RuntimeException("Taiga login failed. Check username and password.");
        }
    }

    public boolean isLoggedIn() {
        return authToken != null && !authToken.isBlank();
    }

    public TaigaProjectData fetchProjectBySlug(String slug) {
        ensureLoggedIn();

        JsonObject project = JsonParser.parseString(
                get("/projects/by_slug", Map.of("slug", slug), FetchMode.PLAIN)
        ).getAsJsonObject();

        long projectId = project.get("id").getAsLong();
        String name = firstNonBlank(textOrNull(project, "name"), slug);
        String projectSlug = firstNonBlank(textOrNull(project, "slug"), slug);

        Map<Long, TaigaStoryData> storiesById = new LinkedHashMap<>();
        for (JsonElement element : parseArray(get("/userstories", Map.of("project", String.valueOf(projectId)), FetchMode.ALL_AT_ONCE))) {
            JsonObject row = element.getAsJsonObject();
            long storyId = row.get("id").getAsLong();
            String title = formatItem(row, "subject", "Story " + refOrId(row, storyId));
            storiesById.put(storyId, new TaigaStoryData(storyId, title));
        }

        for (JsonElement element : parseArray(get("/tasks", Map.of("project", String.valueOf(projectId)), FetchMode.ALL_AT_ONCE))) {
            JsonObject row = element.getAsJsonObject();
            if (!row.has("user_story") || row.get("user_story").isJsonNull()) {
                continue;
            }

            long storyId = row.get("user_story").getAsLong();
            TaigaStoryData story = storiesById.get(storyId);
            if (story == null) {
                continue;
            }

            long taskId = row.get("id").getAsLong();
            String title = formatItem(row, "subject", "Task " + refOrId(row, taskId));
            story.tasks().add(new TaigaTaskData(taskId, title));
        }

        return new TaigaProjectData(projectId, name, projectSlug, new ArrayList<>(storiesById.values()));
    }

    private void ensureLoggedIn() {
        if (authToken == null || authToken.isBlank()) {
            throw new IllegalStateException("Not logged in to Taiga.");
        }
    }

    private enum FetchMode {
        PLAIN,
        ALL_AT_ONCE
    }

    private String get(String path, Map<String, String> query, FetchMode mode) {
        return request("GET", path, query, null, authToken, mode).body();
    }

    private String post(String path, JsonObject body, String token) {
        return request("POST", path, Map.of(), GSON.toJson(body), token, FetchMode.PLAIN).body();
    }

    private JsonArray parseArray(String json) {
        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonArray()) {
            return new JsonArray();
        }
        return parsed.getAsJsonArray();
    }

    private HttpResult request(String method, String path, Map<String, String> query, String jsonBody, String token,
                               FetchMode mode) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(buildUrl(path, query)))
                    .timeout(Duration.ofSeconds(mode == FetchMode.ALL_AT_ONCE ? 120 : 30))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");

            if (token != null && !token.isBlank()) {
                builder.header("Authorization", "Bearer " + token);
            }

            if (!"GET".equals(method)) {
                builder.POST(HttpRequest.BodyPublishers.ofString(Objects.requireNonNull(jsonBody)));
            } else {
                builder.GET();
                if (mode == FetchMode.ALL_AT_ONCE) {
                    builder.header("x-disable-pagination", "True");
                }
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("Taiga API error (" + response.statusCode() + "): " + shorten(response.body()));
            }

            return new HttpResult(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Taiga request interrupted.", ex);
        } catch (IOException ex) {
            throw new RuntimeException("Could not reach Taiga: " + ex.getMessage(), ex);
        }
    }

    private record HttpResult(String body) {
    }

    private static String buildUrl(String path, Map<String, String> query) {
        String base = API + path;
        if (query == null || query.isEmpty()) {
            return base;
        }

        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> entry : query.entrySet()) {
            joiner.add(encode(entry.getKey()) + "=" + encode(entry.getValue()));
        }
        return base + "?" + joiner;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String formatItem(JsonObject row, String subjectField, String fallback) {
        String subject = firstNonBlank(textOrNull(row, subjectField), fallback);
        if (row.has("ref") && !row.get("ref").isJsonNull()) {
            return "#" + row.get("ref").getAsInt() + " " + subject;
        }
        return subject;
    }

    private static int refOrId(JsonObject row, long id) {
        if (row.has("ref") && !row.get("ref").isJsonNull()) {
            return row.get("ref").getAsInt();
        }
        return (int) Math.min(id, Integer.MAX_VALUE);
    }

    private static String textOrNull(JsonObject object, String field) {
        if (!object.has(field) || object.get(field).isJsonNull()) {
            return null;
        }
        return object.get(field).getAsString();
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        return fallback;
    }

    private static String shorten(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() <= 180 ? trimmed : trimmed.substring(0, 177) + "...";
    }

    public record TaigaProjectData(long id, String name, String slug, List<TaigaStoryData> stories) {
    }

    public record TaigaStoryData(long id, String title, List<TaigaTaskData> tasks) {
        TaigaStoryData(long id, String title) {
            this(id, title, new ArrayList<>());
        }
    }

    public record TaigaTaskData(long id, String title) {
    }
}
