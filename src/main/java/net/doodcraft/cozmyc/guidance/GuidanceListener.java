package net.doodcraft.cozmyc.guidance;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.event.AbilityDamageEntityEvent;
import com.projectkorra.projectkorra.event.AbilityVelocityAffectEntityEvent;
import com.projectkorra.projectkorra.event.BendingReloadEvent;
import com.projectkorra.projectkorra.util.ParticleEffect;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockReceiveGameEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.GenericGameEvent;

import java.util.UUID;

public class GuidanceListener implements Listener {

    @EventHandler
    public void onEvent(GenericGameEvent event) {
        if (event.getEntity() == null) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (event.getEntity() instanceof Warden) {
            Warden warden = (Warden) event.getEntity();
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
        if (!(event.getTarget() instanceof LivingEntity)) return;
        if (event.getTarget() != null && Guidance.getSpirits().containsValue((LivingEntity) event.getTarget())) {
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
                if (!(ability instanceof Guidance)) continue;
                Guidance guidance = (Guidance) ability;
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
    public void onReload(BendingReloadEvent event) {
        LightManager.getInstance().removeAllLights();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.isOnline() || player.isDead()) return;
        Bukkit.getScheduler().runTaskLater(ProjectKorra.plugin, () -> {
            BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
            if (bPlayer.hasSubElement(Element.SubElement.SPIRITUAL)) {
                new Guidance(player);
            }
        }, 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        for (Ability ability : ElementalAbility.getAbilitiesByInstances()) {
            if (ability.getName().equalsIgnoreCase("Guidance") && ability.getPlayer().getUniqueId().equals(uuid)) {
                Guidance guidance = (Guidance) ability;
                guidance.remove();
            }
        }
    }

    @EventHandler
    public void onClick(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player player = event.getPlayer();
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

        if (event.isCancelled() || bPlayer == null || !bPlayer.getBoundAbilityName().equalsIgnoreCase("Guidance"))
            return;

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
                break;
            }
        }

        if (!foundGuidance) {
            new Guidance(player);
            Guidance.sendActionBar(player, ConfigManager.defaultConfig.get().getString("ExtraAbilities.Cozmyc.Guidance.Language.ToggledOn"));
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof LivingEntity)) return;

        LivingEntity clickedEntity = (LivingEntity) event.getRightClicked();
        Player player = event.getPlayer();

        if (!Guidance.getSpirits().containsValue(clickedEntity)) return;

        UUID playerId = player.getUniqueId();
        LivingEntity spirit = Guidance.getSpirits().get(playerId);

        if (spirit != null && spirit.getUniqueId().equals(spirit.getUniqueId())) {
            ParticleEffect.HEART.display(spirit.getLocation(), 5, Math.random(), Math.random(), Math.random());
            event.setCancelled(true);
        }
    }
}