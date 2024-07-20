package net.doodcraft.cozmyc.guidance;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

import java.util.UUID;

public record LightInfo(UUID getOwnerUUID, Location getLocation, int getLightLevel, long getDelay, int getRange, BlockData getBlockData, boolean isEphemeral) {}
