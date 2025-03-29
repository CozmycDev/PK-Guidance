package net.doodcraft.cozmyc.guidance;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.event.AbilityDamageEntityEvent;
import com.projectkorra.projectkorra.event.AbilityVelocityAffectEntityEvent;
import com.projectkorra.projectkorra.event.BendingReloadEvent;
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
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.world.GenericGameEvent;

import java.util.HashSet;
import java.util.UUID;


public class GuidanceListener implements Listener {

    private static final HashSet<UUID> recentlyDropped = new HashSet<>();
    private static final HashSet<UUID> adjustingFollowDistance = new HashSet<>();

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) return;

        if (event.isSneaking()) {
            if (!bPlayer.getBoundAbilityName().equalsIgnoreCase("Guidance")) return;
            Guidance guidance = Guidance.getAbility(player, Guidance.class);
            if (guidance != null && guidance.canAllowChangeFollowDistance()) {
                if (Guidance.getCurrentSpirits().containsValue(guidance.getEntity())) {
                    adjustingFollowDistance.add(player.getUniqueId());
                }
            } else {
                adjustingFollowDistance.remove(player.getUniqueId());
            }
        } else {
            if (bPlayer.getBoundAbilityName().equalsIgnoreCase("Guidance")) {
                Guidance guidance = Guidance.getAbility(player, Guidance.class);
                if (guidance != null) guidance.getEntity().teleport(player.getLocation());
            }
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
                    int currentDistance = Guidance.trackedFollowDistance.getOrDefault(player.getUniqueId(), guidance.getFollowDistance());
                    int minDistance = guidance.getMinFollowDistance();
                    int maxDistance = guidance.getMaxFollowDistance();

                    int change = calculateScrollDirection(previousSlot, newSlot);

                    int newDistance = currentDistance - change;

                    if (newDistance >= minDistance && newDistance <= maxDistance) {
                        Guidance.trackedFollowDistance.put(player.getUniqueId(), newDistance);
                        Bukkit.getScheduler().runTaskLater(ProjectKorra.plugin, () ->
                                StaticMethods.sendActionBar(player, StaticMethods.addColor("#cab0ffFollow distance is now: " + newDistance)), 2L);
                        player.playSound(player.getLocation(), Sound.ENTITY_SNIFFER_DROP_SEED, 0.7f, 2.0f);
                    } else if (newDistance < minDistance) {
                        Guidance.trackedFollowDistance.put(player.getUniqueId(), minDistance);
                        Bukkit.getScheduler().runTaskLater(ProjectKorra.plugin, () ->
                                StaticMethods.sendActionBar(player, StaticMethods.addColor("#cab0ffFollow distance is now: " + minDistance)), 2L);
                    } else {
                        Guidance.trackedFollowDistance.put(player.getUniqueId(), maxDistance);
                        Bukkit.getScheduler().runTaskLater(ProjectKorra.plugin, () ->
                                StaticMethods.sendActionBar(player, StaticMethods.addColor("#cab0ffFollow distance is now: " + maxDistance)), 2L);
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
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;
        if (event.getEntity() instanceof Warden warden) {
            if (warden.getEntityAngryAt() == null) return;
            LivingEntity entity = warden.getEntityAngryAt();
            if (Guidance.getCurrentSpirits().containsValue(entity)) {
                warden.setAnger(entity, 0);
            }
        }
        if ((event.getLocation().getBlock().getType() == Material.SCULK_CATALYST || event.getLocation().getBlock().getType() == Material.CALIBRATED_SCULK_SENSOR) && Guidance.getCurrentSpirits().containsValue(livingEntity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSculkEvent(BlockReceiveGameEvent event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;
        if (Guidance.getCurrentSpirits().containsValue(livingEntity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        LivingEntity entity = event.getEntity() instanceof LivingEntity e ? e : null;
        LivingEntity target = event.getTarget() instanceof LivingEntity t ? t: null;

        if (entity == null && target == null) return;

        if (target != null && Guidance.getCurrentSpirits().containsValue(target)) {
            event.setCancelled(true);
        }

        if (entity != null && Guidance.getCurrentSpirits().containsValue(entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onApply(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;
        if (Guidance.getCurrentSpirits().containsValue(livingEntity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (Guidance.getCurrentSpirits().containsValue(event.getEntity())) {
            Guidance targetGuidance = null;
            for (Ability ability : ElementalAbility.getAbilitiesByInstances()) {
                if (ability instanceof Guidance guidance &&
                        guidance.getEntity() != null &&
                        guidance.getEntity().getUniqueId().equals(event.getEntity().getUniqueId())) {
                    targetGuidance = guidance;
                }
            }
            if (targetGuidance != null) {
                targetGuidance.remove();
            }
        }
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (event.getEntity() instanceof LivingEntity entity && Guidance.getCurrentSpirits().containsValue(entity)) {
                event.setCancelled(true);
        }
        if (event.getTarget() != null && Guidance.getCurrentSpirits().containsValue(event.getTarget())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (Guidance.getCurrentSpirits().containsValue(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof LivingEntity entity && Guidance.getCurrentSpirits().containsValue(entity)) {
            event.setCancelled(true);
        }
        if (event.getEntity() instanceof LivingEntity entity && Guidance.getCurrentSpirits().containsValue(entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(AbilityDamageEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;
        if (Guidance.getCurrentSpirits().containsValue(livingEntity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(AbilityVelocityAffectEntityEvent event) {
        if (!(event.getAffected() instanceof LivingEntity livingEntity)) return;
        if (Guidance.getCurrentSpirits().containsValue(livingEntity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCombust(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;
        if (Guidance.getCurrentSpirits().containsValue(livingEntity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
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

        for (CoreAbility ability : ElementalAbility.getAbilitiesByInstances()) {
            if (ability.getPlayer().getUniqueId().equals(player.getUniqueId()) && ability.getName().equalsIgnoreCase("Guidance")) {
                foundGuidance = true;
                Guidance guidance = (Guidance) ability;
                if (guidance.getState() == AbilityState.ACTIVE) {
                    guidance.setState(AbilityState.INACTIVE);
                    StaticMethods.sendActionBar(player, ConfigManager.defaultConfig.get().getString("ExtraAbilities.Cozmyc.Guidance.Language.ToggledOff"));
                } else {
                    guidance.setState(AbilityState.ACTIVE);
                    StaticMethods.sendActionBar(player, ConfigManager.defaultConfig.get().getString("ExtraAbilities.Cozmyc.Guidance.Language.ToggledOn"));
                }
                Guidance.trackedStates.put(player.getUniqueId(), guidance.getState());
                break;
            }
        }

        if (!foundGuidance) {
            new Guidance(player);
            StaticMethods.sendActionBar(player, ConfigManager.defaultConfig.get().getString("ExtraAbilities.Cozmyc.Guidance.Language.ToggledOn"));
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof LivingEntity clickedEntity)) return;

        Player player = event.getPlayer();

        if (!Guidance.getCurrentSpirits().containsValue(clickedEntity)) return;

        UUID playerId = player.getUniqueId();
        LivingEntity spirit = Guidance.getCurrentSpirits().get(playerId);
        Guidance.getInstanceBySpirit(spirit).cureBlindness();

        if (spirit != null && spirit.getUniqueId().equals(Guidance.getInstanceBySpirit(spirit).getEntity().getUniqueId())) {
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
