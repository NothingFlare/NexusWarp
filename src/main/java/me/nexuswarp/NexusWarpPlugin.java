package me.nexuswarp;

import me.nexuswarp.command.NexusWarpCommand;
import me.nexuswarp.command.TpAcceptCommand;
import me.nexuswarp.command.TpCancelCommand;
import me.nexuswarp.command.TpaCommand;
import me.nexuswarp.config.ConfigManager;
import me.nexuswarp.config.LanguageManager;
import me.nexuswarp.listener.PlayerEventListener;
import me.nexuswarp.manager.TpaManager;
import me.nexuswarp.util.ServerScheduler;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class NexusWarpPlugin extends JavaPlugin {

    private static NexusWarpPlugin instance;

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private TpaManager tpaManager;

    private ServerScheduler.TaskHandle tickerTask;

    @Override
    public void onEnable() {
        instance = this;

        // Load configs and language
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        this.languageManager = new LanguageManager(this);
        this.languageManager.load();

        // Initialize TPA Manager
        this.tpaManager = new TpaManager(this);

        // Register event listener
        Bukkit.getPluginManager().registerEvents(new PlayerEventListener(this), this);

        // Register commands
        registerCommands();

        // Start asynchronous 1-second ticker task for expiration (if enabled)
        this.tickerTask = ServerScheduler.runTimerAsync(this, () -> {
            tpaManager.tickExpire();
        }, 1L, 1L);

        // Print startup banner matching Photo 1
        printBanner(false);
    }

    private void registerCommands() {
        TpaCommand tpaCmd = new TpaCommand(this);
        PluginCommand tpa = getCommand("tpa");
        if (tpa != null) {
            tpa.setExecutor(tpaCmd);
            tpa.setTabCompleter(tpaCmd);
        }

        TpAcceptCommand tpAcceptCmd = new TpAcceptCommand(this);
        PluginCommand tpaccept = getCommand("tpaccept");
        if (tpaccept != null) {
            tpaccept.setExecutor(tpAcceptCmd);
            tpaccept.setTabCompleter(tpAcceptCmd);
        }

        TpCancelCommand tpCancelCmd = new TpCancelCommand(this);
        PluginCommand tpacancel = getCommand("tpacancel");
        if (tpacancel != null) {
            tpacancel.setExecutor(tpCancelCmd);
            tpacancel.setTabCompleter(tpCancelCmd);
        }

        NexusWarpCommand nwCmd = new NexusWarpCommand(this);
        PluginCommand nw = getCommand("nexuswarp");
        if (nw != null) {
            nw.setExecutor(nwCmd);
            nw.setTabCompleter(nwCmd);
        }
    }

    @Override
    public void onDisable() {
        if (tickerTask != null) {
            tickerTask.cancel();
            tickerTask = null;
        }

        if (tpaManager != null) {
            tpaManager.clearAll();
        }

        getLogger().info("NexusWarp has been disabled!");
    }

    public void reloadPluginAsync(Runnable onComplete) {
        ServerScheduler.runAsync(this, () -> {
            configManager.load();
            languageManager.load();
            printBanner(true);
            if (onComplete != null) {
                ServerScheduler.runGlobal(this, onComplete);
            }
        });
    }

    public void printBanner(boolean isReload) {
        getLogger().info("");
        getLogger().info("  _   _                       _    _                 ");
        getLogger().info(" | \\ | |                     | |  | |                ");
        getLogger().info(" |  \\| | _____  ___   _ ___  | |  | | __ _ _ __ _ __ ");
        getLogger().info(" | . ` |/ _ \\ \\/ / | | / __| | |/\\| |/ _` | '__| '_ \\");
        getLogger().info(" | |\\  |  __/>  <| |_| \\__ \\ \\  /\\  / (_| | |  | |_) |");
        getLogger().info(" |_| \\_|\\___/_/\\_\\\\__,_|___/  \\/  \\/ \\__,_|_|  | .__/ ");
        getLogger().info("                                               | |   ");
        getLogger().info("                                               |_|   ");
        getLogger().info("");
        getLogger().info(" Привет, мир!");
        getLogger().info(" Наш Telegram канал!");
        getLogger().info(" t.me/lighthouse_top");
        getLogger().info(" Версия NexusWarp: " + getDescription().getVersion());
        getLogger().info(" Язык: " + (languageManager != null ? languageManager.getCurrentLanguage().toUpperCase() : "RU"));
        String platform = ServerScheduler.isFolia() ? "Folia" : Bukkit.getName();
        getLogger().info(" NexusWarp запущен на " + platform);
        if (isReload) {
            getLogger().info(" NexusWarp успешно перезагружен!");
        } else {
            getLogger().info(" NexusWarp успешно включен!");
        }
        getLogger().info("");
    }

    public static NexusWarpPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public TpaManager getTpaManager() {
        return tpaManager;
    }
}
