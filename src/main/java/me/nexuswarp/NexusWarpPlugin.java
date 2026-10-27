package me.nexuswarp;

import me.nexuswarp.command.MsgCommand;
import me.nexuswarp.command.NexusWarpCommand;
import me.nexuswarp.command.ReplyCommand;
import me.nexuswarp.command.TpAcceptCommand;
import me.nexuswarp.command.TpCancelCommand;
import me.nexuswarp.command.TpCommand;
import me.nexuswarp.command.TpHereCommand;
import me.nexuswarp.command.TpaCommand;
import me.nexuswarp.command.eco.BalanceCommand;
import me.nexuswarp.command.eco.BalanceTopCommand;
import me.nexuswarp.command.eco.EcoCommand;
import me.nexuswarp.command.eco.PayCommand;
import me.nexuswarp.config.ConfigManager;
import me.nexuswarp.config.LanguageManager;
import me.nexuswarp.economy.Economy;
import me.nexuswarp.economy.EconomyManager;
import me.nexuswarp.economy.vault.VaultEconomyProvider;
import me.nexuswarp.listener.PlayerEventListener;
import me.nexuswarp.manager.MessageManager;
import me.nexuswarp.manager.TpaManager;
import me.nexuswarp.update.UpdateChecker;
import me.nexuswarp.util.ServerScheduler;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class NexusWarpPlugin extends JavaPlugin {

    private static NexusWarpPlugin instance;

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private TpaManager tpaManager;
    private MessageManager messageManager;
    private EconomyManager economyManager;
    private VaultEconomyProvider vaultEconomyProvider;

    private ServerScheduler.TaskHandle tickerTask;

    @Override
    public void onEnable() {
        instance = this;

        // Load configs and language
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        this.languageManager = new LanguageManager(this);
        this.languageManager.load();

        // Initialize Managers
        this.tpaManager = new TpaManager(this);
        this.messageManager = new MessageManager(this);

        // Initialize Economy
        this.economyManager = new EconomyManager(this);
        this.economyManager.loadSettings();
        Economy.setManager(this.economyManager);

        // Hook into Vault if present
        setupVault();

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

        // Check for updates asynchronously
        if (getConfig().getBoolean("updates.check-for-updates", true)) {
            new UpdateChecker(getDescription().getVersion(), getLogger(), languageManager).checkAsync();
        }
    }

    private void setupVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            try {
                this.vaultEconomyProvider = new VaultEconomyProvider(this);
                Bukkit.getServicesManager().register(
                        net.milkbowl.vault.economy.Economy.class,
                        vaultEconomyProvider,
                        this,
                        ServicePriority.Highest
                );
                getLogger().info("Successfully hooked into Vault as the primary Economy provider!");
            } catch (Throwable t) {
                getLogger().warning("Vault found, but failed to register economy provider: " + t.getMessage());
            }
        }
    }

    private void registerCommands() {
        // TPA commands
        registerCommand("tpa", new TpaCommand(this));
        registerCommand("tpaccept", new TpAcceptCommand(this));
        registerCommand("tpacancel", new TpCancelCommand(this));

        // Direct TP commands
        registerCommand("tp", new TpCommand(this));
        registerCommand("tphere", new TpHereCommand(this));

        // Messaging commands
        registerCommand("msg", new MsgCommand(this));
        registerCommand("reply", new ReplyCommand(this));

        // Economy commands
        registerCommand("eco", new EcoCommand(this));
        registerCommand("balance", new BalanceCommand(this));
        registerCommand("pay", new PayCommand(this));
        registerCommand("balancetop", new BalanceTopCommand(this));

        // Admin command
        registerCommand("nexuswarp", new NexusWarpCommand(this));
    }

    private void registerCommand(String name, Object executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            if (executor instanceof org.bukkit.command.CommandExecutor ce) {
                cmd.setExecutor(ce);
            }
            if (executor instanceof org.bukkit.command.TabCompleter tc) {
                cmd.setTabCompleter(tc);
            }
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

        if (economyManager != null) {
            economyManager.saveAllSync();
        }

        if (vaultEconomyProvider != null) {
            Bukkit.getServicesManager().unregister(net.milkbowl.vault.economy.Economy.class, vaultEconomyProvider);
        }

        getLogger().info("NexusWarp has been disabled!");
    }

    public void reloadPluginAsync(Runnable onComplete) {
        ServerScheduler.runAsync(this, () -> {
            configManager.load();
            languageManager.load();
            economyManager.loadSettings();
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

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }
}
