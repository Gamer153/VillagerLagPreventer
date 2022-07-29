package dev.gamer153.villagerlagpreventer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public final class VillagerLagPreventer extends JavaPlugin {

    private final Logger logger = getLogger();
    private final List<Material> noAIMaterials = new ArrayList<>();
    private Method handleMethod = null;
    private Method handleGetLevelUpReadyMethod = null;
    BukkitTask task = null;

    @Override
    @SuppressWarnings("deprecation")
    public void onEnable() {
        if (Bukkit.getUnsafe().getDataVersion() != 3105) { // data version for 1.19
            logger.severe("For this plugin to work, the server has to run MC 1.19! VillagerLagPrevention is disabled.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        try {
            handleMethod = Villager.class.getMethod("getHandle");
            handleMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        if (!new File(getDataFolder(), "config.yml").exists()) {
            logger.info("New config.yml created! Edit it and then reload it with /vlpreload.");
            saveDefaultConfig();
        }

        for (String mat : getConfig().getStringList("villager-noai-blocks")) {
            Material material = Material.matchMaterial(mat);
            if (material == null) {
                logger.warning("Block type \"${mat}\" not found!");
            } else {
                noAIMaterials.add(material);
            }
        }

        World world;
        try {
            Properties props = new Properties();
            var reader = new BufferedReader(new FileReader("server.properties"));
            props.load(reader);
            reader.close();
            world = Bukkit.getWorld(props.getProperty("level-name", "world"));
            if (world == null) {
                logger.severe("Your main server world was not found. Please check the level-name in your server.properties! VillagerLagPrevention is disabled.");
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }
        } catch (IOException exception) {
            logger.severe("Error with reading server.properties:");
            exception.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        task = startTask(world);

        getCommand("villagerlagpreventerreload").setExecutor((sender, command, args, label) -> {
            if (task != null) task.cancel();
            reloadConfig();
            task = startTask(world);
            return true;
        });

        logger.info("VillagerLagPreventer by Gamer153 (Gamer153#9675, https://gamer153.dev) was enabled.");
    }

    private BukkitTask startTask(World world) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                for (Villager entity : world.getEntitiesByClass(Villager.class)) {
                    if (entity == null) continue;
                    if (entity.getWorld().getChunkAt(entity.getLocation()).isLoaded()) {
                        try {
                            if (entity.getProfession() != Villager.Profession.NONE && noRecipesBlockedFor(entity) && !hasLevelUpReady(entity))
                                entity.setAware(!hasNoAIBlocks(entity));
                            else entity.setAware(true);
                        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                            logger.severe("There was an error with determining if a villager had a level up ready, please contact Gamer153#9675 with the following error message:");
                            e.printStackTrace();
                            task.cancel();
                        }
                    }
                }
            }
        }.runTaskTimer(this, 10 * 20L, getConfig().getLong("villager-noai-check-ticks"));
    }

    private boolean noRecipesBlockedFor(Villager villager) {
        for (MerchantRecipe recipe : villager.getRecipes()) {
            if (recipe.getUses() >= recipe.getMaxUses()) return false;
        }
        return true;
    }

    private boolean hasLevelUpReady(Villager villager) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Object handle = handleMethod.invoke(villager);
        if (handleGetLevelUpReadyMethod == null) {
            handleGetLevelUpReadyMethod = handle.getClass().getMethod("gf");
            handleGetLevelUpReadyMethod.setAccessible(true);
        }
        return (Boolean) handleGetLevelUpReadyMethod.invoke(handle);
    }

    private boolean hasNoAIBlocks(Villager villager) {
        World world = villager.getWorld();
        Location loc = villager.getLocation();
        List<Material> blocksToCheck = new ArrayList<>();
        blocksToCheck.add(world.getType(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ()));
        blocksToCheck.add(world.getType(loc.getBlockX(), loc.getBlockY() + 2, loc.getBlockZ()));
        blocksToCheck.add(world.getType(loc.getBlockX(), loc.getBlockY() + 3, loc.getBlockZ()));
        for (Material block : blocksToCheck) {
            if (noAIMaterials.contains(block)) return true;
        }
        return false;
    }

    @Override
    public void onDisable() {
        if (task != null) task.cancel();
    }
}
