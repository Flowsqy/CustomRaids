package fr.flowsqy.customraids;

import fr.flowsqy.abstractmenu.item.ItemBuilder;
import fr.flowsqy.abstractmob.AbstractMobPlugin;
import fr.flowsqy.abstractmob.entity.EntityBuilder;
import fr.flowsqy.customevents.api.Event;
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

public class RaidsEvent implements Event, Listener {

    private final AbstractMobPlugin abstractMobPlugin;
    private final String worldName;
    private final String worldAlias;
    private final int spawnRadius;
    private final List<EntityBuilder> raidEntities;
    private final Map<ItemBuilder, List<Integer>> rewards;
    private final String startMessage;
    private final String endMessage;

    private final Set<Entity> aliveEntities;
    private Location spawnLocation;

    public RaidsEvent(
            CustomRaidsPlugin customRaidsPlugin,
            AbstractMobPlugin abstractMobPlugin,
            String worldName,
            String worldAlias,
            int spawnRadius,
            List<EntityBuilder> raidEntities,
            Map<ItemBuilder, List<Integer>> rewards,
            String startMessage,
            String endMessage
    ) {
        this.abstractMobPlugin = abstractMobPlugin;
        this.worldName = worldName;
        this.worldAlias = worldAlias;
        this.spawnRadius = spawnRadius;
        this.raidEntities = raidEntities;
        this.rewards = rewards;
        this.startMessage = startMessage;
        this.endMessage = endMessage;

        this.aliveEntities = new HashSet<>();

        customRaidsPlugin.setEvent(this);
    }

    @Override
    public void perform() {
        // End previous event
        killPreviousEntities();

        // Start the new one
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return;
        }

        final Random random = new Random();
        final int doubledRadius = spawnRadius * 2;
        final Vector vector = new Vector(random.nextInt(doubledRadius) - spawnRadius, 0, random.nextInt(doubledRadius) - spawnRadius);
        vector.normalize().multiply((double) spawnRadius * random.nextDouble());
        spawnLocation = new Location(world, 0, 0, 0).add(vector);

        for (EntityBuilder entityBuilder : raidEntities) {
            final List<Entity> spawnedEntities = entityBuilder.spawn(
                    abstractMobPlugin,
                    spawnLocation,
                    entityBuilder.getRadius(),
                    true
            );
            aliveEntities.addAll(spawnedEntities);
        }

        if (startMessage != null) {
            final String message = startMessage
                    .replace("%world%", worldAlias)
                    .replace("%x%", String.valueOf(spawnLocation.getBlockX()))
                    .replace("%z%", String.valueOf(spawnLocation.getBlockZ()));
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(message);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        if (aliveEntities.remove(event.getEntity())) {
            // Check if the event is finished
            if (aliveEntities.isEmpty()) {
                final World world = Objects.requireNonNull(spawnLocation.getWorld());
                final Block rewardChest = world.getHighestBlockAt(spawnLocation).getRelative(BlockFace.UP);
                rewardChest.setType(Material.CHEST);
                final Chest chest = (Chest) rewardChest.getState();
                final Inventory inventory = chest.getBlockInventory();
                for (Map.Entry<ItemBuilder, List<Integer>> entry : rewards.entrySet()) {
                    final ItemStack itemStack = entry.getKey().create(null);
                    for (int slot : entry.getValue()) {
                        inventory.setItem(slot, itemStack);
                    }
                }
                if (endMessage != null) {
                    final String message = endMessage
                            .replace("%world%", worldAlias)
                            .replace("%x%", String.valueOf(spawnLocation.getBlockX()))
                            .replace("%z%", String.valueOf(spawnLocation.getBlockZ()));
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(message);
                    }
                }
            }
        }
    }

    public void killPreviousEntities() {
        for (Entity entity : aliveEntities) {
            entity.remove();
        }
    }

}
