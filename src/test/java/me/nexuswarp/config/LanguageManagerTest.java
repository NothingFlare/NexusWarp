package me.nexuswarp.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class LanguageManagerTest {

    private File tempFile;

    @BeforeEach
    public void setUp() throws IOException {
        tempFile = File.createTempFile("messages_test", ".yml");
    }

    @AfterEach
    public void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    public void testSyncLanguageFileWhenMatchingDoesNothing() throws IOException {
        String content = "prefix: \"&8[&6Nexus&8] &r\"\n" +
                "usage_playtime: \"&cUsage: &e/playtime\"\n" +
                "playtime_self: \"&aTime: &e{time}\"\n";
        Files.writeString(tempFile.toPath(), content, StandardCharsets.UTF_8);

        long lastModified = tempFile.lastModified();

        String refYaml = "prefix: \"\"\n" +
                "usage_playtime: \"&cUsage: &e/playtime\"\n" +
                "playtime_self: \"&aTime: &e{time}\"\n";
        InputStream refStream = new ByteArrayInputStream(refYaml.getBytes(StandardCharsets.UTF_8));

        int added = LanguageManager.syncLanguageFile(tempFile, refStream, null);

        assertEquals(0, added, "When config matches reference, sync should return 0 (do nothing)");
        String afterContent = Files.readString(tempFile.toPath(), StandardCharsets.UTF_8);
        assertEquals(content, afterContent, "File content should remain completely untouched when matching");
    }

    @Test
    public void testSyncLanguageFileWhenOutdatedUpdatesFile() throws IOException {
        // Outdated file with only 2 keys and a custom prefix
        String initialContent = "# Custom user comment\n" +
                "prefix: \"&c[Custom] &r\"\n" +
                "cannot_tp_self: \"&cCannot tp to self!\"\n";
        Files.writeString(tempFile.toPath(), initialContent, StandardCharsets.UTF_8);

        // Reference config with new keys and comments from update
        String refYaml = "prefix: \"\"\n" +
                "cannot_tp_self: \"&cDefault cannot tp!\"\n" +
                "\n" +
                "# New economy commands\n" +
                "usage_playtime: \"&cИспользование: &e/playtime\"\n" +
                "playtime_self: \"&aВремя в игре: &e{time}\"\n";
        InputStream refStream = new ByteArrayInputStream(refYaml.getBytes(StandardCharsets.UTF_8));

        int added = LanguageManager.syncLanguageFile(tempFile, refStream, null);

        assertEquals(2, added, "Should detect 2 missing keys (usage_playtime, playtime_self)");

        String updatedContent = Files.readString(tempFile.toPath(), StandardCharsets.UTF_8);

        // Verify existing user customization is preserved
        assertTrue(updatedContent.contains("# Custom user comment"));
        assertTrue(updatedContent.contains("prefix: \"&c[Custom] &r\""));
        assertTrue(updatedContent.contains("cannot_tp_self: \"&cCannot tp to self!\""));

        // Verify new keys were added
        assertTrue(updatedContent.contains("usage_playtime: \"&cИспользование: &e/playtime\""));
        assertTrue(updatedContent.contains("playtime_self: \"&aВремя в игре: &e{time}\""));
        assertTrue(updatedContent.contains("# New economy commands"));

        // Verify with YamlConfiguration that keys are all readable
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(tempFile);
        assertEquals("&c[Custom] &r", yaml.getString("prefix"));
        assertEquals("&cCannot tp to self!", yaml.getString("cannot_tp_self"));
        assertEquals("&cИспользование: &e/playtime", yaml.getString("usage_playtime"));
        assertEquals("&aВремя в игре: &e{time}", yaml.getString("playtime_self"));

        // Second sync on the updated file should do NOTHING (returns 0)
        InputStream refStream2 = new ByteArrayInputStream(refYaml.getBytes(StandardCharsets.UTF_8));
        int secondAdded = LanguageManager.syncLanguageFile(tempFile, refStream2, null);
        assertEquals(0, secondAdded, "Subsequent sync must detect match and do nothing");
    }

    @Test
    public void testSyncLanguageFileFallbackToEnglish() throws IOException {
        // Disk file has partial keys
        String initialContent = "prefix: \"\"\n";
        Files.writeString(tempFile.toPath(), initialContent, StandardCharsets.UTF_8);

        // Primary stream (e.g. Russian) has key1
        String primaryYaml = "prefix: \"\"\n" +
                "key1: \"&aValue 1\"\n";
        // Fallback stream (English) has key1 and key2
        String fallbackYaml = "prefix: \"\"\n" +
                "key1: \"&aEnglish Value 1\"\n" +
                "key2: \"&cEnglish Value 2\"\n";

        InputStream primaryStream = new ByteArrayInputStream(primaryYaml.getBytes(StandardCharsets.UTF_8));
        InputStream fallbackStream = new ByteArrayInputStream(fallbackYaml.getBytes(StandardCharsets.UTF_8));

        int added = LanguageManager.syncLanguageFile(tempFile, primaryStream, fallbackStream);

        assertEquals(2, added);

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(tempFile);
        assertEquals("&aValue 1", yaml.getString("key1"));
        assertEquals("&cEnglish Value 2", yaml.getString("key2"));
    }
}
