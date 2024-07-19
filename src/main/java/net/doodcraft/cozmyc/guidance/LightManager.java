package net.doodcraft.cozmyc.guidance;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LightManager {
    private static final LightManager INSTANCE = new LightManager();

    private final ConcurrentMap<Location, Integer> lightCounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<Location, BlockData> originalData = new ConcurrentHashMap<>();

    private LightManager() {
    }

    /**
     * Returns the singleton instance of the LightManager class.
     *
     * @return the singleton instance of LightManager
     */
    public static LightManager getInstance() {
        return INSTANCE;
    }

    /**
     * Adds a light to the specified location if the block type and brightness conditions are met.
     * The location should be air or water and the brightness should be less than the locations current brightness.
     *
     * @param plugin     the plugin instance handling tasks
     * @param location   the location to add the light
     * @param brightness the brightness level of the light, between 0-15
     * @param delay      the delay before removing the light, in ticks
     * @param range      the range within which players can see the light
     */
    public void addLight(Plugin plugin, Location location, int brightness, long delay, int range) {
        Block block = location.getBlock();
        if (block.getLightLevel() >= brightness) return;

        final Material type = block.getType();
        if (type != Material.WATER && type != Material.AIR) return;

        lightCounts.merge(location, 1, Integer::sum);

        if (!originalData.containsKey(location)) {
            originalData.put(location, block.getBlockData());
        }

        BlockData lightData = Material.LIGHT.createBlockData();

        if (type == Material.WATER) ((Waterlogged) lightData).setWaterlogged(true);
        ((Levelled) lightData).setLevel(brightness);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals(location.getWorld())) continue;
            if (player.getLocation().distance(location) > range) continue;
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> player.sendBlockChange(location, lightData));
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> removeLight(plugin, location), delay);
    }

    /**
     * Removes a light at the specified location, reverting the block to its most recent state.
     *
     * @param plugin   the plugin instance handling tasks
     * @param location the location of the light to remove
     */
    public void removeLight(Plugin plugin, Location location) {
        int count = lightCounts.merge(location, -1, Integer::sum);
        if (count <= 0) {
            BlockData data = originalData.remove(location);
            if (data != null) {
                Block block = location.getBlock();
                BlockData currentData = block.getBlockData();
                BlockData revertData = (block.getType() == data.getMaterial()) ? data : currentData;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.getWorld().equals(location.getWorld())) continue;
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> player.sendBlockChange(location, revertData));
                }
            }
            lightCounts.remove(location);
        }
    }

    /**
     * Removes all the lights immediately.
     */
    public void removeAllLights() {
        for (Location location : lightCounts.keySet()) {
            BlockData data = originalData.get(location);
            if (data != null) {
                Block block = location.getBlock();
                BlockData revertData = (block.getType() == data.getMaterial()) ? data : block.getBlockData();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.getWorld().equals(location.getWorld())) continue;
                    player.sendBlockChange(location, revertData);
                }
            }
        }
        lightCounts.clear();
        originalData.clear();
    }
}