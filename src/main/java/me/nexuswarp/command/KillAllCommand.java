package me.nexuswarp.command;

import me.nexuswarp.NexusWarpPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Ambient;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Boat;
import org.bukkit.entity.ComplexLivingEntity;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Flying;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.WaterMob;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class KillAllCommand implements CommandExecutor, TabCompleter {

    private final NexusWarpPlugin plugin;

    public KillAllCommand(NexusWarpPlugin plugin) {
        this.plugin = plugin;
    }

    private enum RemoveTarget {
        DROPS,
        MOBS,
        MONSTERS,
        ANIMALS,
        XP,
        ARROWS,
        BOATS,
        MINECARTS,
        PAINTINGS,
        ITEMFRAMES,
        ENDERCRYSTALS,
        ALL,
        NAMED,
        TAMED
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nexuswarp.killall") && !sender.hasPermission("essentials.killall")
                && !sender.hasPermission("nexuswarp.remove") && !sender.hasPermission("essentials.remove")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("usage_killall"));
            return true;
        }

        World world = null;
        if (sender instanceof Player player) {
            world = player.getWorld();
        }

        int radius = 0;

        if (args.length >= 2) {
            try {
                radius = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                World specifiedWorld = Bukkit.getWorld(args[1]);
                if (specifiedWorld != null) {
                    world = specifiedWorld;
                }
            }
        }

        if (args.length >= 3) {
            World specifiedWorld = Bukkit.getWorld(args[2]);
            if (specifiedWorld != null) {
                world = specifiedWorld;
            }
        }

        if (world == null) {
            if (!Bukkit.getWorlds().isEmpty()) {
                world = Bukkit.getWorlds().get(0);
            } else {
                sender.sendMessage(plugin.getLanguageManager().getMessage("world_not_found"));
                return true;
            }
        }

        String typeArg = args[0].toLowerCase(Locale.ROOT);
        Set<RemoveTarget> targets = new HashSet<>();
        EntityType specificType = null;

        for (String part : typeArg.split(",")) {
            part = part.trim();
            switch (part) {
                case "drops":
                case "drop":
                case "items":
                case "item":
                    targets.add(RemoveTarget.DROPS);
                    break;
                case "mobs":
                case "mob":
                    targets.add(RemoveTarget.MOBS);
                    break;
                case "monsters":
                case "monster":
                case "hostile":
                    targets.add(RemoveTarget.MONSTERS);
                    break;
                case "animals":
                case "animal":
                case "passive":
                    targets.add(RemoveTarget.ANIMALS);
                    break;
                case "xp":
                case "exp":
                case "orbs":
                    targets.add(RemoveTarget.XP);
                    break;
                case "arrows":
                case "arrow":
                case "projectiles":
                    targets.add(RemoveTarget.ARROWS);
                    break;
                case "boats":
                case "boat":
                    targets.add(RemoveTarget.BOATS);
                    break;
                case "minecarts":
                case "minecart":
                    targets.add(RemoveTarget.MINECARTS);
                    break;
                case "paintings":
                case "painting":
                    targets.add(RemoveTarget.PAINTINGS);
                    break;
                case "itemframes":
                case "itemframe":
                    targets.add(RemoveTarget.ITEMFRAMES);
                    break;
                case "endercrystals":
                case "crystals":
                    targets.add(RemoveTarget.ENDERCRYSTALS);
                    break;
                case "all":
                case "*":
                    targets.add(RemoveTarget.ALL);
                    break;
                case "named":
                    targets.add(RemoveTarget.NAMED);
                    break;
                case "tamed":
                    targets.add(RemoveTarget.TAMED);
                    break;
                default:
                    try {
                        specificType = EntityType.valueOf(part.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ignored) {}
                    break;
            }
        }

        if (targets.isEmpty() && specificType == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("invalid_entity_type", "type", args[0]));
            return true;
        }

        Player playerSender = (sender instanceof Player p) ? p : null;
        int radiusSquared = radius * radius;
        int removedCount = 0;

        for (Entity entity : world.getEntities()) {
            if (entity instanceof HumanEntity) {
                continue; // Never kill players
            }

            if (radius > 0 && playerSender != null) {
                if (!entity.getWorld().equals(playerSender.getWorld())
                        || playerSender.getLocation().distanceSquared(entity.getLocation()) > radiusSquared) {
                    continue;
                }
            }

            // Skip tamed animals unless specifically requested
            if (entity instanceof Tameable tameable && tameable.isTamed() && !targets.contains(RemoveTarget.TAMED) && !targets.contains(RemoveTarget.ALL)) {
                continue;
            }

            // Skip named living entities unless specifically requested
            if (entity instanceof LivingEntity && entity.getCustomName() != null && !targets.contains(RemoveTarget.NAMED) && !targets.contains(RemoveTarget.ALL)) {
                continue;
            }

            boolean shouldRemove = false;

            if (specificType != null && entity.getType() == specificType) {
                shouldRemove = true;
            } else if (targets.contains(RemoveTarget.ALL)) {
                shouldRemove = true;
            } else if (targets.contains(RemoveTarget.DROPS) && entity instanceof Item) {
                shouldRemove = true;
            } else if (targets.contains(RemoveTarget.XP) && entity instanceof ExperienceOrb) {
                shouldRemove = true;
            } else if (targets.contains(RemoveTarget.ARROWS) && entity instanceof Projectile) {
                shouldRemove = true;
            } else if (targets.contains(RemoveTarget.BOATS) && entity instanceof Boat) {
                shouldRemove = true;
            } else if (targets.contains(RemoveTarget.MINECARTS) && entity instanceof Minecart) {
                shouldRemove = true;
            } else if (targets.contains(RemoveTarget.PAINTINGS) && entity instanceof Painting) {
                shouldRemove = true;
            } else if (targets.contains(RemoveTarget.ITEMFRAMES) && entity instanceof ItemFrame) {
                shouldRemove = true;
            } else if (targets.contains(RemoveTarget.ENDERCRYSTALS) && entity instanceof EnderCrystal) {
                shouldRemove = true;
            } else if (targets.contains(RemoveTarget.MONSTERS) && (entity instanceof Monster || entity instanceof Slime || entity instanceof Flying || entity instanceof ComplexLivingEntity)) {
                shouldRemove = true;
            } else if (targets.contains(RemoveTarget.ANIMALS) && (entity instanceof Animals || entity instanceof WaterMob || entity instanceof Ambient)) {
                shouldRemove = true;
            } else if (targets.contains(RemoveTarget.MOBS) && (entity instanceof LivingEntity)) {
                shouldRemove = true;
            } else if (targets.contains(RemoveTarget.NAMED) && entity instanceof LivingEntity && entity.getCustomName() != null) {
                shouldRemove = true;
            } else if (targets.contains(RemoveTarget.TAMED) && entity instanceof Tameable tameable && tameable.isTamed()) {
                shouldRemove = true;
            }

            if (shouldRemove) {
                entity.remove();
                removedCount++;
            }
        }

        sender.sendMessage(plugin.getLanguageManager().getMessage("killall_success",
                "count", String.valueOf(removedCount),
                "type", args[0],
                "world", world.getName()));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("nexuswarp.killall") && !sender.hasPermission("essentials.killall")
                && !sender.hasPermission("nexuswarp.remove") && !sender.hasPermission("essentials.remove")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> types = Arrays.asList(
                    "drops", "mobs", "monsters", "animals", "xp", "arrows", "boats",
                    "minecarts", "paintings", "itemframes", "all", "named", "tamed"
            );
            List<String> list = new ArrayList<>();
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String t : types) {
                if (t.startsWith(prefix)) {
                    list.add(t);
                }
            }
            return list;
        }

        if (args.length == 2) {
            List<String> list = new ArrayList<>(Arrays.asList("10", "25", "50", "100", "500"));
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (World w : Bukkit.getWorlds()) {
                if (w.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    list.add(w.getName());
                }
            }
            return list;
        }

        return Collections.emptyList();
    }
}
