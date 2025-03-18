package net.doodcraft.cozmyc.guidance;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.ability.SpiritualAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.LightManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class Guidance extends SpiritualAbility implements AddonAbility {

    public static final HashMap<UUID, AbilityState> trackedStates = new HashMap<>();
    public static final Map<UUID, Guidance> trackedEntities = new ConcurrentHashMap<>();
    public static final HashMap<UUID, Class<? extends LivingEntity>> trackedTypes = new HashMap<>();
    public static final HashMap<UUID, Integer> trackedFollowDistance = new HashMap<>();
    public static final HashMap<UUID, String> trackedNames = new HashMap<>();
    private static final List<Class<? extends LivingEntity>> adultSpiritClasses = new ArrayList<>();
    private static final List<Class<? extends LivingEntity>> babySpiritClasses = new ArrayList<>();
    private static final Map<UUID, LivingEntity> SPIRITS = new ConcurrentHashMap<>();
    public static int guidanceTaskId = -1;
    public final int followDistance;
    public final boolean allowChangeFollowDistance;
    public final int minFollowDistance;
    public final int maxFollowDistance;
    private final int activationLightLevel;
    private final boolean passivelyCuresBlindness;
    private final boolean passivelyCuresDarkness;
    private final boolean allowInspect;
    private final boolean alwaysDisplayName;
    private final boolean displayParticles;
    private final boolean displayParticlesFlash;
    private final int entityLightLevel;
    private final List<String> entityNames;
    private final int inspectRange;
    private final boolean playSounds;
    private final boolean cureBlindness;
    private final boolean cureDarkness;
    private final int cureRange;
    private AbilityState state;
    private long lastSpawnTime;
    private transient long lastInspectCooldownStart = 0;

    public Guidance(Player player) {
        super(player);

        boolean defaultActive = ConfigManager.defaultConfig.get().getBoolean("ExtraAbilities.Cozmyc.Guidance.DefaultActive");
        this.state = (trackedStates.get(player.getUniqueId()) == AbilityState.ACTIVE) ? AbilityState.ACTIVE : (defaultActive ? AbilityState.ACTIVE : AbilityState.INACTIVE);
        trackedStates.put(player.getUniqueId(), this.state);

        this.alwaysDisplayName = ConfigManager.defaultConfig.get().getBoolean("ExtraAbilities.Cozmyc.Guidance.AlwaysDisplayName");

        this.playSounds = ConfigManager.defaultConfig.get().getBoolean("ExtraAbilities.Cozmyc.Guidance.Teleport.Sound");
        this.displayParticles = ConfigManager.defaultConfig.get().getBoolean("ExtraAbilities.Cozmyc.Guidance.Teleport.Particles");
        this.displayParticlesFlash = ConfigManager.defaultConfig.get().getBoolean("ExtraAbilities.Cozmyc.Guidance.Teleport.Flash");

        this.followDistance = ConfigManager.defaultConfig.get().getInt("ExtraAbilities.Cozmyc.Guidance.FollowDistance.Default");
        int defaultFollowDistance = ConfigManager.defaultConfig.get().getInt("ExtraAbilities.Cozmyc.Guidance.FollowDistance.Default");
        trackedFollowDistance.put(player.getUniqueId(), trackedFollowDistance.getOrDefault(player.getUniqueId(), defaultFollowDistance));

        this.allowChangeFollowDistance = ConfigManager.defaultConfig.get().getBoolean("ExtraAbilities.Cozmyc.Guidance.FollowDistance.AllowChange");
        this.minFollowDistance = ConfigManager.defaultConfig.get().getInt("ExtraAbilities.Cozmyc.Guidance.FollowDistance.Min");
        this.maxFollowDistance = ConfigManager.defaultConfig.get().getInt("ExtraAbilities.Cozmyc.Guidance.FollowDistance.Max");

        this.allowInspect = ConfigManager.defaultConfig.get().getBoolean("ExtraAbilities.Cozmyc.Guidance.Inspect.Enabled");
        this.inspectRange = ConfigManager.defaultConfig.get().getInt("ExtraAbilities.Cozmyc.Guidance.Inspect.Range");

        this.activationLightLevel = ConfigManager.defaultConfig.get().getInt("ExtraAbilities.Cozmyc.Guidance.Passive.ActivationLightLevel");
        this.entityLightLevel = ConfigManager.defaultConfig.get().getInt("ExtraAbilities.Cozmyc.Guidance.Passive.EntityLightLevel");

        this.cureBlindness = ConfigManager.defaultConfig.get().getBoolean("ExtraAbilities.Cozmyc.Guidance.Boon.Cure.Blindness");
        this.cureDarkness = ConfigManager.defaultConfig.get().getBoolean("ExtraAbilities.Cozmyc.Guidance.Boon.Cure.Darkness");
        this.cureRange = ConfigManager.defaultConfig.get().getInt("ExtraAbilities.Cozmyc.Guidance.Boon.Cure.Range");

        this.passivelyCuresBlindness = ConfigManager.defaultConfig.get().getBoolean("ExtraAbilities.Cozmyc.Guidance.Passive.CureBlindness");
        this.passivelyCuresDarkness = ConfigManager.defaultConfig.get().getBoolean("ExtraAbilities.Cozmyc.Guidance.Passive.CureDarkness");

        this.entityNames = ConfigManager.defaultConfig.get().getStringList("ExtraAbilities.Cozmyc.Guidance.EntityNames");

        this.lastSpawnTime = 0;

        start();
    }

    public static Guidance getInstanceBySpirit(LivingEntity spirit) {
        if (spirit == null) {
            return null;
        }
        return trackedEntities.get(spirit.getUniqueId());
    }

    public static void startGuidanceTask() {
        if (guidanceTaskId != -1) {
            Bukkit.getScheduler().cancelTask(guidanceTaskId);
        }

        guidanceTaskId = Bukkit.getScheduler().runTaskTimer(ProjectKorra.plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
                if (bPlayer == null) continue;

                UUID uuid = player.getUniqueId();
                boolean hasGuidance = ElementalAbility.getAbilitiesByInstances().stream()
                        .anyMatch(ability -> ability instanceof Guidance && ability.getPlayer().getUniqueId().equals(uuid));

                if (bPlayer.hasSubElement(Element.SubElement.SPIRITUAL)) {
                    if (!hasGuidance) {
                        new Guidance(player);
                    }
                } else {
                    ElementalAbility.getAbilitiesByInstances().stream()
                            .filter(ability -> ability instanceof Guidance && ability.getPlayer().getUniqueId().equals(uuid))
                            .forEach(CoreAbility::remove);
                }
            }
        }, 0L, 40L).getTaskId();
    }

    public static Map<UUID, LivingEntity> getSpirits() {
        return SPIRITS;
    }

    @Override
    public void progress() {
        if (this.state == AbilityState.INACTIVE || this.player == null || !this.player.isOnline() || this.player.isDead() || RegionProtection.isRegionProtected(this.player, this.player.getLocation())) {
            if (this.getEntity() != null) spiritEffects();
            removeEntity();
            return;
        }

        if (this.player.getLocation().getBlock().getLightLevel() < this.activationLightLevel) {
            if (this.getEntity() == null) {
                if (System.currentTimeMillis() - this.lastSpawnTime < 2000) return;
                spawnSpirit();
                spiritEffects();
            }
            emitLight();
            updateSpirit();

            if (this.passivelyCuresBlindness || this.passivelyCuresDarkness) cureBlindness();

            updateSpiritLocation();
        } else {
            if (this.getEntity() != null) spiritEffects();
            removeEntity();
            this.lastSpawnTime = System.currentTimeMillis();
        }
    }

    @Override
    public boolean isSneakAbility() {
        return false;
    }

    @Override
    public boolean isHarmlessAbility() {
        return true;
    }

    @Override
    public long getCooldown() {
        return 0;
    }

    @Override
    public String getName() {
        return "Guidance";
    }

    @Override
    public Location getLocation() {
        return this.getEntity() != null ? this.getEntity().getLocation() : null;
    }

    @Override
    public void remove() {
        removeEntity();
        super.remove();
    }

    @Override
    public boolean isHiddenAbility() {
        return false;
    }

    @Override
    public String getInstructions() {
        return ConfigManager.defaultConfig.get().getString("ExtraAbilities.Cozmyc.Guidance.Language.Instructions");
    }

    @Override
    public String getDescription() {
        return ConfigManager.defaultConfig.get().getString("ExtraAbilities.Cozmyc.Guidance.Language.Description");
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public List<Location> getLocations() {
        if (this.getEntity() == null) return new ArrayList<>();
        return Collections.singletonList(this.getEntity().getLocation());
    }

    @Override
    public Element getElement() {
        return Element.SPIRITUAL;
    }

    @Override
    public void load() {
        ProjectKorra.plugin.getServer().getPluginManager().registerEvents(new GuidanceListener(), ProjectKorra.plugin);

        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.DefaultActive", true);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.AlwaysDisplayName", false);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.CureRange", 8);

        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.Teleport.Sound", true);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.Teleport.Particles", true);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.Teleport.Flash", true);

        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.FollowDistance.AllowChange", true);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.FollowDistance.Default", 2);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.FollowDistance.Min", 1);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.FollowDistance.Max", 10);

        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.Inspect.Enabled", true);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.Inspect.Range", 64);

        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.Boon.Cure.Blindness", false);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.Boon.Cure.Darkness", false);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.Boon.HeartParticles", 5);

        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.Passive.CureBlindness", true);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.Passive.CureDarkness", true);

        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.Passive.ActivationLightLevel", 5);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.Passive.EntityLightLevel", 15);

        List<String> adultList = new ArrayList<>();
        adultList.add("allay");
        adultList.add("bat");
        adultList.add("cat");
        adultList.add("chicken");
        adultList.add("frog");
        adultList.add("ocelot");
        adultList.add("parrot");
        adultList.add("rabbit");
        adultList.add("bee");
        adultList.add("fox");
        adultList.add("wolf");

        List<String> babyList = new ArrayList<>();
        babyList.add("panda");
        babyList.add("polar_bear");
        babyList.add("sniffer");
        babyList.add("goat");

        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.EntityTypes.Adult", adultList);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.EntityTypes.Baby", babyList);

        List<String> nameList = new ArrayList<>();
        nameList.add("#cab0ffSpirit Buddy");
        nameList.add("#cab0ffFriendly Spirit");

        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.EntityNames", nameList);

        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.Language.Description", "Friendly spirits will naturally offer help to proficient airbenders. While in darkness, a 'Spirit Buddy' will spawn, following the player as a moving light source.");
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.Language.Instructions", "When in darkness, friendly spirits will automatically spawn. With this ability selected, Left Click to toggle the ability on or off. Hold Shift to move the spirit to a specific location or switch item slots to adjust the follow distance.");
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.Language.ToggledOn", "&aFriendly spirits are now following you");
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.Language.ToggledOff", "&cFriendly spirits are no longer following you");

        ConfigManager.defaultConfig.save();

        if (!setupEntities()) {
            ProjectKorra.plugin.getLogger().info("There was an error setting up entity types, please check 'ExtraAbilities.Cozmyc.Guidance.EntityTypes` in ProjectKorra's config.yml.");
            this.stop();
            return;
        }

        ProjectKorra.plugin.getLogger().info("Guidance " + getVersion() + " by LuxaelNI and Cozmyc is now enabled!");

        startGuidanceTask();
    }

    @Override
    public void stop() {
        ProjectKorra.plugin.getLogger().info("Guidance is now disabled!");
    }

    @Override
    public String getAuthor() {
        return "LuxaelNI, Cozmyc";
    }

    @Override
    public String getVersion() {
        return "1.3.0";
    }

    private boolean setupEntities() {
        List<String> adults = ConfigManager.defaultConfig.get().getStringList("ExtraAbilities.Cozmyc.Guidance.EntityTypes.Adult");
        List<String> babies = ConfigManager.defaultConfig.get().getStringList("ExtraAbilities.Cozmyc.Guidance.EntityTypes.Baby");

        boolean noError = true;

        for (String adult : adults) {
            try {
                Class<? extends Entity> entityClass = EntityUtil.getEntityClass(adult);
                if (LivingEntity.class.isAssignableFrom(entityClass)) {
                    adultSpiritClasses.add((Class<? extends LivingEntity>) entityClass);
                } else {
                    ProjectKorra.plugin.getLogger().info("Cannot add adult entity to Guidance, ensure its a living entity: " + adult);
                }
            } catch (IllegalArgumentException exception) {
                noError = false;
                ProjectKorra.plugin.getLogger().info("Cannot add adult entity to Guidance because it might not exist, check the name: " + adult);
            }
        }

        for (String baby : babies) {
            try {
                Class<? extends Entity> entityClass = EntityUtil.getEntityClass(baby);
                if (LivingEntity.class.isAssignableFrom(entityClass)) {
                    babySpiritClasses.add((Class<? extends LivingEntity>) entityClass);
                } else {
                    ProjectKorra.plugin.getLogger().info("Cannot add baby entity to Guidance, ensure its a living entity:: " + baby);
                }
            } catch (IllegalArgumentException exception) {
                noError = false;
                ProjectKorra.plugin.getLogger().info("Cannot add baby entity to Guidance because it might not exist, check the name: " + baby);
            }
        }

        return noError;
    }

    public LivingEntity getEntity() {
        return SPIRITS.get(this.player.getUniqueId());
    }

    private void spawnSpirit() {
        Location cursor = getLookingAt(5);
        if (cursor == null) {
            cursor = player.getLocation();
        }

        Location safeLocation = findSafeLocation(cursor);
        Location spawnLocation;

        if (safeLocation != null) {
            spawnLocation = safeLocation;
        } else {
            spawnLocation = cursor;
        }

        Class<? extends LivingEntity> entityClass;
        if (!trackedTypes.containsKey(player.getUniqueId())) {
            List<Class<? extends LivingEntity>> combinedList = new ArrayList<>(adultSpiritClasses);
            combinedList.addAll(babySpiritClasses);
            entityClass = combinedList.get(ThreadLocalRandom.current().nextInt(combinedList.size()));
        } else {
            entityClass = trackedTypes.get(player.getUniqueId());  // repeat last entity type for this player
        }

        if (spawnLocation.getWorld() == null) return;

        LivingEntity entity = spawnLocation.getWorld().spawn(spawnLocation, entityClass);

        setupSpirit(entity);

        SPIRITS.put(player.getUniqueId(), entity);
        trackedTypes.put(player.getUniqueId(), entityClass);
        trackedEntities.put(entity.getUniqueId(), this);
    }

    private void updateSpirit() {
        if (this.getEntity() instanceof Ageable && isBabySpirit(this.getEntity())) {
            ((Ageable) this.getEntity()).setBaby();
        }

        if (this.getEntity() instanceof Mob && ((Mob) this.getEntity()).getTarget() != null) {
            ((Mob) this.getEntity()).setTarget(null);
        }
    }

    public void cureBlindness() {
        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(this.getEntity().getLocation(), this.cureRange)) {
            if (entity instanceof Player p) {
                if ((this.cureDarkness || this.passivelyCuresDarkness) && p.hasPotionEffect(PotionEffectType.DARKNESS)) {
                    p.removePotionEffect(PotionEffectType.DARKNESS);
                }
                if ((this.cureBlindness || this.passivelyCuresBlindness) && p.hasPotionEffect(PotionEffectType.BLINDNESS)) {
                    p.removePotionEffect(PotionEffectType.BLINDNESS);
                }
            }
        }
    }

    private Location getLookingAt(int range) {
        BlockIterator blockIterator = new BlockIterator(player, range);
        Block block;

        while (blockIterator.hasNext()) {
            block = blockIterator.next();
            if (block.getType() != Material.AIR && block.getType() != Material.WATER && !block.isPassable()) {
                return block.getLocation();
            }
        }

        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection().normalize();
        return eyeLocation.add(direction.multiply(trackedFollowDistance.get(player.getUniqueId())));
    }

    private void updateSpiritLocation() {
        if (this.getEntity().getLocation().getWorld() == null) return;

        // inspect mode
        if (this.allowInspect && this.player.isSneaking() && this.bPlayer.getBoundAbilityName().equalsIgnoreCase("Guidance") && !isInspectOnCooldown()) {
            Location cursor = getLookingAt(this.inspectRange);
            if (cursor == null) return;

            Location safeLocation = findSafeLocation(cursor);
            if (safeLocation == null) return;

            if (!this.getEntity().getLocation().getWorld().equals(safeLocation.getWorld()) || this.getEntity().getLocation().distance(safeLocation) >= 16) {
                spiritEffects();
                this.getEntity().teleport(safeLocation);
                spiritEffects();
            }

            if (this.getEntity().getLocation().getWorld().equals(safeLocation.getWorld())) {
                Vector dir = safeLocation.clone().subtract(this.getEntity().getLocation()).toVector();
                Location loc = this.getEntity().getLocation();
                loc.setDirection(dir);
                this.getEntity().teleport(loc);
            }

            if (this.getEntity().getLocation().distance(safeLocation) > 1) {
                Vector velocity = safeLocation.clone().toVector().subtract(this.getEntity().getLocation().toVector()).normalize();
                this.getEntity().setVelocity(velocity);
            }
            return;
        }

        // follow mode
        Location cursor = getLookingAt(trackedFollowDistance.get(player.getUniqueId()));
        if (cursor == null) return;

        Location safeLocation = findSafeLocation(cursor);
        if (safeLocation == null) return;

        int currentFollowDistance = trackedFollowDistance.get(player.getUniqueId());
        double entityToPlayerDistance = this.getEntity().getLocation().distance(this.player.getLocation());

        if (!this.getEntity().getLocation().getWorld().equals(this.player.getLocation().getWorld()) || entityToPlayerDistance >= currentFollowDistance + maxFollowDistance) {
            spiritEffects();
            this.getEntity().teleport(safeLocation);
            spiritEffects();
        } else {
            if (this.getEntity().getLocation().getWorld().equals(this.player.getLocation().getWorld())) {
                Vector dir = this.player.getLocation().clone().add(0, 1, 0).subtract(this.getEntity().getLocation()).toVector();
                Location loc = this.getEntity().getLocation();
                loc.setDirection(dir);
                this.getEntity().teleport(loc);
            }

            if (entityToPlayerDistance < currentFollowDistance - 0.5) {
                Vector awayVector = this.getEntity().getLocation().toVector().subtract(this.player.getLocation().clone().add(0, 1, 0).toVector()).normalize();
                this.getEntity().setVelocity(awayVector.multiply(0.2));
            } else if (entityToPlayerDistance > currentFollowDistance + 0.5) {
                Vector towardsVector = this.player.getLocation().clone().add(0, 1, 0).toVector().subtract(this.getEntity().getLocation().toVector()).normalize();
                this.getEntity().setVelocity(towardsVector.multiply(0.2));
            } else {
                this.getEntity().setVelocity(new Vector(0, 0, 0));
            }
        }
    }

    private Location findSafeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;

        Location checkLoc = loc.clone();
        for (int i = 0; i < 10; i++) {

            if (isLocationSafe(checkLoc)) {
                return checkLoc;
            }

            checkLoc.add(0, 1, 0);
        }

        return loc;
    }

    private boolean isLocationSafe(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        return !loc.getBlock().getType().isSolid() && !loc.clone().add(0, 1, 0).getBlock().getType().isSolid();
    }

    private void setupSpirit(LivingEntity entity) {
        String entityName;
        if (trackedNames.containsKey(player.getUniqueId())) {
            entityName = trackedNames.get(player.getUniqueId());
        } else {
            entityName = this.entityNames.get(ThreadLocalRandom.current().nextInt(this.entityNames.size()));
            trackedNames.put(player.getUniqueId(), entityName);
        }
        entity.setCustomName(StaticMethods.addColor(StaticMethods.parse(entityName, this.player)));

        if (this.alwaysDisplayName) entity.setCustomNameVisible(true);

        entity.setGlowing(true);
        entity.setGravity(false);
        entity.setCollidable(false);

        entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, PotionEffect.INFINITE_DURATION, 0, true, true));

        if (entity instanceof Ageable && isBabySpirit(this.getEntity())) {
            ((Ageable) entity).setBaby();
        }

        if (entity instanceof Slime) {
            ((Slime) entity).setSize(0);
        }
    }

    private void spiritEffects() {
        Location loc = this.getEntity().getLocation();
        if (this.displayParticles) {
            this.getEntity().getWorld().spawnParticle(
                    Particle.END_ROD, loc, 4, 0.5, 1.5, 0.5, 0.02, null, true
            );
        }
        if (this.displayParticlesFlash) {
            this.getEntity().getWorld().spawnParticle(
                    Particle.FLASH, loc, 1, 0.5, 1.5, 0.5, 0.02, null, true
            );
        }

        if (this.playSounds && this.getEntity().getLocation().getWorld() != null)
            this.getEntity().getLocation().getWorld().playSound(this.getEntity().getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.65f);
    }

    private void emitLight() {
        Block mainBlock = this.getEntity().getLocation().getBlock();
        Location baseLocation = mainBlock.getLocation();

        int[][] offsets = {
                {0, 0, 0},
                {0, 1, 0},
                {0, 2, 0},
                {1, 0, 0},
                {0, 0, 1},
                {-1, 0, 0},
                {0, 0, -1}
        };

        for (int[] offset : offsets) {
            Location location = baseLocation.clone().add(offset[0], offset[1], offset[2]);
            Block block = location.getBlock();

            if (block.isEmpty() || block.getType() == Material.WATER) {
                LightManager.createLight(location).brightness(entityLightLevel).timeUntilFadeout(350L).emit();
            }
        }
    }

    public void removeEntity() {
        LivingEntity spirit = SPIRITS.get(this.player.getUniqueId());
        SPIRITS.remove(this.player.getUniqueId());
        if (spirit != null) spirit.remove();
    }

    public AbilityState getState() {
        return this.state;
    }

    public void setState(AbilityState state) {
        this.state = state;
    }

    private boolean isBabySpirit(LivingEntity entity) {
        for (Class<? extends LivingEntity> clazz : babySpiritClasses) {
            if (clazz.isInstance(entity)) {
                return true;
            }
        }
        return false;
    }

    public void startInspectCooldown() {
        this.lastInspectCooldownStart = System.currentTimeMillis();
    }

    public boolean isInspectOnCooldown() {
        int inspectCooldownTicks = 60;
        return System.currentTimeMillis() - this.lastInspectCooldownStart < inspectCooldownTicks * 50L;
    }
}
