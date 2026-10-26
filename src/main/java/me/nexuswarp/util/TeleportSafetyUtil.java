package me.nexuswarp.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class TeleportSafetyUtil {

    private TeleportSafetyUtil() {}

    public static boolean isSafe(Location location, boolean preventLava, boolean preventVoid, boolean preventSuffocation) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        World world = location.getWorld();
        int blockX = location.getBlockX();
        int blockY = location.getBlockY();
        int blockZ = location.getBlockZ();

        if (preventVoid && blockY <= world.getMinHeight()) {
            return false;
        }

        Block feetBlock = world.getBlockAt(blockX, blockY, blockZ);
        Block headBlock = world.getBlockAt(blockX, blockY + 1, blockZ);
        Block groundBlock = world.getBlockAt(blockX, blockY - 1, blockZ);

        if (preventLava) {
            if (isLava(feetBlock) || isLava(headBlock) || isLava(groundBlock)) {
                return false;
            }
        }

        if (preventVoid) {
            // Check if ground is air/dangerous and there is nothing solid below
            if (!groundBlock.getType().isSolid() && groundBlock.getType() != Material.WATER) {
                boolean hasSolidBelow = false;
                for (int y = blockY - 1; y >= world.getMinHeight(); y--) {
                    Material mat = world.getBlockAt(blockX, y, blockZ).getType();
                    if (mat.isSolid() || mat == Material.WATER) {
                        hasSolidBelow = true;
                        break;
                    }
                }
                if (!hasSolidBelow) {
                    return false;
                }
            }
        }

        if (preventSuffocation) {
            if (isSuffocating(feetBlock) || isSuffocating(headBlock)) {
                return false;
            }
        }

        return true;
    }

    public static Location getSafeLocation(Location targetLocation, boolean preventLava, boolean preventVoid, boolean preventSuffocation) {
        if (targetLocation == null || targetLocation.getWorld() == null) {
            return null;
        }

        if (isSafe(targetLocation, preventLava, preventVoid, preventSuffocation)) {
            return targetLocation;
        }

        World world = targetLocation.getWorld();
        int originX = targetLocation.getBlockX();
        int originY = targetLocation.getBlockY();
        int originZ = targetLocation.getBlockZ();

        // Check 1 block radius around target for safe spot
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    Location candidate = new Location(
                            world,
                            originX + dx + 0.5,
                            originY + dy,
                            originZ + dz + 0.5,
                            targetLocation.getYaw(),
                            targetLocation.getPitch()
                    );

                    if (isSafe(candidate, preventLava, preventVoid, preventSuffocation)) {
                        return candidate;
                    }
                }
            }
        }

        return null;
    }

    private static boolean isLava(Block block) {
        Material mat = block.getType();
        return mat == Material.LAVA || mat.name().contains("LAVA");
    }

    private static boolean isSuffocating(Block block) {
        Material mat = block.getType();
        if (!mat.isOccluding()) {
            return false;
        }
        return mat.isSolid();
    }
}
