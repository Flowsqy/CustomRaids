package fr.flowsqy.customraids;

import fr.flowsqy.abstractmenu.item.ItemBuilder;
import fr.flowsqy.abstractmob.entity.EntityBuilder;
import fr.flowsqy.customevents.api.Event;
import fr.flowsqy.customevents.api.EventData;
import fr.flowsqy.customraids.data.RaidsData;
import fr.flowsqy.customraids.data.SpawnData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

public class RaidsEvent implements Event, Listener {

    private final static int RANDOM_CONSTANT = 100_000;

    private final EventData eventData;
    private final RaidsData raidsData;

    private final Map<UUID, Entity> aliveEntities;
    private Location spawnLocation;
    private boolean started;

    /**
     * Create a raid event
     *
     * @param eventData The generic parameters of the event
     * @param raidsData The specific parameters of the event
     */
    RaidsEvent(EventData eventData, RaidsData raidsData) {
        this.eventData = eventData;
        this.raidsData = raidsData;
        this.aliveEntities = new HashMap<>();
    }

    @Override
    public void perform() {
        // End previous event
        killPreviousEntities();

        // Start the new one
        final SpawnData spawnData = raidsData.spawnData();
        final World world = Bukkit.getWorld(spawnData.worldName());
        if (world == null) {
            return;
        }

        // Select location
        final Random random = new Random();
        final int doubledRadius = spawnData.spawnRadius() * 2;
        final Vector vector = new Vector(
                random.nextInt(doubledRadius * RANDOM_CONSTANT) / RANDOM_CONSTANT - spawnData.spawnRadius(),
                0,
                random.nextInt(doubledRadius * RANDOM_CONSTANT) / RANDOM_CONSTANT - spawnData.spawnRadius()
        );
        int radius_multiplier;
        do {
            radius_multiplier = random.nextInt(spawnData.spawnRadius() * RANDOM_CONSTANT);
        } while (radius_multiplier <= spawnData.minSpawnRadius() * RANDOM_CONSTANT);
        vector.normalize().multiply(radius_multiplier / RANDOM_CONSTANT);
        spawnLocation = world.getSpawnLocation().add(vector);

        // Spawn and register entities
        for (EntityBuilder entityBuilder : raidsData.raidEntities()) {
            final List<Entity> spawnedEntities = entityBuilder.spawn(
                    raidsData.abstractMobPlugin(),
                    spawnLocation,
                    entityBuilder.getRadius(),
                    true
            );
            for (Entity entity : spawnedEntities) {
                aliveEntities.put(entity.getUniqueId(), entity);
            }
        }

        // Load the progression bar
        if (raidsData.progressionBar().isEnable()) {
            raidsData.progressionBar().load(
                    world,
                    spawnLocation.getBlockX(),
                    spawnLocation.getBlockZ(),
                    aliveEntities.size()
            );
        }

        // Send start message
        sendMessage(raidsData.startMessage());

        started = true;
    }

    @Override
    public EventData getData() {
        return eventData;
    }

    @Override
    public void check() {
        if (started) {
            // Remove all invalid entities
            aliveEntities.entrySet()
                    .stream()
                    .filter(entry -> !entry.getValue().isValid())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet()) // Get every key of invalid entities
                    .forEach(aliveEntities::remove); // Remove them

            refreshProgressionBar();
            checkFinish();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onDeath(EntityDeathEvent event) {
        // Check if the dying entity is one of the raid entity
        if (aliveEntities.remove(event.getEntity().getUniqueId()) != null) {
            refreshProgressionBar();
            checkFinish();
        }
    }

    /**
     * Refresh the progression bar if needed
     */
    private void refreshProgressionBar() {
        if (raidsData.progressionBar().isEnable()) {
            raidsData.progressionBar().refresh(aliveEntities.size());
        }
    }

    /**
     * Check whether the event is finished.
     * Create the reward chest if every entity is removed
     */
    public void checkFinish() {
        if (aliveEntities.isEmpty() && started) {
            if (raidsData.progressionBar().isEnable()) {
                raidsData.progressionBar().unload();
            }
            addChest();
            // Send end message
            sendMessage(raidsData.endMessage());
            started = false;
        }
    }

    /**
     * At the reward chest and fill it
     */
    private void addChest() {
        // Set rewards
        // Create the chest
        final World world = Objects.requireNonNull(spawnLocation.getWorld());
        final Block rewardChest = world.getHighestBlockAt(spawnLocation).getRelative(BlockFace.UP);
        rewardChest.setType(Material.CHEST);

        // Fill the chest with rewards
        final Chest chest = (Chest) rewardChest.getState();
        final Inventory inventory = chest.getBlockInventory();
        for (Map.Entry<ItemBuilder, List<Integer>> entry : raidsData.rewards().entrySet()) {
            final ItemStack itemStack = entry.getKey().create(null);
            for (int slot : entry.getValue()) {
                inventory.setItem(slot, itemStack);
            }
        }
    }

    /**
     * Remove the entities from the previous event
     */
    public void killPreviousEntities() {
        for (Entity entity : aliveEntities.values()) {
            entity.remove();
        }
        aliveEntities.clear();
    }

    /**
     * Send a message to all players
     *
     * @param rawMessage The message with placeholders
     */
    private void sendMessage(String rawMessage) {
        if (rawMessage != null) {
            final String message = rawMessage
                    .replace("%world%", raidsData.spawnData().worldAlias())
                    .replace("%x%", String.valueOf(spawnLocation.getBlockX()))
                    .replace("%z%", String.valueOf(spawnLocation.getBlockZ()));
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(message);
            }
        }
    }

}
