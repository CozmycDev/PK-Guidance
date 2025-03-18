package net.doodcraft.cozmyc.guidance;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.event.*;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockReceiveGameEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.GenericGameEvent;

import java.util.HashSet;
import java.util.UUID;


public class GuidanceListener implements Listener {

    private static final HashSet<UUID> recentlyDropped = new HashSet<>();
    private final HashSet<UUID> adjustingFollowDistance = new HashSet<>();

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return;

        if (event.isSneaking()) {
            if (bPlayer.getBoundAbilityName().equalsIgnoreCase("Guidance") && Guidance.getAbility(player, Guidance.class) != null && Guidance.getAbility(player, Guidance.class).allowChangeFollowDistance) {
                adjustingFollowDistance.add(player.getUniqueId());
            } else {
                adjustingFollowDistance.remove(player.getUniqueId());
            }
        } else {
            adjustingFollowDistance.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return;

        if (adjustingFollowDistance.contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.getInventory().setHeldItemSlot(event.getPreviousSlot());
            Guidance guidance = Guidance.getAbility(player, Guidance.class);
            if (guidance != null) {
                guidance.startInspectCooldown();
                int previousSlot = event.getPreviousSlot();
                int newSlot = event.getNewSlot();

                if (newSlot != previousSlot) {
                    int currentDistance = Guidance.trackedFollowDistance.getOrDefault(player.getUniqueId(), guidance.followDistance);
                    int minDistance = guidance.minFollowDistance;
                    int maxDistance = guidance.maxFollowDistance;
                    int change = calculateScrollDirection(previousSlot, newSlot);

                    int newDistance = currentDistance - change;

                    if (newDistance >= minDistance && newDistance <= maxDistance) {
                        Guidance.trackedFollowDistance.put(player.getUniqueId(), newDistance);
                        Bukkit.getScheduler().runTaskLater(ProjectKorra.plugin, () ->
                                Guidance.sendActionBar(player, Guidance.addColor("#cab0ffFollow distance is now: " + newDistance)), 2L);
                        player.playSound(player.getLocation(), Sound.ENTITY_SNIFFER_DROP_SEED, 0.7f, 2.0f);
                    } else if (newDistance < minDistance) {
                        Guidance.trackedFollowDistance.put(player.getUniqueId(), minDistance);
                        Bukkit.getScheduler().runTaskLater(ProjectKorra.plugin, () ->
                                Guidance.sendActionBar(player, Guidance.addColor("#cab0ffFollow distance is now: " + minDistance)), 2L);
                    } else {
                        Guidance.trackedFollowDistance.put(player.getUniqueId(), maxDistance);
                        Bukkit.getScheduler().runTaskLater(ProjectKorra.plugin, () ->
                                Guidance.sendActionBar(player, Guidance.addColor("#cab0ffFollow distance is now: " + maxDistance)), 2L);
                    }
                }
            }
        }
    }

    private int calculateScrollDirection(int previousSlot, int newSlot) {
        if (newSlot > previousSlot) {
            if (newSlot - previousSlot > 4) {
                return -1;
            } else {
                return 1;
            }
        } else if (newSlot < previousSlot) {
            if (previousSlot - newSlot > 4) {
                return 1;
            } else {
                return -1;
            }
        } else {
            return 0;
        }
    }

