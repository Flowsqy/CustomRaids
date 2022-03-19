package fr.flowsqy.customraids;

import fr.flowsqy.abstractmenu.item.ItemBuilder;
import fr.flowsqy.abstractmob.entity.EntityBuilder;
import fr.flowsqy.customevents.api.Event;
import fr.flowsqy.customevents.api.EventData;
import fr.flowsqy.customraids.data.FeaturesData;
import fr.flowsqy.customraids.data.RaidsData;
import fr.flowsqy.customraids.data.SpawnData;
import fr.flowsqy.customraids.feature.ProgressionFeature;
import fr.flowsqy.customraids.feature.TopKillFeature;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTransformEvent;
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
        unloadFeatures();

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

        // Load features
        final FeaturesData featuresData = raidsData.featuresData();

        // Load the progression bar
        final ProgressionFeature progression = featuresData.progression();
        if (progression.isEnable()) {
            progression.load(
                    world,
                    spawnLocation.getBlockX(),
                    spawnLocation.getBlockZ(),
                    aliveEntities.size()
            );
        }

        // Load top killer counter
        final TopKillFeature topKillFeature = featuresData.topKill();
        if (topKillFeature.isEnable()) {
            topKillFeature.load(
                    world,
                    spawnLocation.getBlockX(),
                    spawnLocation.getBlockZ()
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

            refreshProgression();
            checkFinish();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onDeath(EntityDeathEvent event) {
        detectDeath(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onExplode(EntityExplodeEvent event) {
        detectDeath(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onTransform(EntityTransformEvent event) {
        // Check if the transformed entity is used in this event
        if (aliveEntities.remove(event.getEntity().getUniqueId()) != null) {
            // Add new entities
            final List<Entity> transformedEntities = event.getTransformedEntities();
            for (Entity newEntity : transformedEntities) {
                aliveEntities.put(newEntity.getUniqueId(), newEntity);
            }

            // Modify the progression bar and refresh it
            final ProgressionFeature progressionFeature = raidsData.featuresData().progression();
            if (progressionFeature.isEnable() && transformedEntities.size() != 1) {
                progressionFeature.modifyMaxEntity(transformedEntities.size() - 1);
                progressionFeature.refresh(aliveEntities.size());
            }
        }
    }

    /**
     * Detect a death in the raid {@link Entity} list
     *
     * @param entity The entity to check
     */
    private void detectDeath(Entity entity) {
        // Check if the dying entity is one of the raid entity
        if (aliveEntities.remove(entity.getUniqueId()) != null) {
            refreshProgression();

            // Actualize the kill counters
            final TopKillFeature topKillFeature = raidsData.featuresData().topKill();
            if (topKillFeature.isEnable() && entity instanceof LivingEntity livingEntity) {
                topKillFeature.entityDied(livingEntity);
            }

            checkFinish();
        }
    }

    /**
     * Refresh the progression if needed
     */
    private void refreshProgression() {
        final ProgressionFeature progression = raidsData.featuresData().progression();
        if (progression.isEnable()) {
            progression.refresh(aliveEntities.size());
        }
    }

    /**
     * Check whether the event is finished.
     * Create the reward chest if every entity is removed
     */
    public void checkFinish() {
        if (aliveEntities.isEmpty() && started) {
            // Send the top killer message
            final TopKillFeature topKillFeature = raidsData.featuresData().topKill();
            if (topKillFeature.isEnable()) {
                topKillFeature.sendMessage(spawnLocation.getWorld(), spawnLocation.getBlockX(), spawnLocation.getBlockZ());
            }

            addChest();
            // Send end message
            sendMessage(raidsData.endMessage());
            unloadFeatures();
            started = false;
        }
    }

    /**
     * Unload the features loaded for this event
     */
    public void unloadFeatures() {
        final FeaturesData featuresData = raidsData.featuresData();

        // Unload progression features
        final ProgressionFeature progression = featuresData.progression();
        if (progression.isEnable()) {
            progression.unload();
        }

        // Unload the top killer feature
        final TopKillFeature topKillFeature = featuresData.topKill();
        if (topKillFeature.isEnable()) {
            topKillFeature.unload();
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
