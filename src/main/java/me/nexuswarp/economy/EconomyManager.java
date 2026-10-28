package me.nexuswarp.economy;

import me.nexuswarp.NexusWarpPlugin;
import me.nexuswarp.economy.event.UserBalanceUpdateEvent;
import me.nexuswarp.util.ServerScheduler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class EconomyManager {

    public static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;

    private final NexusWarpPlugin plugin;
    private final File userdataFolder;

    // In-memory thread-safe caches
    private final Map<UUID, UserAccount> accounts = new ConcurrentHashMap<>();
    private final Map<String, UUID> nameToUuid = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, BigDecimal>> pendingPayConfirmations = new ConcurrentHashMap<>();

    // Settings
    private boolean enabled = true;
    private BigDecimal startingBalance = BigDecimal.ZERO;
    private BigDecimal minBalance = BigDecimal.ZERO;
    private BigDecimal maxBalance = new BigDecimal("10000000000000");
    private String currencySymbol = "$";
    private boolean symbolBehind = false;
    private BigDecimal minimumPayAmount = new BigDecimal("0.01");

    // Baltop settings & cache
    private int baltopInterval = 60;
    private boolean baltopShowZero = true;
    private BigDecimal baltopMinBalance = BigDecimal.ZERO;
    private int baltopEntryLimit = -1;
    private boolean baltopShowNpcs = false;

    // Smart incremental baltop data structures
    private final Object baltopLock = new Object();
    private final Map<UUID, BalanceTopEntry> baltopEntryMap = new ConcurrentHashMap<>();
    private final List<BalanceTopEntry> baltopSortedList = new ArrayList<>();

    private final AtomicBoolean isCalculatingBaltop = new AtomicBoolean(false);
    private volatile List<BalanceTopEntry> cachedBaltop = Collections.emptyList();
    private volatile BigDecimal cachedTotalServerMoney = BigDecimal.ZERO;
    private volatile long lastBaltopCalculateTime = 0L;

    public static final Comparator<BalanceTopEntry> BALTOP_COMPARATOR = (a, b) -> {
        int c = b.getBalance().compareTo(a.getBalance());
        if (c != 0) return c;
        return a.getUuid().compareTo(b.getUuid());
    };

    public static class BalanceTopEntry {
        private final UUID uuid;
        private final String name;
        private final BigDecimal balance;

        public BalanceTopEntry(UUID uuid, String name, BigDecimal balance) {
            this.uuid = Objects.requireNonNull(uuid, "uuid cannot be null");
            this.name = name != null ? name : uuid.toString().substring(0, 8);
            this.balance = balance != null ? balance : BigDecimal.ZERO;
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BalanceTopEntry that = (BalanceTopEntry) o;
            return uuid.equals(that.uuid);
        }

        @Override
        public int hashCode() {
            return uuid.hashCode();
        }
    }

    public EconomyManager(NexusWarpPlugin plugin) {
        this.plugin = plugin;
        this.userdataFolder = new File(plugin.getDataFolder(), "userdata");
        if (!userdataFolder.exists()) {
            userdataFolder.mkdirs();
        }
    }

    public void loadSettings() {
        this.enabled = plugin.getConfig().getBoolean("economy.enabled", true);
        this.startingBalance = new BigDecimal(plugin.getConfig().getString("economy.starting-balance", "0.00"));
        
        BigDecimal min = new BigDecimal(plugin.getConfig().getString("economy.min-balance", "0.00"));
        if (min.signum() > 0) {
            min = min.negate();
        }
        this.minBalance = min;

        this.maxBalance = new BigDecimal(plugin.getConfig().getString("economy.max-balance", "10000000000000.00"));
        this.currencySymbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        this.symbolBehind = plugin.getConfig().getBoolean("economy.currency-symbol-placed-behind", false);
        this.minimumPayAmount = new BigDecimal(plugin.getConfig().getString("economy.minimum-pay-amount", "0.01"));

        this.baltopInterval = Math.max(5, plugin.getConfig().getInt("economy.baltop-update-interval", 60));
        this.baltopShowZero = plugin.getConfig().getBoolean("economy.baltop-show-zero", true);
        this.baltopMinBalance = new BigDecimal(plugin.getConfig().getString("economy.baltop-min-balance", "0.00"));
        this.baltopEntryLimit = plugin.getConfig().getInt("economy.baltop-entry-limit", -1);
        this.baltopShowNpcs = plugin.getConfig().getBoolean("economy.baltop-show-npcs", false);

        loadAllDataAsync();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAvailable() {
        return enabled;
    }

    // -------------------------------------------------------------
    // Data Loading & Storage
    // -------------------------------------------------------------

    public void loadAllDataAsync() {
        ServerScheduler.runAsync(plugin, () -> {
            File[] files = userdataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    try {
                        String fileName = file.getName();
                        UUID uuid = UUID.fromString(fileName.substring(0, fileName.length() - 4));
                        loadUserFile(uuid, file);
                    } catch (Throwable ignored) {}
                }
            }
            // Index all online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                onPlayerJoin(player);
            }
            // Initial Baltop calculation
            calculateBaltop();
        });
    }

    private UserAccount loadUserFile(UUID uuid, File file) {
        if (!file.exists()) {
            return null;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String name = yaml.getString("last-account-name");
        if (name == null || name.isEmpty()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            name = op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);
        }

        BigDecimal balance;
        String rawMoney = yaml.getString("money");
        if (rawMoney != null) {
            try {
                balance = new BigDecimal(rawMoney);
            } catch (Exception e) {
                balance = startingBalance;
            }
        } else {
            balance = startingBalance;
        }

        UserAccount account = new UserAccount(uuid, name, balance);
        account.setNpc(yaml.getBoolean("npc", false));
        account.setNpcName(yaml.getString("npc-name", null));
        account.setAcceptingPay(yaml.getBoolean("accepting-pay", true));
        account.setPromptPayConfirm(yaml.getBoolean("confirm-pay", false));
        account.setDirty(false);

        accounts.put(uuid, account);
        if (name != null) {
            nameToUuid.put(name.toLowerCase(Locale.ROOT), uuid);
        }
        return account;
    }

    public UserAccount getAccount(UUID uuid) {
        if (uuid == null) return null;
        UserAccount account = accounts.get(uuid);
        if (account != null) return account;

        File file = new File(userdataFolder, uuid + ".yml");
        if (file.exists()) {
            return loadUserFile(uuid, file);
        }
        return null;
    }

    public UserAccount getOrCreateAccount(UUID uuid, String name) {
        if (uuid == null) return null;
        UserAccount account = getAccount(uuid);
        if (account == null) {
            account = createAccount(uuid, name);
        } else if (name != null && !name.equalsIgnoreCase(account.getLastAccountName())) {
            nameToUuid.remove(account.getLastAccountName().toLowerCase(Locale.ROOT));
            account.setLastAccountName(name);
            nameToUuid.put(name.toLowerCase(Locale.ROOT), uuid);
            saveAccountAsync(account);
            updateBaltopName(uuid, name);
        }
        return account;
    }

    public UserAccount createAccount(UUID uuid, String name) {
        if (uuid == null) return null;
        String resolvedName = name != null ? name : Bukkit.getOfflinePlayer(uuid).getName();
        if (resolvedName == null) resolvedName = uuid.toString().substring(0, 8);

        UserAccount account = new UserAccount(uuid, resolvedName, startingBalance);
        accounts.put(uuid, account);
        nameToUuid.put(resolvedName.toLowerCase(Locale.ROOT), uuid);
        saveAccountAsync(account);
        updateBaltopForUser(uuid, resolvedName, BigDecimal.ZERO, startingBalance);
        return account;
    }

    public void onPlayerJoin(Player player) {
        if (player == null) return;
        getOrCreateAccount(player.getUniqueId(), player.getName());
    }

    public void saveAccountAsync(UserAccount account) {
        if (account == null) return;
        ServerScheduler.runAsync(plugin, () -> saveAccountBlocking(account));
    }

    public synchronized void saveAccountBlocking(UserAccount account) {
        if (account == null) return;
        File file = new File(userdataFolder, account.getUuid() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("last-account-name", account.getLastAccountName());
        yaml.set("money", account.getBalance().toPlainString());
        yaml.set("npc", account.isNpc());
        if (account.getNpcName() != null) {
            yaml.set("npc-name", account.getNpcName());
        }
        yaml.set("accepting-pay", account.isAcceptingPay());
        yaml.set("confirm-pay", account.isPromptPayConfirm());

        try {
            yaml.save(file);
            account.setDirty(false);
        } catch (IOException e) {
            plugin.getLogger().warning("[NexusWarp] Failed to save user data for " + account.getUuid() + ": " + e.getMessage());
        }
    }

    public void saveAllBlocking() {
        for (UserAccount account : accounts.values()) {
            if (account.isDirty()) {
                saveAccountBlocking(account);
            }
        }
    }

    // -------------------------------------------------------------
    // Name & UUID Resolution
    // -------------------------------------------------------------

    public UUID getUuidByName(String name) {
        if (name == null || name.isEmpty()) return null;
        UUID cached = nameToUuid.get(name.toLowerCase(Locale.ROOT));
        if (cached != null) return cached;

        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            onPlayerJoin(online);
            return online.getUniqueId();
        }

        OfflinePlayer op = Bukkit.getOfflinePlayer(name);
        if (op.hasPlayedBefore() || op.isOnline()) {
            return op.getUniqueId();
        }

        return null;
    }

    public String getNameByUuid(UUID uuid) {
        if (uuid == null) return "Unknown";
        UserAccount account = accounts.get(uuid);
        if (account != null && account.getLastAccountName() != null) {
            return account.getLastAccountName();
        }
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        if (op.getName() != null) return op.getName();
        return uuid.toString().substring(0, 8);
    }

    public boolean hasAccount(UUID uuid) {
        if (uuid == null) return false;
        return accounts.containsKey(uuid) || new File(userdataFolder, uuid + ".yml").exists();
    }

    public boolean hasAccount(String name) {
        if (name == null || name.isEmpty()) return false;
        UUID uuid = getUuidByName(name);
        return uuid != null && hasAccount(uuid);
    }

    // -------------------------------------------------------------
    // Balance & Economy Operations
    // -------------------------------------------------------------

    public BigDecimal getBalance(UUID uuid) {
        if (uuid == null) return startingBalance;
        UserAccount account = getAccount(uuid);
        return account != null ? account.getBalance() : startingBalance;
    }

    public BigDecimal getBalance(String name) {
        UUID uuid = getUuidByName(name);
        return uuid != null ? getBalance(uuid) : startingBalance;
    }

    public BigDecimal getBalance(OfflinePlayer player) {
        return player != null ? getBalance(player.getUniqueId()) : startingBalance;
    }

    public boolean hasEnough(UUID uuid, BigDecimal amount) {
        if (amount == null) return false;
        return getBalance(uuid).compareTo(amount) >= 0;
    }

    public boolean canAfford(UUID uuid, BigDecimal cost, boolean hasLoanPerm) {
        if (cost == null) return false;
        BigDecimal remaining = getBalance(uuid).subtract(cost, MATH_CONTEXT);
        if (hasLoanPerm) {
            return remaining.compareTo(minBalance) >= 0;
        }
        return remaining.signum() >= 0;
    }

    public synchronized void setBalance(UUID uuid, BigDecimal newBalance, UserBalanceUpdateEvent.Cause cause)
            throws NoLoanPermittedException, MaxMoneyException {
        if (uuid == null) return;
        if (newBalance == null) newBalance = BigDecimal.ZERO;

        if (newBalance.compareTo(maxBalance) > 0) {
            throw new MaxMoneyException();
        }
        if (newBalance.compareTo(minBalance) < 0) {
            throw new NoLoanPermittedException();
        }

        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        boolean hasLoan = op.isOnline() && op.getPlayer() != null &&
                (op.getPlayer().hasPermission("nexuswarp.eco.loan") || op.getPlayer().hasPermission("essentials.eco.loan"));

        if (newBalance.signum() < 0 && !hasLoan) {
            throw new NoLoanPermittedException();
        }

        UserAccount account = getOrCreateAccount(uuid, op.getName());
        BigDecimal oldBalance = account.getBalance();

        // Fire UserBalanceUpdateEvent
        UserBalanceUpdateEvent event = new UserBalanceUpdateEvent(op, oldBalance, newBalance, cause);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        BigDecimal finalBalance = event.getNewBalance();
        if (finalBalance.compareTo(maxBalance) > 0) {
            throw new MaxMoneyException();
        }
        if (finalBalance.compareTo(minBalance) < 0) {
            throw new NoLoanPermittedException();
        }

        account.setBalance(finalBalance);
        saveAccountAsync(account);

        // Smart incremental baltop update only for this user
        updateBaltopForUser(uuid, account.getLastAccountName(), oldBalance, finalBalance);
    }

    public synchronized void deposit(UUID uuid, BigDecimal amount, UserBalanceUpdateEvent.Cause cause)
            throws MaxMoneyException {
        if (uuid == null || amount == null) return;
        if (amount.signum() < 0) {
            try {
                withdraw(uuid, amount.abs(), false, cause);
            } catch (NoLoanPermittedException e) {
                plugin.getLogger().warning("[NexusWarp] Loan not permitted for negative deposit: " + e.getMessage());
            }
            return;
        }

        BigDecimal current = getBalance(uuid);
        BigDecimal target = current.add(amount, MATH_CONTEXT);
        try {
            setBalance(uuid, target, cause);
        } catch (NoLoanPermittedException ignored) {}
    }

    public synchronized void withdraw(UUID uuid, BigDecimal amount, boolean checkLoanPerm, UserBalanceUpdateEvent.Cause cause)
            throws NoLoanPermittedException, MaxMoneyException {
        if (uuid == null || amount == null) return;
        if (amount.signum() < 0) {
            deposit(uuid, amount.abs(), cause);
            return;
        }

        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        boolean hasLoan = !checkLoanPerm || (op.isOnline() && op.getPlayer() != null &&
                (op.getPlayer().hasPermission("nexuswarp.eco.loan") || op.getPlayer().hasPermission("essentials.eco.loan")));

        if (!canAfford(uuid, amount, hasLoan)) {
            throw new NoLoanPermittedException();
        }

        BigDecimal current = getBalance(uuid);
        BigDecimal target = current.subtract(amount, MATH_CONTEXT);
        setBalance(uuid, target, cause);
    }

    public synchronized void multiply(UUID uuid, BigDecimal amount, UserBalanceUpdateEvent.Cause cause)
            throws NoLoanPermittedException, MaxMoneyException {
        if (uuid == null || amount == null) return;
        BigDecimal current = getBalance(uuid);
        BigDecimal target = current.multiply(amount, MATH_CONTEXT);
        setBalance(uuid, target, cause);
    }

    public synchronized void divide(UUID uuid, BigDecimal amount, UserBalanceUpdateEvent.Cause cause)
            throws NoLoanPermittedException, MaxMoneyException {
        if (uuid == null || amount == null) return;
        if (amount.signum() == 0) {
            throw new ArithmeticException("Division by zero");
        }
        BigDecimal current = getBalance(uuid);
        BigDecimal target = current.divide(amount, MATH_CONTEXT);
        setBalance(uuid, target, cause);
    }

    public synchronized void resetBalance(UUID uuid, UserBalanceUpdateEvent.Cause cause)
            throws NoLoanPermittedException, MaxMoneyException {
        if (uuid == null) return;
        setBalance(uuid, startingBalance, cause);
    }

    // -------------------------------------------------------------
    // NPC Accounts
    // -------------------------------------------------------------

    public boolean isNPC(UUID uuid) {
        UserAccount account = getAccount(uuid);
        return account != null && account.isNpc();
    }

    public boolean isNPC(String name) {
        UUID uuid = getUuidByName(name);
        return uuid != null && isNPC(uuid);
    }

    public synchronized boolean createNPC(String name) {
        if (name == null || name.isEmpty() || hasAccount(name)) {
            return false;
        }

        UUID npcUUID = UUID.nameUUIDFromBytes(("NPC:" + name).getBytes(StandardCharsets.UTF_8));
        UserAccount account = new UserAccount(npcUUID, name, startingBalance);
        account.setNpc(true);
        account.setNpcName(name);

        accounts.put(npcUUID, account);
        nameToUuid.put(name.toLowerCase(Locale.ROOT), npcUUID);
        saveAccountBlocking(account);
        updateBaltopForUser(npcUUID, name, BigDecimal.ZERO, startingBalance);
        return true;
    }

    public synchronized void removeNPC(String name) throws UserDoesNotExistException {
        if (name == null || name.isEmpty()) {
            throw new UserDoesNotExistException(name);
        }

        UUID uuid = getUuidByName(name);
        if (uuid == null) {
            uuid = UUID.nameUUIDFromBytes(("NPC:" + name).getBytes(StandardCharsets.UTF_8));
        }

        UserAccount account = getAccount(uuid);
        if (account == null || !account.isNpc()) {
            throw new UserDoesNotExistException(name);
        }

        accounts.remove(uuid);
        nameToUuid.remove(name.toLowerCase(Locale.ROOT));
        File file = new File(userdataFolder, uuid + ".yml");
        if (file.exists()) {
            file.delete();
        }
        removeBaltopForUser(uuid);
    }

    // -------------------------------------------------------------
    // Pay Toggle & Confirmation
    // -------------------------------------------------------------

    public boolean isAcceptingPay(UUID uuid) {
        UserAccount account = getAccount(uuid);
        return account == null || account.isAcceptingPay();
    }

    public void setAcceptingPay(UUID uuid, boolean accepting) {
        UserAccount account = getOrCreateAccount(uuid, null);
        if (account != null) {
            account.setAcceptingPay(accepting);
            saveAccountAsync(account);
        }
    }

    public boolean isPromptingPayConfirm(UUID uuid) {
        UserAccount account = getAccount(uuid);
        return account != null && account.isPromptPayConfirm();
    }

    public void setPromptingPayConfirm(UUID uuid, boolean prompt) {
        UserAccount account = getOrCreateAccount(uuid, null);
        if (account != null) {
            account.setPromptPayConfirm(prompt);
            saveAccountAsync(account);
        }
    }

    public BigDecimal getPendingPayConfirm(UUID sender, UUID target) {
        Map<UUID, BigDecimal> map = pendingPayConfirmations.get(sender);
        return map != null ? map.get(target) : null;
    }

    public void setPendingPayConfirm(UUID sender, UUID target, BigDecimal amount) {
        pendingPayConfirmations.computeIfAbsent(sender, k -> new ConcurrentHashMap<>()).put(target, amount);
    }

    public void clearPendingPayConfirm(UUID sender) {
        pendingPayConfirmations.remove(sender);
    }

    public void removePendingPayConfirm(UUID sender, UUID target) {
        Map<UUID, BigDecimal> map = pendingPayConfirmations.get(sender);
        if (map != null) {
            map.remove(target);
        }
    }

    // -------------------------------------------------------------
    // Smart Baltop Cache & Incremental Updates
    // -------------------------------------------------------------

    public int getBaltopInterval() {
        return baltopInterval;
    }

    public List<BalanceTopEntry> getBaltop() {
        if (cachedBaltop.isEmpty() && lastBaltopCalculateTime == 0L) {
            calculateBaltop();
        }
        return cachedBaltop;
    }

    public BigDecimal getTotalServerMoney() {
        return cachedTotalServerMoney;
    }

    public long getLastBaltopCalculateTime() {
        return lastBaltopCalculateTime;
    }

    public boolean isCalculatingBaltop() {
        return isCalculatingBaltop.get();
    }

    public void updateBaltopAsync() {
        if (!isCalculatingBaltop.compareAndSet(false, true)) {
            return;
        }

        ServerScheduler.runAsync(plugin, () -> {
            try {
                calculateBaltop();
            } catch (Throwable t) {
                plugin.getLogger().warning("[NexusWarp] Error while updating balancetop: " + t.getMessage());
            } finally {
                isCalculatingBaltop.set(false);
            }
        });
    }

    /**
     * Performs a full baseline recalculation of baltop from all accounts.
     */
    public void calculateBaltop() {
        synchronized (baltopLock) {
            baltopEntryMap.clear();
            baltopSortedList.clear();
            BigDecimal total = BigDecimal.ZERO;

            for (UserAccount account : accounts.values()) {
                if (account == null) continue;
                BigDecimal bal = account.getBalance();
                total = total.add(bal, MATH_CONTEXT);

                if (!qualifiesForBaltop(account, bal)) {
                    continue;
                }

                BalanceTopEntry entry = new BalanceTopEntry(account.getUuid(), account.getLastAccountName(), bal);
                baltopEntryMap.put(account.getUuid(), entry);
                baltopSortedList.add(entry);
            }

            baltopSortedList.sort(BALTOP_COMPARATOR);
            updateCachedBaltopSnapshot();

            this.cachedTotalServerMoney = total;
            this.lastBaltopCalculateTime = System.currentTimeMillis();
        }
    }

    private void updateCachedBaltopSnapshot() {
        if (baltopEntryLimit > 0 && baltopSortedList.size() > baltopEntryLimit) {
            this.cachedBaltop = Collections.unmodifiableList(new ArrayList<>(baltopSortedList.subList(0, baltopEntryLimit)));
        } else {
            this.cachedBaltop = Collections.unmodifiableList(new ArrayList<>(baltopSortedList));
        }
    }

    /**
     * Smartly and incrementally updates baltop only for the user whose balance changed.
     * Takes O(log N) time instead of re-scanning or re-sorting all users.
     */
    public void updateBaltopForUser(UUID uuid, String name, BigDecimal oldBalance, BigDecimal newBalance) {
        if (uuid == null) return;
        if (oldBalance == null) oldBalance = BigDecimal.ZERO;
        if (newBalance == null) newBalance = BigDecimal.ZERO;

        UserAccount account = getAccount(uuid);
        if (account == null) return;

        synchronized (baltopLock) {
            // 1. Incrementally update total server money
            BigDecimal delta = newBalance.subtract(oldBalance, MATH_CONTEXT);
            cachedTotalServerMoney = cachedTotalServerMoney.add(delta, MATH_CONTEXT);
            if (cachedTotalServerMoney.signum() < 0) {
                cachedTotalServerMoney = BigDecimal.ZERO;
            }

            // 2. Remove old entry if present
            BalanceTopEntry oldEntry = baltopEntryMap.remove(uuid);
            if (oldEntry != null) {
                int oldIndex = Collections.binarySearch(baltopSortedList, oldEntry, BALTOP_COMPARATOR);
                if (oldIndex >= 0) {
                    baltopSortedList.remove(oldIndex);
                } else {
                    baltopSortedList.remove(oldEntry);
                }
            }

            // 3. Insert at new sorted position if player qualifies
            if (qualifiesForBaltop(account, newBalance)) {
                String resolvedName = name != null ? name : account.getLastAccountName();
                BalanceTopEntry newEntry = new BalanceTopEntry(uuid, resolvedName, newBalance);
                baltopEntryMap.put(uuid, newEntry);

                int newIndex = Collections.binarySearch(baltopSortedList, newEntry, BALTOP_COMPARATOR);
                if (newIndex < 0) {
                    newIndex = -newIndex - 1;
                }
                baltopSortedList.add(newIndex, newEntry);
            }

            // 4. Update the volatile read snapshot
            updateCachedBaltopSnapshot();
        }
    }

    public void removeBaltopForUser(UUID uuid) {
        if (uuid == null) return;
        synchronized (baltopLock) {
            BalanceTopEntry oldEntry = baltopEntryMap.remove(uuid);
            if (oldEntry != null) {
                int oldIndex = Collections.binarySearch(baltopSortedList, oldEntry, BALTOP_COMPARATOR);
                if (oldIndex >= 0) {
                    baltopSortedList.remove(oldIndex);
                } else {
                    baltopSortedList.remove(oldEntry);
                }
                cachedTotalServerMoney = cachedTotalServerMoney.subtract(oldEntry.getBalance(), MATH_CONTEXT);
                if (cachedTotalServerMoney.signum() < 0) {
                    cachedTotalServerMoney = BigDecimal.ZERO;
                }
                updateCachedBaltopSnapshot();
            }
        }
    }

    public void updateBaltopName(UUID uuid, String newName) {
        if (uuid == null || newName == null) return;
        synchronized (baltopLock) {
            BalanceTopEntry oldEntry = baltopEntryMap.get(uuid);
            if (oldEntry != null && !newName.equals(oldEntry.getName())) {
                int idx = Collections.binarySearch(baltopSortedList, oldEntry, BALTOP_COMPARATOR);
                if (idx >= 0) {
                    BalanceTopEntry updated = new BalanceTopEntry(uuid, newName, oldEntry.getBalance());
                    baltopSortedList.set(idx, updated);
                    baltopEntryMap.put(uuid, updated);
                    updateCachedBaltopSnapshot();
                }
            }
        }
    }

    public boolean qualifiesForBaltop(UserAccount account, BigDecimal balance) {
        if (account == null) return false;
        if (!baltopShowNpcs && account.isNpc()) return false;
        if (!baltopShowZero && balance.signum() <= 0) return false;
        if (balance.compareTo(baltopMinBalance) < 0) return false;

        Player player = Bukkit.getPlayer(account.getUuid());
        if (player != null && (player.hasPermission("nexuswarp.balancetop.exclude") ||
                player.hasPermission("essentials.balancetop.exclude"))) {
            return false;
        }
        return true;
    }

    public List<BalanceTopEntry> getBaltopOnline() {
        List<BalanceTopEntry> list = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("nexuswarp.balancetop.exclude") || p.hasPermission("essentials.balancetop.exclude")) {
                continue;
            }
            list.add(new BalanceTopEntry(p.getUniqueId(), p.getName(), getBalance(p.getUniqueId())));
        }
        list.sort(BALTOP_COMPARATOR);
        return list;
    }

    // -------------------------------------------------------------
    // Currency Formatting
    // -------------------------------------------------------------

    public String format(BigDecimal amount) {
        return EconomyUtil.formatCurrency(amount, currencySymbol, symbolBehind);
    }

    public String format(double amount) {
        return format(BigDecimal.valueOf(amount));
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public boolean isSymbolBehind() {
        return symbolBehind;
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

    public Collection<UserAccount> getAllAccounts() {
        return Collections.unmodifiableCollection(accounts.values());
    }
}
