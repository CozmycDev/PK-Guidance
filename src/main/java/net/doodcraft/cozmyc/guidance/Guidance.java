package net.doodcraft.cozmyc.guidance;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.SpiritualAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.ParticleEffect;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Guidance extends SpiritualAbility implements AddonAbility {

    private static final Pattern colorPattern = Pattern.compile("#[a-fA-F0-9]{6}");
    private static final Map<UUID, LivingEntity> SPIRITS = new ConcurrentHashMap<>();
    private static final List<Class<? extends LivingEntity>> adultSpiritClasses = new ArrayList<>();
    private static final List<Class<? extends LivingEntity>> babySpiritClasses = new ArrayList<>();

    private final int activationLightLevel;
    private final boolean allowInspect;
    private final boolean alwaysDisplayName;
    private final boolean displayParticles;
    private final int entityLightLevel;
    private final List<String> entityNames;
    private final int followDistance;
    private final int inspectRange;
    private final boolean playSounds;
    private final boolean removesBlindness;

    private AbilityState state;
    private long lastSpawnTime;

    public Guidance(Player player) {
        super(player);

        this.activationLightLevel = ConfigManager.defaultConfig.get().getInt("ExtraAbilities.Cozmyc.Guidance.ActivationLightLevel");
        this.allowInspect = ConfigManager.defaultConfig.get().getBoolean("ExtraAbilities.Cozmyc.Guidance.AllowInspect");
        this.alwaysDisplayName = ConfigManager.defaultConfig.get().getBoolean("ExtraAbilities.Cozmyc.Guidance.AlwaysDisplayName");
        this.displayParticles = ConfigManager.defaultConfig.get().getBoolean("ExtraAbilities.Cozmyc.Guidance.DisplayParticles");
        this.entityLightLevel = ConfigManager.defaultConfig.get().getInt("ExtraAbilities.Cozmyc.Guidance.EntityLightLevel");
        this.entityNames = ConfigManager.defaultConfig.get().getStringList("ExtraAbilities.Cozmyc.Guidance.EntityNames");
        this.followDistance = ConfigManager.defaultConfig.get().getInt("ExtraAbilities.Cozmyc.Guidance.FollowDistance");
        this.inspectRange = ConfigManager.defaultConfig.get().getInt("ExtraAbilities.Cozmyc.Guidance.InspectRange");
        this.playSounds = ConfigManager.defaultConfig.get().getBoolean("ExtraAbilities.Cozmyc.Guidance.PlaySounds");
        this.removesBlindness = ConfigManager.defaultConfig.get().getBoolean("ExtraAbilities.Cozmyc.Guidance.RemovesBlindness");

        this.state = AbilityState.ACTIVE;
        this.lastSpawnTime = 0;

        start();
    }

    public static Map<UUID, LivingEntity> getSpirits() {
        return SPIRITS;
    }

    public static void sendActionBar(Player player, String message) {
        if (player == null || !player.isOnline() || message == null) {
            return;
        }

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(addColor(message)));
    }

    public static String addColor(String message) {
        Matcher matcher = colorPattern.matcher(message);

        while (matcher.find()) {
            String color = message.substring(matcher.start(), matcher.end());
            message = message.replace(color, net.md_5.bungee.api.ChatColor.of(color) + "");
            matcher = colorPattern.matcher(message);
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String parse(String message, Player player) {
        return message.replaceAll("<player>", player.getName());
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
            cureBlindness();
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

        List<Class<? extends LivingEntity>> combinedList = new ArrayList<>(adultSpiritClasses);
        combinedList.addAll(babySpiritClasses);

        Class<? extends LivingEntity> entityClass = combinedList.get(ThreadLocalRandom.current().nextInt(combinedList.size()));

        if (spawnLocation.getWorld() == null) return;
        LivingEntity entity = spawnLocation.getWorld().spawn(spawnLocation, entityClass);
        setupSpirit(entity);
        SPIRITS.put(player.getUniqueId(), entity);
        addEntityToNoCollisionTeam(entity);
    }

    private void updateSpirit() {
        if (this.getEntity() instanceof Ageable && isBabySpirit(this.getEntity())) {
            ((Ageable) this.getEntity()).setBaby();
        }

        if (this.getEntity() instanceof Mob && ((Mob) this.getEntity()).getTarget() != null) {
            ((Mob) this.getEntity()).setTarget(null);
        }
    }

    private void cureBlindness() {
        if (!this.removesBlindness) return;
        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(this.getEntity().getLocation(), 5)) {
            if (entity instanceof Player p) {
                if (p.hasPotionEffect(PotionEffectType.DARKNESS) || p.hasPotionEffect(PotionEffectType.BLINDNESS)) {
                    p.removePotionEffect(PotionEffectType.DARKNESS);
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
        return eyeLocation.add(direction.multiply(this.followDistance));
    }

    private void updateSpiritLocation() {
        if (this.getEntity().getLocation().getWorld() == null) return;

        // inspect mode
        if (this.allowInspect && this.player.isSneaking() && this.bPlayer.getBoundAbilityName().equalsIgnoreCase("Guidance")) {
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
        Location cursor = getLookingAt(this.followDistance);
        if (cursor == null) return;

        Location safeLocation = findSafeLocation(cursor);
        if (safeLocation == null) return;

        if (!this.getEntity().getLocation().getWorld().equals(this.player.getLocation().getWorld()) || this.getEntity().getLocation().distance(this.player.getLocation()) >= this.followDistance * 2) {
            spiritEffects();
            this.getEntity().teleport(safeLocation);
            spiritEffects();
        }

        if (this.getEntity().getLocation().getWorld().equals(this.player.getLocation().getWorld())) {
            Vector dir = this.player.getLocation().clone().add(0, 1, 0).subtract(this.getEntity().getLocation()).toVector();
            Location loc = this.getEntity().getLocation();
            loc.setDirection(dir);
            this.getEntity().teleport(loc);
        }

        if (this.getEntity().getLocation().distance(this.player.getLocation()) > this.followDistance) {
            Vector velocity = this.player.getLocation().clone().add(0, 1, 0).toVector().subtract(this.getEntity().getLocation().toVector()).normalize();
            this.getEntity().setVelocity(velocity);
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
        String randomName = this.entityNames.get(ThreadLocalRandom.current().nextInt(this.entityNames.size()));
        entity.setCustomName(addColor(parse(randomName, this.player)));

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

    @Override
    public void remove() {
        removeEntity();
        super.remove();
    }

    public void removeEntity() {
        if (this.getEntity() != null) {
            removeEntityFromNoCollisionTeam(this.getEntity());
            this.getEntity().remove();
        }

        SPIRITS.remove(this.player.getUniqueId());
    }

    private void removeEntityFromNoCollisionTeam(Entity entity) {
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager == null) return;
        Scoreboard scoreboard = scoreboardManager.getMainScoreboard();

        Team noCollisionTeam = scoreboard.getTeam("NoCollisionSpirits");
        if (noCollisionTeam == null) {
            noCollisionTeam = scoreboard.registerNewTeam("NoCollisionSpirits");
            noCollisionTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }

        if (noCollisionTeam.hasEntry(entity.getUniqueId().toString())) {
            noCollisionTeam.removeEntry(entity.getUniqueId().toString());
        }
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

    private void spiritEffects() {
        if (this.displayParticles) {
            ParticleEffect.FLASH.display(this.getEntity().getLocation(), 1, 0.5, 1.5, 0.5);
            ParticleEffect.END_ROD.display(this.getEntity().getLocation(), 4, 0.5, 1.5, 0.5);
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
                LightManager.get().addLight(location, entityLightLevel, 350L, null, null);
            }
        }
    }

    @Override
    public Element getElement() {
        return Element.SPIRITUAL;
    }

    @Override
    public void load() {
        ProjectKorra.plugin.getServer().getPluginManager().registerEvents(new GuidanceListener(), ProjectKorra.plugin);

        List<String> nameList = new ArrayList<>();
        nameList.add("&aSpirit Buddy");
        nameList.add("&aFriendly Spirit");

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

        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.ActivationLightLevel", 5);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.AllowInspect", true);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.AlwaysDisplayName", true);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.DisplayParticles", true);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.EntityLightLevel", 15);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.EntityNames", nameList);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.FollowDistance", 4);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.InspectRange", 64);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.PlaySounds", true);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.RemovesBlindness", true);

        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.Language.Description", "Friendly spirits will naturally offer help to proficient airbenders. While in darkness, a 'Spirit Buddy' will spawn, following the player as a moving light source.");
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.Language.Instructions", "When in darkness, friendly spirits will automatically spawn. With this ability selected, Left Click to toggle the ability on or off. Hold Shift to move the spirit to a specific location.");
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.Language.ToggledOn", "&aFriendly spirits are now following you");
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.Language.ToggledOff", "&cFriendly spirits are no longer following you");
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.EntityTypes.Adult", adultList);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.Cozmyc.Guidance.EntityTypes.Baby", babyList);

        if (!setupEntities()) {
            ProjectKorra.plugin.getLogger().info("There was an error setting up entity types, please check 'ExtraAbilities.Cozmyc.Guidance.EntityTypes` in ProjectKorra's config.yml.");
            this.stop();
            return;
        }

        registerNoCollisionTeam();

        ProjectKorra.plugin.getLogger().info("Guidance by LuxaelNI and Cozmyc is now enabled!");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline() || player.isDead()) continue;
            BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
            if (bPlayer.hasSubElement(Element.SubElement.SPIRITUAL)) {
                new Guidance(player);
            }
        }
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
        return "1.1.0";
    }

    @SuppressWarnings("unchecked")
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

    // todo
    private void registerNoCollisionTeam() {
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager == null) return;
        Scoreboard scoreboard = scoreboardManager.getMainScoreboard();

        Team noCollisionTeam = scoreboard.getTeam("NoCollisionSpirits");
        if (noCollisionTeam == null) {
            noCollisionTeam = scoreboard.registerNewTeam("NoCollisionSpirits");
        }

        noCollisionTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
    }

    public AbilityState getState() {
        return this.state;
    }

    public void setState(AbilityState state) {
        this.state = state;
    }

    // todo
    private void addEntityToNoCollisionTeam(Entity entity) {
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager == null) return;
        Scoreboard scoreboard = scoreboardManager.getMainScoreboard();

        Team noCollisionTeam = scoreboard.getTeam("NoCollisionSpirits");
        if (noCollisionTeam == null) {
            noCollisionTeam = scoreboard.registerNewTeam("NoCollisionSpirits");
            noCollisionTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }

        if (!noCollisionTeam.hasEntry(entity.getUniqueId().toString())) {
            noCollisionTeam.addEntry(entity.getUniqueId().toString());
        }
    }

    private boolean isBabySpirit(LivingEntity entity) {
        for (Class<? extends LivingEntity> clazz : babySpiritClasses) {
            if (clazz.isInstance(entity)) {
                return true;
            }
        }
        return false;
    }
}