# VillagerLagPreventer
Spigot plugin for minimizing lag caused by villagers by soft-disabling their AI on certain configurable blocks

### Features
 - Soft-disables (`setAware(false)`) villagers when the block types of the blocks below or 1 or 2 blocks above them are found in the `villager-noai-blocks` in the config.
 - That's it.

## Warning
Because this plugin uses reflection to access internal Minecraft features, it only works on a specific version.
### Current supported version: Spigot/Paper 1.19

Using [PaperMC](https://papermc.io) is recommended for your server because it fixes some Spigot and Vanilla bugs and improves performance.