    @EventHandler
    public void onEvent(GenericGameEvent event) {
        if (event.getEntity() == null) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (event.getEntity() instanceof Warden warden) {
            if (warden.getEntityAngryAt() == null) return;
            LivingEntity entity = warden.getEntityAngryAt();
            if (Guidance.getSpirits().containsValue(entity)) {
                warden.setAnger(entity, 0);
            }
        }
        if (event.getLocation().getBlock().getType() == Material.SCULK_CATALYST || event.getLocation().getBlock().getType() == Material.CALIBRATED_SCULK_SENSOR) {
            if (Guidance.getSpirits().containsValue((LivingEntity) event.getEntity())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onSculkEvent(BlockReceiveGameEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (Guidance.getSpirits().containsValue((LivingEntity) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        LivingEntity entity = event.getEntity() instanceof LivingEntity ? (LivingEntity) event.getEntity() : null;
        LivingEntity target = event.getTarget() instanceof LivingEntity ? (LivingEntity) event.getTarget() : null;

        if (entity == null && target == null) return;

        if (target != null && Guidance.getSpirits().containsValue(target)) {
            event.setCancelled(true);
        }

        if (entity != null && Guidance.getSpirits().containsValue(entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onApply(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (Guidance.getSpirits().containsValue((LivingEntity) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (Guidance.getSpirits().containsValue(event.getEntity())) {
            for (Ability ability : ElementalAbility.getAbilitiesByInstances()) {
                if (!(ability instanceof Guidance guidance)) continue;
                if (guidance.getEntity().getUniqueId().equals(event.getEntity().getUniqueId())) {
                    guidance.remove();
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            if (Guidance.getSpirits().containsValue((LivingEntity) event.getEntity())) {
                event.setCancelled(true);
            }
        }
        if (event.getTarget() != null && Guidance.getSpirits().containsValue(event.getTarget())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (Guidance.getSpirits().containsValue((LivingEntity) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof LivingEntity) {
            if (Guidance.getSpirits().containsValue((LivingEntity) event.getDamager())) {
                event.setCancelled(true);
            }
        }
        if (event.getEntity() instanceof LivingEntity) {
            if (Guidance.getSpirits().containsValue((LivingEntity) event.getEntity())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDamage(AbilityDamageEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (Guidance.getSpirits().containsValue((LivingEntity) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(AbilityVelocityAffectEntityEvent event) {
        if (!(event.getAffected() instanceof LivingEntity)) return;
        if (Guidance.getSpirits().containsValue((LivingEntity) event.getAffected())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCombust(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (Guidance.getSpirits().containsValue((LivingEntity) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void PlayerDropItemEvent(PlayerDropItemEvent event) {
        recentlyDropped.add(event.getPlayer().getUniqueId());
        Bukkit.getScheduler().runTaskLater(ProjectKorra.plugin, () -> recentlyDropped.remove(event.getPlayer().getUniqueId()), 2L);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (recentlyDropped.contains(player.getUniqueId())) return;
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

        if (bPlayer == null || !bPlayer.getBoundAbilityName().equalsIgnoreCase("Guidance")) return;

        Guidance.trackedTypes.remove(player.getUniqueId());
        Guidance.trackedNames.remove(player.getUniqueId());

        boolean foundGuidance = false;

        for (Ability ability : ElementalAbility.getAbilitiesByInstances()) {
            if (ability.getPlayer().getUniqueId().equals(player.getUniqueId()) && ability.getName().equalsIgnoreCase("Guidance")) {
                foundGuidance = true;
                Guidance guidance = (Guidance) ability;
                if (guidance.getState() == AbilityState.ACTIVE) {
                    guidance.setState(AbilityState.INACTIVE);
                    Guidance.sendActionBar(player, ConfigManager.defaultConfig.get().getString("ExtraAbilities.Cozmyc.Guidance.Language.ToggledOff"));
                } else {
                    guidance.setState(AbilityState.ACTIVE);
                    Guidance.sendActionBar(player, ConfigManager.defaultConfig.get().getString("ExtraAbilities.Cozmyc.Guidance.Language.ToggledOn"));
                }
                Guidance.trackedStates.put(player.getUniqueId(), guidance.getState());
                break;
            }
        }

        if (!foundGuidance) {
            new Guidance(player);
            Guidance.sendActionBar(player, ConfigManager.defaultConfig.get().getString("ExtraAbilities.Cozmyc.Guidance.Language.ToggledOn"));
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof LivingEntity clickedEntity)) return;

        Player player = event.getPlayer();

        if (!Guidance.getSpirits().containsValue(clickedEntity)) return;

        UUID playerId = player.getUniqueId();
        LivingEntity spirit = Guidance.getSpirits().get(playerId);

        Guidance.getInstanceBySpirit(spirit).cureBlindness();

        if (spirit != null && spirit.getUniqueId().equals(spirit.getUniqueId())) {

            spirit.getWorld().spawnParticle(
                    Particle.HEART,
                    spirit.getLocation(),
                    ConfigManager.defaultConfig.get().getInt("ExtraAbilities.Cozmyc.Guidance.Boon.HeartParticles"),
                    0.5, 0.5, 0.5,
                    0.02,
                    null,
                    true
            );

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onReload(BendingReloadEvent event) {
        if (Guidance.guidanceTaskId != -1) {
            Bukkit.getScheduler().cancelTask(Guidance.guidanceTaskId);
        }
    }
}
