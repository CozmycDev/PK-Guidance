package net.doodcraft.cozmyc.guidance;

import com.projectkorra.projectkorra.ProjectKorra;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LightManager {
    private static final LightManager INSTANCE = new LightManager();

    private final ConcurrentMap<LightInfo, Long> lightTimestamps = new ConcurrentHashMap<>();
    private final ConcurrentMap<Location, LightInfo> lights = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private LightManager() {
        Bukkit.getScheduler().runTaskTimer(ProjectKorra.plugin, this::removeExpiredLights, 1L, 1L);
    }

    /**
     * Returns the instance of the LightManager class.
     *
     * @return the singleton instance of the LightManager
     */
    public static LightManager getInstance() {
        return INSTANCE;
    }

    /**
     * Rounds the coordinates of the given Location to the nearest integer values and sets pitch, yaw, and direction to zero.
     *
     * @param  location  the Location to be rounded
     * @return          the rounded Location
     */
    private Location roundLocation(Location location) {
        Location loc = location.clone();
        loc.setX(Math.floor(location.getX()));
        loc.setY(Math.floor(location.getY()));
        loc.setZ(Math.floor(location.getZ()));
        loc.setPitch(0);
        loc.setYaw(0);
        loc.setDirection(new Vector(0,0,0));
        return loc;
    }

    /**
     * Adds a light to the specified location with the given location, brightness, range, and delay.
     * Calling this with the same location before the delay expires resets the locations scheduled light removal.
     *
     * @param  location    the location where the light will be added
     * @param  brightness  the brightness level of the light, 0-15
     * @param  range       the range within which the light is visible
     * @param  delay       the delay in ticks before the light is removed
     * @param  isEphemeral whether the light is visible to everyone or just the player, defaults to false without uuid
     * @param  uuid       the uuid of the player instantiating the light
     */
    public void addLight(Location location, int brightness, int range, long delay, @Nullable Boolean isEphemeral, @Nullable UUID uuid) {
        Location roundedLocation = roundLocation(location);
        lock.writeLock().lock();
        try {
            Block block = roundedLocation.getBlock();
            if (block.getLightLevel() >= brightness) return;

            final Material type = block.getType();
            if (type != Material.WATER && type != Material.AIR) return;

            BlockData lightData = Material.LIGHT.createBlockData();
            if (type == Material.WATER) ((Waterlogged) lightData).setWaterlogged(true);
            ((Levelled) lightData).setLevel(brightness);

            if (isEphemeral == null || uuid == null) isEphemeral = false;

            LightInfo light = new LightInfo(uuid, roundedLocation, brightness, delay, range, lightData, isEphemeral);

            lightTimestamps.put(light, System.currentTimeMillis());
            lights.put(roundedLocation, light);

            if (isEphemeral) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || player.isDead() || !player.isOnline()) return;
                if (!player.getWorld().equals(roundedLocation.getWorld())) return;
                if (player.getLocation().distance(roundedLocation) > range) return;
                Bukkit.getScheduler().runTaskLaterAsynchronously(ProjectKorra.plugin, () -> player.sendBlockChange(roundedLocation, lightData), 0L);
            } else {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player == null || player.isDead() || !player.isOnline()) continue;
                    if (!player.getWorld().equals(roundedLocation.getWorld())) continue;
                    if (player.getLocation().distance(roundedLocation) > range) continue;
                    Bukkit.getScheduler().runTaskLaterAsynchronously(ProjectKorra.plugin, () -> player.sendBlockChange(roundedLocation, lightData), 0L);
                }
            }

        } finally {
            lock.writeLock().unlock();
        }
    }

    private void removeExpiredLights() {
        lock.writeLock().lock();
        try {
            long currentTime = System.currentTimeMillis();
            for (LightInfo light : lights.values()) {
                if (currentTime - lightTimestamps.get(light) >= light.getDelay()*50) {
                    removeLight(light.getLocation());
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void removeLight(Location location) {
        LightInfo light = lights.remove(roundLocation(location));
        if (light == null) return;

        lock.writeLock().lock();
        try {
            Block block = light.getLocation().getBlock();
            BlockData currentBlockData = block.getBlockData();
            BlockData revertData = (block.getType() == light.getBlockData().getMaterial()) ? light.getBlockData() : currentBlockData;

            if (light.isEphemeral()) {
                Player player = Bukkit.getPlayer(light.getOwnerUUID());
                if (player == null || player.isDead() || !player.isOnline()) return;
                if (!player.getWorld().equals(light.getLocation().getWorld())) return;
                Bukkit.getScheduler().runTaskLaterAsynchronously(ProjectKorra.plugin, () -> player.sendBlockChange(light.getLocation(), revertData), 0L);
            } else {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player == null || player.isDead() || !player.isOnline()) continue;
                    if (!player.getWorld().equals(light.getLocation().getWorld())) continue;
                    Bukkit.getScheduler().runTaskLaterAsynchronously(ProjectKorra.plugin, () -> player.sendBlockChange(light.getLocation(), revertData), 0L);
                }
            }

            lightTimestamps.remove(light);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes all lights from the environment immediately, reverting blocks to their most current state.
     */
    public void removeAllLights() {
        lock.writeLock().lock();
        try {
            for (LightInfo light : lights.values()) {
                BlockData data = light.getBlockData();
                Block block = light.getLocation().getBlock();
                BlockData revertData = (block.getType() == data.getMaterial()) ? data : block.getBlockData();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.getWorld().equals(light.getLocation().getWorld())) continue;
                    player.sendBlockChange(light.getLocation(), revertData);
                }
            }
            lightTimestamps.clear();
            lights.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Adds lights to the specified list of locations with the given brightness, range, delay, and ephemeral status.
     *
     * @param  locations    a list of locations where the lights will be added
     * @param  brightness   the brightness level of the lights, 0-15
     * @param  range        the range within which the lights are visible
     * @param  delay        the delay in ticks before the lights are removed
     * @param  isEphemeral  whether the lights are visible to everyone or just the player, defaults to false without uuid
     * @param  uuid         the uuid of the player instantiating the lights
     */
    public void addLights(List<Location> locations, int brightness, int range, long delay, @Nullable Boolean isEphemeral, @Nullable UUID uuid) {
        lock.writeLock().lock();
        try {
            for (Location location : locations) {
                addLight(location, brightness, range, delay, isEphemeral, uuid);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
