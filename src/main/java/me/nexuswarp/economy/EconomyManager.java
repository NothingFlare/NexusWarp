package me.nexuswarp.economy;

import me.nexuswarp.NexusWarpPlugin;
import me.nexuswarp.util.ServerScheduler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyManager {

    public static class BalanceTopEntry {
        private final UUID uuid;
        private final String name;
        private final BigDecimal balance;

        public BalanceTopEntry(UUID uuid, String name, BigDecimal balance) {
            this.uuid = uuid;
            this.name = name;
            this.balance = balance;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public BigDecimal getBalance() {
            return balance;
        }
    }

    private final NexusWarpPlugin plugin;
    private final File userdataFolder;

    private final Map<UUID, BigDecimal> balanceCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> uuidToNameCache = new ConcurrentHashMap<>();
    private final Map<String, UUID> nameToUuidCache = new ConcurrentHashMap<>();

    private BigDecimal startingBalance = BigDecimal.ZERO;
    private BigDecimal minBalance = BigDecimal.ZERO;
    private BigDecimal maxBalance = new BigDecimal("10000000000000");
    private String currencySymbol = "$";
    private boolean symbolBehind = false;
    private BigDecimal minimumPayAmount = new BigDecimal("0.01");

    // BalanceTop cache
    private List<BalanceTopEntry> baltopCache = new ArrayList<>();
    private BigDecimal totalServerMoney = BigDecimal.ZERO;
    private long lastBaltopCalculateTime = 0L;
    private static final long BALTOP_CACHE_MILLIS = 2 * 60 * 1000L; // 2 minutes

    public EconomyManager(NexusWarpPlugin plugin) {
        this.plugin = plugin;
        this.userdataFolder = new File(plugin.getDataFolder(), "userdata");
        if (!userdataFolder.exists()) {
            userdataFolder.mkdirs();
        }
    }

    public void loadSettings() {
        this.startingBalance = new BigDecimal(plugin.getConfig().getString("economy.starting-balance", "0.00"));
        this.minBalance = new BigDecimal(plugin.getConfig().getString("economy.min-balance", "0.00"));
        this.maxBalance = new BigDecimal(plugin.getConfig().getString("economy.max-balance", "10000000000000.00"));
        this.currencySymbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        this.symbolBehind = plugin.getConfig().getBoolean("economy.currency-symbol-placed-behind", false);
        this.minimumPayAmount = new BigDecimal(plugin.getConfig().getString("economy.minimum-pay-amount", "0.01"));

        loadAllDataAsync();
    }

    private void loadAllDataAsync() {
        ServerScheduler.runAsync(plugin, () -> {
            File[] files = userdataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files == null) return;

            for (File file : files) {
                try {
                    String fileName = file.getName();
                    String uuidStr = fileName.substring(0, fileName.length() - 4);
                    UUID uuid = UUID.fromString(uuidStr);

                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                    String name = yaml.getString("name", "");
                    String moneyStr = yaml.getString("money", startingBalance.toPlainString());
                    BigDecimal money = new BigDecimal(moneyStr);

                    balanceCache.put(uuid, money);
                    if (!name.isEmpty()) {
                        uuidToNameCache.put(uuid, name);
                        nameToUuidCache.put(name.toLowerCase(), uuid);
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    public boolean hasAccount(UUID uuid) {
        if (uuid == null) return false;
        if (balanceCache.containsKey(uuid)) return true;
        File file = new File(userdataFolder, uuid + ".yml");
        return file.exists();
    }

    public boolean hasAccount(String playerName) {
        if (playerName == null || playerName.isEmpty()) return false;
        UUID uuid = getUuidByName(playerName);
        if (uuid != null) return true;
        Player p = Bukkit.getPlayerExact(playerName);
        return p != null && hasAccount(p.getUniqueId());
    }

    public void createAccount(UUID uuid, String name) {
        if (uuid == null) return;
        if (!hasAccount(uuid)) {
            balanceCache.put(uuid, startingBalance);
            if (name != null) {
                uuidToNameCache.put(uuid, name);
                nameToUuidCache.put(name.toLowerCase(), uuid);
            }
            saveUserAsync(uuid);
        }
    }

    public BigDecimal getBalance(UUID uuid) {
        if (uuid == null) return startingBalance;
        BigDecimal bal = balanceCache.get(uuid);
        if (bal != null) return bal;

        File file = new File(userdataFolder, uuid + ".yml");
        if (file.exists()) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                String moneyStr = yaml.getString("money", startingBalance.toPlainString());
                bal = new BigDecimal(moneyStr);
                balanceCache.put(uuid, bal);
                String name = yaml.getString("name", "");
                if (!name.isEmpty()) {
                    uuidToNameCache.put(uuid, name);
                    nameToUuidCache.put(name.toLowerCase(), uuid);
                }
                return bal;
            } catch (Exception ignored) {}
        }

        // Account doesn't exist yet, return starting balance
        balanceCache.put(uuid, startingBalance);
        return startingBalance;
    }

    public BigDecimal getBalance(String playerName) {
        UUID uuid = getUuidByName(playerName);
        if (uuid != null) {
            return getBalance(uuid);
        }
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        if (op != null && op.getUniqueId() != null) {
            return getBalance(op.getUniqueId());
        }
        return startingBalance;
    }

    public void setBalance(UUID uuid, BigDecimal amount) throws MaxMoneyException, NoLoanPermittedException {
        if (uuid == null || amount == null) return;
        amount = amount.setScale(2, RoundingMode.HALF_UP);

        if (amount.compareTo(maxBalance) > 0) {
            throw new MaxMoneyException();
        }
        if (amount.compareTo(minBalance) < 0) {
            throw new NoLoanPermittedException();
        }

        balanceCache.put(uuid, amount);
        saveUserAsync(uuid);
    }

    public void deposit(UUID uuid, BigDecimal amount) throws MaxMoneyException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;
        BigDecimal current = getBalance(uuid);
        BigDecimal next = current.add(amount);

        if (next.compareTo(maxBalance) > 0) {
            throw new MaxMoneyException();
        }

        try {
            setBalance(uuid, next);
        } catch (NoLoanPermittedException ignored) {}
    }

    public void withdraw(UUID uuid, BigDecimal amount) throws NoLoanPermittedException, MaxMoneyException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;
        BigDecimal current = getBalance(uuid);
        BigDecimal next = current.subtract(amount);

        if (next.compareTo(minBalance) < 0) {
            throw new NoLoanPermittedException();
        }

        setBalance(uuid, next);
    }

    public boolean hasEnough(UUID uuid, BigDecimal amount) {
        return getBalance(uuid).compareTo(amount) >= 0;
    }

    public void saveUserAsync(UUID uuid) {
        if (uuid == null) return;
        BigDecimal balance = balanceCache.get(uuid);
        String name = uuidToNameCache.get(uuid);

        ServerScheduler.runAsync(plugin, () -> {
            try {
                File file = new File(userdataFolder, uuid + ".yml");
                YamlConfiguration yaml = new YamlConfiguration();
                if (file.exists()) {
                    yaml.load(file);
                }
                if (balance != null) {
                    yaml.set("money", balance.toPlainString());
                }
                if (name != null && !name.isEmpty()) {
                    yaml.set("name", name);
                }
                yaml.set("last-seen", System.currentTimeMillis());
                yaml.save(file);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not save economy profile for " + uuid + ": " + e.getMessage());
            }
        });
    }

    public void saveAllSync() {
        for (Map.Entry<UUID, BigDecimal> entry : balanceCache.entrySet()) {
            UUID uuid = entry.getKey();
            BigDecimal balance = entry.getValue();
            String name = uuidToNameCache.get(uuid);
            try {
                File file = new File(userdataFolder, uuid + ".yml");
                YamlConfiguration yaml = new YamlConfiguration();
                if (file.exists()) {
                    yaml.load(file);
                }
                yaml.set("money", balance.toPlainString());
                if (name != null) {
                    yaml.set("name", name);
                }
                yaml.set("last-seen", System.currentTimeMillis());
                yaml.save(file);
            } catch (Exception ignored) {}
        }
    }

    public void updatePlayerCache(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        uuidToNameCache.put(uuid, name);
        nameToUuidCache.put(name.toLowerCase(), uuid);

        if (!hasAccount(uuid)) {
            createAccount(uuid, name);
        }
    }

    public UUID getUuidByName(String name) {
        if (name == null) return null;
        UUID cached = nameToUuidCache.get(name.toLowerCase());
        if (cached != null) return cached;

        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            updatePlayerCache(online);
            return online.getUniqueId();
        }

        // Search offline player
        OfflinePlayer op = Bukkit.getOfflinePlayer(name);
        if (op.hasPlayedBefore() || op.isOnline()) {
            return op.getUniqueId();
        }
        return null;
    }

    public String getNameByUuid(UUID uuid) {
        if (uuid == null) return "Unknown";
        String cached = uuidToNameCache.get(uuid);
        if (cached != null) return cached;

        Player p = Bukkit.getPlayer(uuid);
        if (p != null) return p.getName();

        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        if (op.getName() != null) return op.getName();

        return uuid.toString().substring(0, 8);
    }

    public String format(BigDecimal amount) {
        if (amount == null) amount = BigDecimal.ZERO;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        String formatted = df.format(amount);

        if (symbolBehind) {
            return formatted + currencySymbol;
        } else {
            return currencySymbol + formatted;
        }
    }

    public String format(double amount) {
        return format(BigDecimal.valueOf(amount));
    }

    public List<BalanceTopEntry> getBaltop() {
        if (System.currentTimeMillis() - lastBaltopCalculateTime > BALTOP_CACHE_MILLIS || baltopCache.isEmpty()) {
            calculateBaltop();
        }
        return baltopCache;
    }

    public BigDecimal getTotalServerMoney() {
        return totalServerMoney;
    }

    public synchronized void calculateBaltop() {
        List<BalanceTopEntry> list = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        // Ensure all files are processed
        File[] files = userdataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    String fileName = file.getName();
                    UUID uuid = UUID.fromString(fileName.substring(0, fileName.length() - 4));
                    BigDecimal bal = getBalance(uuid);
                    String name = getNameByUuid(uuid);
                    list.add(new BalanceTopEntry(uuid, name, bal));
                    total = total.add(bal);
                } catch (Exception ignored) {}
            }
        }

        list.sort((a, b) -> b.getBalance().compareTo(a.getBalance()));
        this.baltopCache = Collections.unmodifiableList(list);
        this.totalServerMoney = total;
        this.lastBaltopCalculateTime = System.currentTimeMillis();
    }

    public BigDecimal getStartingBalance() {
        return startingBalance;
    }

    public BigDecimal getMinBalance() {
        return minBalance;
    }

    public BigDecimal getMaxBalance() {
        return maxBalance;
    }

    public BigDecimal getMinimumPayAmount() {
        return minimumPayAmount;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }
}
