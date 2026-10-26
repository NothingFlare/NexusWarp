package me.nexuswarp.config;

import me.nexuswarp.NexusWarpPlugin;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigManager {

    private final NexusWarpPlugin plugin;

    private String language;
    private boolean disabledWorldsEnabled;
    private boolean ignoreWorldBypass;
    private final Set<String> disabledWorlds = new HashSet<>();

    private int requestTimeout;
    private int teleportDelay;
    private boolean cancelOnMovement;
    private boolean cancelOnDamage;

    private boolean safeTeleportEnabled;
    private boolean preventLava;
    private boolean preventVoid;
    private boolean preventSuffocation;

    public ConfigManager(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.language = config.getString("language", "ru").toLowerCase();

        this.disabledWorldsEnabled = config.getBoolean("disabled-worlds.enabled", false);
        this.ignoreWorldBypass = config.getBoolean("disabled-worlds.ignore-bypass", true);
        this.disabledWorlds.clear();
        List<String> worldsList = config.getStringList("disabled-worlds.worlds");
        if (worldsList != null) {
            for (String w : worldsList) {
                if (w != null && !w.trim().isEmpty()) {
                    this.disabledWorlds.add(w.trim().toLowerCase());
                }
            }
        }

        this.requestTimeout = Math.max(0, config.getInt("requests.timeout", 0));
        this.teleportDelay = Math.max(0, config.getInt("requests.teleport-delay", 0));
        this.cancelOnMovement = config.getBoolean("requests.cancel-on-movement", true);
        this.cancelOnDamage = config.getBoolean("requests.cancel-on-damage", true);

        this.safeTeleportEnabled = config.getBoolean("safe-teleport.enabled", true);
        this.preventLava = config.getBoolean("safe-teleport.prevent-lava", true);
        this.preventVoid = config.getBoolean("safe-teleport.prevent-void", true);
        this.preventSuffocation = config.getBoolean("safe-teleport.prevent-suffocation", true);
    }

    public boolean isWorldDisabled(World world) {
        if (!disabledWorldsEnabled || world == null) {
            return false;
        }
        String name = world.getName().toLowerCase();
        String env = world.getEnvironment().name().toLowerCase();
        return disabledWorlds.contains(name) || disabledWorlds.contains(env);
    }

    public boolean isWorldDisabled(String worldName) {
        if (!disabledWorldsEnabled || worldName == null) {
            return false;
        }
        return disabledWorlds.contains(worldName.toLowerCase());
    }

    public String getLanguage() {
        return language;
    }

    public boolean isDisabledWorldsEnabled() {
        return disabledWorldsEnabled;
    }

    public boolean isIgnoreWorldBypass() {
        return ignoreWorldBypass;
    }

    public Set<String> getDisabledWorlds() {
        return disabledWorlds;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public int getTeleportDelay() {
        return teleportDelay;
    }

    public boolean isCancelOnMovement() {
        return cancelOnMovement;
    }

    public boolean isCancelOnDamage() {
        return cancelOnDamage;
    }

    public boolean isSafeTeleportEnabled() {
        return safeTeleportEnabled;
    }

    public boolean isPreventLava() {
        return preventLava;
    }

    public boolean isPreventVoid() {
        return preventVoid;
    }

    public boolean isPreventSuffocation() {
        return preventSuffocation;
    }
}
