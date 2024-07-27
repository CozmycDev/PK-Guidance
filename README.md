# Guidance Ability for ProjectKorra

This is an addon ability for the [ProjectKorra](https://projectkorra.com/) plugin for Spigot Minecraft servers. This
ability allows players to summon friendly spirits that provide light and assistance. This concept was designed by
LuxaelNi.

https://github.com/user-attachments/assets/e21559eb-ca2e-48b1-bc52-4e5c203e6b33

## Description

**Guidance** is a Spiritual ability that enables players to summon a 'Spirit Buddy' while in darkness. This friendly
spirit follows the player, acting as a moving light source and providing various beneficial effects.

### Features

- **Summon Spirit**: Summon a friendly spirit that follows you around and provides light.
- **Blindness Cure**: Automatically cures blindness and darkness effects from nearby players.
- **Particle Effects**: Displays particles around the spirit for a visual effect.
- **Customizable**: Various settings can be adjusted, such as light levels, entity types, and names.

## Instructions

- **Activation**: When in darkness, friendly spirits will automatically spawn. With this ability selected, Left Click to
  toggle the ability on or off. Hold Shift to move the spirit to a specific location.

## Installation

1. Download the latest `guidance.jar` file from [releases](https://github.com/CozmycDev/PK-Guidance/releases).
2. Place the `guidance.jar` file in the `./plugins/ProjectKorra/Abilities` directory.
3. Restart your server or reload the ProjectKorra plugin with `/b reload` to enable the ability.

## Compatibility

- **Minecraft Version**: Tested and working on MC 1.20.4.
- **ProjectKorra Version**: Tested and working on PK 1.11.2 and 1.11.3. Might support earlier versions too.

## Configuration

The configuration for this ability can be found in the `ProjectKorra` configuration file (`config.yml`). Below are the
default settings:

```yaml
ExtraAbilities:
  Cozmyc:
    Guidance:
      ActivationLightLevel: 5
      AllowInspect: true
      AlwaysDisplayName: true
      DisplayParticles: true
      EntityLightLevel: 15
      EntityNames:
      - '&aSpirit Buddy'
      - '&aFriendly Spirit'
      FollowDistance: 4
      InspectRange: 64
      PlaySounds: true
      RemovesBlindness: true
      Language:
        Description: 'Friendly spirits will naturally offer help to proficient airbenders. While in darkness, a Spirit Buddy will spawn, following the player as a moving light source.'
        Instructions: 'When in darkness, friendly spirits will automatically spawn. With this ability selected, Left Click to toggle the ability on or off. Hold Shift to move the spirit to a specific location.'
        ToggledOn: '&aFriendly spirits are now following you'
        ToggledOff: '&cFriendly spirits are no longer following you'
      EntityTypes:
        Adult:
        - allay
        - bat
        - cat
        - chicken
        - frog
        - ocelot
        - parrot
        - rabbit
        - bee
        - fox
        - wolf
        Baby:
        - panda
        - polar_bear
        - sniffer
        - goat
```

## Development

- **Authors**: LuxaelNI, Cozmyc

## Features

### Summon Spirit

When the player's light level is below the activation threshold, a spirit is summoned. The spirit follows the player and
acts as a moving light source.

### Blindness Cure

If the `RemovesBlindness` setting is enabled, the spirit will automatically cure nearby players of blindness and
darkness effects.

### Particle Effects

If the `DisplayParticles` setting is enabled, the spirit will emit particles, adding a visual effect to its presence.

### Customizable

Various aspects of the ability can be customized via the configuration file, including the types of entities used for
the spirits, their names, and the light levels they provide.

## Usage

- **Summon Spirit**: The spirit is automatically summoned when the player's light level is below the activation
  threshold.
- **Move Spirit**: Hold Shift to inspect and move the spirit to a specific location.
- **Toggle Ability**: Left Click to toggle the ability on or off.

## Setup Entities

The ability supports a variety of entity types for the spirits, which can be customized in the configuration file. The
default setup includes a mix of adult and baby entities, ensuring a diverse range of spirits. You can find a full list of entity types available [here](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/entity/EntityType.html) but note only [LivingEntity](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/entity/LivingEntity.html) types are allowed. There may be some issues with specific hostile mobs, so add them at your own risk!
