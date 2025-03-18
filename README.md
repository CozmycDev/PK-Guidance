[![GitHub Pre-Release](https://img.shields.io/github/release-pre/CozmycDev/PK-Guidance.svg)](https://github.com/CozmycDev/PK-Guidance/releases)
[![Github All Releases](https://img.shields.io/github/downloads/CozmycDev/PK-Guidance/total.svg)](https://github.com/CozmycDev/PK-Guidance/releases)
![Size](https://img.shields.io/github/repo-size/CozmycDev/PK-Guidance.svg)

# Guidance Ability for ProjectKorra

This is an addon ability for the [ProjectKorra](https://projectkorra.com/) plugin for Spigot Minecraft servers. This ability allows players to summon friendly spirits that provide light and assistance. 
This concept was designed by LuxaelNi.

https://github.com/user-attachments/assets/e21559eb-ca2e-48b1-bc52-4e5c203e6b33

## Description

**Guidance** is a Spiritual ability that enables players to summon a 'Spirit Buddy' while in darkness. This friendly spirit follows the player, acting as a moving light source and providing various beneficial effects.

### Features

- **Summon Spirit**: Summon a friendly spirit that follows you around and provides light.
- **Blindness/Darkness Cure**: Automatically cures blindness and darkness effects from nearby players or through interaction.
- **Adjustable Follow Distance**: Players can change how far the spirit follows by holding shift and scrolling.
- **Customizable**: Various settings and defaults can be adjusted, such as particles, light levels, entity types, and names.
- **Persistent Preferences**: Remembers a player's spirit name and entity type across instances (but not restarts yet).

## Instructions

- **Activation**: When in darkness, friendly spirits will automatically spawn if `DefaultActive` is enabled. With this ability selected, Left Click to toggle the ability on or off. Hold Shift to move the spirit to a specific location.
- **Adjust Follow Distance**: With this ability selected, hold Shift and scroll to adjust how closely the spirit follows.
- **Interaction Mechanic**: Players can interact with their spirit by right-clicking, triggering unique effects.

## Installation

1. Download the latest `guidance.jar` file from [releases](https://github.com/CozmycDev/PK-Guidance/releases).
2. Place the `guidance.jar` file in the `./plugins/ProjectKorra/Abilities` directory.
3. Restart your server or reload the ProjectKorra plugin with `/b reload` to enable the ability.

## Compatibility

- **Minecraft Version**: Tested and working on MC 1.20.4, 1.21.1 and 1.21.4.
- **ProjectKorra Version**: Tested and working on PK 1.11.2, 1.11.3, and PK 1.12 BETA 12 and PRE-RELEASE 3.

## Configuration

The configuration for this ability can be found in the `ProjectKorra` configuration file (`config.yml`). Below are the default settings:

```yaml
ExtraAbilities:
  Cozmyc:
    Guidance:
      DefaultActive: true
      AlwaysDisplayName: false
      CureRange: 8
      Teleport:
        Sound: true
        Particles: true
        Flash: true
      FollowDistance:
        AllowChange: true
        Default: 2
        Min: 1
        Max: 10
      Inspect:
        Enabled: true
        Range: 64
      Boon:
        Cure:
          Blindness: false
          Darkness: false
        HeartParticles: 5
      Passive:
        CureBlindness: true
        CureDarkness: true
        ActivationLightLevel: 5
        EntityLightLevel: 15
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
      EntityNames:
        - '#cab0ffSpirit Buddy'
        - '#cab0ffFriendly Spirit'
      Language:
        Description: Friendly spirits will naturally offer help to proficient airbenders.
          While in darkness, a 'Spirit Buddy' will spawn, following the player as
          a moving light source.
        Instructions: When in darkness, friendly spirits will automatically spawn.
          With this ability selected, Left Click to toggle the ability on or off.
          Hold Shift to move the spirit to a specific location or switch item slots
          to adjust the follow distance.
        ToggledOn: '&aFriendly spirits are now following you'
        ToggledOff: '&cFriendly spirits are no longer following you'
```

## Development

- **Authors**: LuxaelNI, Cozmyc
