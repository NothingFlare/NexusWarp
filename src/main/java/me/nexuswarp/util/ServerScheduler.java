package me.nexuswarp.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class ServerScheduler {

    public interface TaskHandle {
        void cancel();
    }

    private static final boolean IS_FOLIA;

    private static Object asyncSchedulerObj;
    private static MethodHandle asyncRunNowHandle;
    private static MethodHandle asyncRunAtFixedRateHandle;

    private static Object globalRegionSchedulerObj;
    private static MethodHandle globalRunHandle;

    private static MethodHandle getEntitySchedulerHandle;
    private static MethodHandle entityRunHandle;
    private static MethodHandle entityRunDelayedHandle;

    private static MethodHandle scheduledTaskCancelHandle;

    private static MethodHandle teleportAsyncHandle;

    static {
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException ignored) {
            folia = false;
        }
        IS_FOLIA = folia;

        MethodHandles.Lookup lookup = MethodHandles.publicLookup();

        if (IS_FOLIA) {
            try {
                // Bukkit.getAsyncScheduler()
                Method getAsync = Bukkit.class.getMethod("getAsyncScheduler");
                asyncSchedulerObj = getAsync.invoke(null);
                Class<?> asyncClass = asyncSchedulerObj.getClass();

                // asyncScheduler.runNow(Plugin, Consumer<ScheduledTask>)
                Method runNowMethod = asyncClass.getMethod("runNow", Plugin.class, Consumer.class);
                asyncRunNowHandle = lookup.unreflect(runNowMethod);

                // asyncScheduler.runAtFixedRate(Plugin, Consumer<ScheduledTask>, long, long, TimeUnit)
                Method runAtFixedRateMethod = asyncClass.getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class);
                asyncRunAtFixedRateHandle = lookup.unreflect(runAtFixedRateMethod);

                // Bukkit.getGlobalRegionScheduler()
                Method getGlobal = Bukkit.class.getMethod("getGlobalRegionScheduler");
                globalRegionSchedulerObj = getGlobal.invoke(null);
                Class<?> globalClass = globalRegionSchedulerObj.getClass();

                // globalRegionScheduler.run(Plugin, Consumer<ScheduledTask>)
                Method globalRunMethod = globalClass.getMethod("run", Plugin.class, Consumer.class);
                globalRunHandle = lookup.unreflect(globalRunMethod);

                // Player.getScheduler() -> EntityScheduler
                Method getEntitySched = Player.class.getMethod("getScheduler");
                getEntitySchedulerHandle = lookup.unreflect(getEntitySched);

                Class<?> entitySchedClass = getEntitySched.getReturnType();
                // entityScheduler.run(Plugin, Consumer<ScheduledTask>, Runnable)
                Method entityRunMethod = entitySchedClass.getMethod("run", Plugin.class, Consumer.class, Runnable.class);
                entityRunHandle = lookup.unreflect(entityRunMethod);

                // entityScheduler.runDelayed(Plugin, Consumer<ScheduledTask>, Runnable, long)
                Method entityRunDelayedMethod = entitySchedClass.getMethod("runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class);
                entityRunDelayedHandle = lookup.unreflect(entityRunDelayedMethod);

                // ScheduledTask.cancel()
                Class<?> scheduledTaskClass = runAtFixedRateMethod.getReturnType();
                Method cancelMethod = scheduledTaskClass.getMethod("cancel");
                scheduledTaskCancelHandle = lookup.unreflect(cancelMethod);

            } catch (Throwable t) {
                Bukkit.getLogger().warning("[NexusWarp] Failed to initialize Folia scheduler handles, falling back to Bukkit scheduler: " + t.getMessage());
            }
        }

        // Check for teleportAsync on Player
        try {
            Method tpAsyncMethod = Player.class.getMethod("teleportAsync", Location.class);
            teleportAsyncHandle = lookup.unreflect(tpAsyncMethod);
        } catch (Throwable ignored) {
            teleportAsyncHandle = null;
        }
    }

    private ServerScheduler() {}

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    public static void runAsync(Plugin plugin, Runnable runnable) {
        if (IS_FOLIA && asyncRunNowHandle != null) {
            try {
                Consumer<Object> consumer = task -> runnable.run();
                asyncRunNowHandle.invoke(asyncSchedulerObj, plugin, consumer);
                return;
            } catch (Throwable t) {
                Bukkit.getLogger().severe("[NexusWarp] Error invoking Folia async runNow: " + t.getMessage());
            }
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public static TaskHandle runTimerAsync(Plugin plugin, Runnable runnable, long initialDelaySeconds, long periodSeconds) {
        if (IS_FOLIA && asyncRunAtFixedRateHandle != null) {
            try {
                Consumer<Object> consumer = task -> runnable.run();
                Object scheduledTask = asyncRunAtFixedRateHandle.invoke(asyncSchedulerObj, plugin, consumer, initialDelaySeconds, periodSeconds, TimeUnit.SECONDS);
                return () -> {
                    if (scheduledTask != null && scheduledTaskCancelHandle != null) {
                        try {
                            scheduledTaskCancelHandle.invoke(scheduledTask);
                        } catch (Throwable ignored) {}
                    }
                };
            } catch (Throwable t) {
                Bukkit.getLogger().severe("[NexusWarp] Error invoking Folia runAtFixedRate: " + t.getMessage());
            }
        }
        BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, initialDelaySeconds * 20L, periodSeconds * 20L);
        return task::cancel;
    }

    public static void runGlobal(Plugin plugin, Runnable runnable) {
        if (IS_FOLIA && globalRunHandle != null) {
            try {
                Consumer<Object> consumer = task -> runnable.run();
                globalRunHandle.invoke(globalRegionSchedulerObj, plugin, consumer);
                return;
            } catch (Throwable t) {
                Bukkit.getLogger().severe("[NexusWarp] Error invoking Folia global run: " + t.getMessage());
            }
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public static void runOnPlayer(Plugin plugin, Player player, Runnable runnable) {
        if (player == null || !player.isOnline()) return;

        if (IS_FOLIA && getEntitySchedulerHandle != null && entityRunHandle != null) {
            try {
                Object entitySched = getEntitySchedulerHandle.invoke(player);
                if (entitySched != null) {
                    Consumer<Object> consumer = task -> runnable.run();
                    entityRunHandle.invoke(entitySched, plugin, consumer, (Runnable) null);
                    return;
                }
            } catch (Throwable t) {
                Bukkit.getLogger().severe("[NexusWarp] Error invoking Folia entity run: " + t.getMessage());
            }
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public static TaskHandle runOnPlayerDelayed(Plugin plugin, Player player, Runnable runnable, long delayTicks) {
        if (player == null || !player.isOnline()) return () -> {};

        if (IS_FOLIA && getEntitySchedulerHandle != null && entityRunDelayedHandle != null) {
            try {
                Object entitySched = getEntitySchedulerHandle.invoke(player);
                if (entitySched != null) {
                    Consumer<Object> consumer = task -> runnable.run();
                    Object scheduledTask = entityRunDelayedHandle.invoke(entitySched, plugin, consumer, (Runnable) null, delayTicks);
                    return () -> {
                        if (scheduledTask != null && scheduledTaskCancelHandle != null) {
                            try {
                                scheduledTaskCancelHandle.invoke(scheduledTask);
                            } catch (Throwable ignored) {}
                        }
                    };
                }
            } catch (Throwable t) {
                Bukkit.getLogger().severe("[NexusWarp] Error invoking Folia entity runDelayed: " + t.getMessage());
            }
        }
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        return task::cancel;
    }

    public static CompletableFuture<Boolean> teleportAsync(Player player, Location targetLocation) {
        if (player == null || !player.isOnline() || targetLocation == null || targetLocation.getWorld() == null) {
            return CompletableFuture.completedFuture(false);
        }

        if (teleportAsyncHandle != null) {
            try {
                @SuppressWarnings("unchecked")
                CompletableFuture<Boolean> future = (CompletableFuture<Boolean>) teleportAsyncHandle.invoke(player, targetLocation);
                return future;
            } catch (Throwable t) {
                Bukkit.getLogger().warning("[NexusWarp] teleportAsync invoke failed, falling back: " + t.getMessage());
            }
        }

        CompletableFuture<Boolean> fallbackFuture = new CompletableFuture<>();
        runOnPlayer(player.getServer().getPluginManager().getPlugin("NexusWarp"), player, () -> {
            try {
                boolean result = player.teleport(targetLocation);
                fallbackFuture.complete(result);
            } catch (Throwable t) {
                fallbackFuture.complete(false);
            }
        });
        return fallbackFuture;
    }
}
