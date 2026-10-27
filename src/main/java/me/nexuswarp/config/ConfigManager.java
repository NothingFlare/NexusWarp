package me.nexuswarp.config;

import me.nexuswarp.NexusWarpPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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

    private boolean spawnEnabled;
    private String spawnWorld;
    private double spawnX;
    private double spawnY;
    private double spawnZ;
    private float spawnYaw;
    private float spawnPitch;

    private boolean hubEnabled;
    private String hubWorld;
    private double hubX;
    private double hubY;
    private double hubZ;
    private float hubYaw;
    private float hubPitch;

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

        // Spawn
        this.spawnEnabled = config.getBoolean("spawn.enabled", false);
        this.spawnWorld = config.getString("spawn.world", "world");
        this.spawnX = config.getDouble("spawn.x", 0.5);
        this.spawnY = config.getDouble("spawn.y", 64.0);
        this.spawnZ = config.getDouble("spawn.z", 0.5);
        this.spawnYaw = (float) config.getDouble("spawn.yaw", 0.0);
        this.spawnPitch = (float) config.getDouble("spawn.pitch", 0.0);

        // Hub
        this.hubEnabled = config.getBoolean("hub.enabled", false);
        this.hubWorld = config.getString("hub.world", "world");
        this.hubX = config.getDouble("hub.x", 0.5);
        this.hubY = config.getDouble("hub.y", 64.0);
        this.hubZ = config.getDouble("hub.z", 0.5);
        this.hubYaw = (float) config.getDouble("hub.yaw", 0.0);
        this.hubPitch = (float) config.getDouble("hub.pitch", 0.0);
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

    public boolean isSpawnEnabled() {
        return spawnEnabled;
    }

    public String getSpawnWorldName() {
        return spawnWorld;
    }

    public Location getSpawnLocation() {
        World world = Bukkit.getWorld(spawnWorld);
        if (world == null) return null;
        return new Location(world, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
    }

    public void setSpawnLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        this.spawnEnabled = true;
        this.spawnWorld = loc.getWorld().getName();
        this.spawnX = Math.round(loc.getX() * 100.0) / 100.0;
        this.spawnY = Math.round(loc.getY() * 100.0) / 100.0;
        this.spawnZ = Math.round(loc.getZ() * 100.0) / 100.0;
        this.spawnYaw = Math.round(loc.getYaw() * 10.0f) / 10.0f;
        this.spawnPitch = Math.round(loc.getPitch() * 10.0f) / 10.0f;

        FileConfiguration config = plugin.getConfig();
        config.set("spawn.enabled", true);
        config.set("spawn.world", spawnWorld);
        config.set("spawn.x", spawnX);
        config.set("spawn.y", spawnY);
        config.set("spawn.z", spawnZ);
        config.set("spawn.yaw", spawnYaw);
        config.set("spawn.pitch", spawnPitch);
        plugin.saveConfig();
    }

    public boolean isHubEnabled() {
        return hubEnabled;
    }

    public String getHubWorldName() {
        return hubWorld;
    }

    public Location getHubLocation() {
        World world = Bukkit.getWorld(hubWorld);
        if (world == null) return null;
        return new Location(world, hubX, hubY, hubZ, hubYaw, hubPitch);
    }

    public void setHubLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        this.hubEnabled = true;
        this.hubWorld = loc.getWorld().getName();
        this.hubX = Math.round(loc.getX() * 100.0) / 100.0;
        this.hubY = Math.round(loc.getY() * 100.0) / 100.0;
        this.hubZ = Math.round(loc.getZ() * 100.0) / 100.0;
        this.hubYaw = Math.round(loc.getYaw() * 10.0f) / 10.0f;
        this.hubPitch = Math.round(loc.getPitch() * 10.0f) / 10.0f;

        FileConfiguration config = plugin.getConfig();
        config.set("hub.enabled", true);
        config.set("hub.world", hubWorld);
        config.set("hub.x", hubX);
        config.set("hub.y", hubY);
        config.set("hub.z", hubZ);
        config.set("hub.yaw", hubYaw);
        config.set("hub.pitch", hubPitch);
        plugin.saveConfig();
    }
}
