package me.nexuswarp.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.nexuswarp.config.LanguageManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class UpdateChecker {

    public static final String GITHUB_RELEASES_URL = "https://github.com/NothingFlare/NexusWarp/releases";
    private static final String GITHUB_API = "https://api.github.com/repos/NothingFlare/NexusWarp/releases/latest";
    private static final String GITHUB_TAGS_API = "https://api.github.com/repos/NothingFlare/NexusWarp/tags";

    private final String currentVersion;
    private final Logger logger;
    private final LanguageManager languageManager;
    private final HttpClient httpClient;

    public UpdateChecker(String currentVersion, Logger logger) {
        this(currentVersion, logger, null);
    }

    public UpdateChecker(String currentVersion, Logger logger, LanguageManager languageManager) {
        this.currentVersion = currentVersion;
        this.logger = logger;
        this.languageManager = languageManager;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public CompletableFuture<Void> checkAsync() {
        return CompletableFuture.runAsync(this::checkAndLog);
    }

    public void checkAndLog() {
        try {
            String checkingMsg = languageManager != null && !languageManager.getMessageRaw("console_updates_checking").isEmpty()
                    ? languageManager.getMessage("console_updates_checking")
                    : "[NexusWarp] Checking for updates...";
            logger.info(checkingMsg);

            String remoteVersion = fetchLatestVersion();
            if (remoteVersion == null || remoteVersion.trim().isEmpty()) {
                String failMsg = languageManager != null && !languageManager.getMessageRaw("console_updates_fail").isEmpty()
                        ? languageManager.getMessage("console_updates_fail")
                        : "[NexusWarp] Could not retrieve the latest version from GitHub API.";
                logger.warning(failMsg);
                return;
            }

            int comparison = compareVersions(remoteVersion, currentVersion);
            if (comparison > 0) {
                logger.warning("****************************************************************");
                String availMsg = languageManager != null && !languageManager.getMessageRaw("console_updates_available").isEmpty()
                        ? languageManager.getMessage("console_updates_available", "remote", remoteVersion, "current", currentVersion)
                        : "[NexusWarp] A new version is available: v" + remoteVersion + " (Current: v" + currentVersion + ")";
                logger.warning(availMsg);
                String dlMsg = languageManager != null && !languageManager.getMessageRaw("console_updates_download").isEmpty()
                        ? languageManager.getMessage("console_updates_download")
                        : "[NexusWarp] Download the update at:";
                logger.warning(dlMsg);
                logger.warning("[NexusWarp]   GitHub: " + GITHUB_RELEASES_URL);
                logger.warning("****************************************************************");
            } else {
                String latestMsg = languageManager != null && !languageManager.getMessageRaw("console_updates_latest").isEmpty()
                        ? languageManager.getMessage("console_updates_latest", "version", currentVersion)
                        : "[NexusWarp] You are running on the latest version (v" + currentVersion + ")";
                logger.info(latestMsg);
            }
        } catch (Exception e) {
            String errMsg = languageManager != null && !languageManager.getMessageRaw("console_updates_error").isEmpty()
                    ? languageManager.getMessage("console_updates_error", "error", e.getMessage())
                    : "[NexusWarp] Could not check for updates: " + e.getMessage();
            logger.warning(errMsg);
        }
    }

    public String fetchLatestVersion() {
        // Try releases/latest first
        String fromReleases = fetchFromReleases();
        if (fromReleases != null && !fromReleases.trim().isEmpty()) {
            return fromReleases;
        }
        // Fallback to tags API if no formal release was drafted yet
        return fetchFromTags();
    }

    private String fetchFromReleases() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GITHUB_API))
                    .header("User-Agent", "NothingFlare/NexusWarp/" + currentVersion)
                    .header("Accept", "application/vnd.github.v3+json")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                if (obj.has("tag_name")) {
                    return obj.get("tag_name").getAsString().trim();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String fetchFromTags() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GITHUB_TAGS_API))
                    .header("User-Agent", "NothingFlare/NexusWarp/" + currentVersion)
                    .header("Accept", "application/vnd.github.v3+json")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonElement root = JsonParser.parseString(response.body());
                if (root.isJsonArray()) {
                    JsonArray array = root.getAsJsonArray();
                    if (array.size() > 0) {
                        JsonObject latest = array.get(0).getAsJsonObject();
                        if (latest.has("name")) {
                            return latest.get("name").getAsString().trim();
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static int compareVersions(String v1, String v2) {
        if (v1 == null) v1 = "0";
        if (v2 == null) v2 = "0";

        String clean1 = v1.trim().replaceAll("^[vV]", "");
        String clean2 = v2.trim().replaceAll("^[vV]", "");

        String[] parts1 = clean1.split("[.\\-_]");
        String[] parts2 = clean2.split("[.\\-_]");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int num1 = 0;
            int num2 = 0;

            if (i < parts1.length) {
                String p1 = parts1[i].replaceAll("\\D+", "");
                if (!p1.isEmpty()) {
                    try {
                        num1 = Integer.parseInt(p1);
                    } catch (NumberFormatException ignored) {}
                }
            }

            if (i < parts2.length) {
                String p2 = parts2[i].replaceAll("\\D+", "");
                if (!p2.isEmpty()) {
                    try {
                        num2 = Integer.parseInt(p2);
                    } catch (NumberFormatException ignored) {}
                }
            }

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }

        return 0;
    }
}
