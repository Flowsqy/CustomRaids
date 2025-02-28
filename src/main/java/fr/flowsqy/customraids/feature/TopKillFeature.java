package fr.flowsqy.customraids.feature;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import fr.flowsqy.customraids.CustomRaidsPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public class TopKillFeature extends ZonedFeature implements Listener {

    public final static TopKillFeature NULL = new TopKillFeature(
            false,
            null,
            0,
            null,
            null,
            null,
            0);

    private final CustomRaidsPlugin plugin;
    private final String message;
    private final ChatMessageType messageType;
    private final String permanentMessage;
    private final int permanentRadius;
    private final Map<UUID, Integer> playerKills;
    private boolean loaded;
    private Runnable taskRunnable;
    private BukkitTask task;

    public TopKillFeature(
            boolean enable,
            CustomRaidsPlugin plugin,
            int radius,
            String message,
            ChatMessageType messageType,
            String permanentMessage,
            int permanentRadius) {
        super(enable, radius);
        this.plugin = plugin;
        this.message = message;
        this.messageType = messageType;
        this.permanentMessage = permanentMessage;
        this.permanentRadius = permanentRadius;
        playerKills = new ConcurrentHashMap<>();
    }

    /**
     * Whether the feature is unused
     *
     * @return {@code true} if the feature is unused.
     */
    private boolean isUnused() {
        return message == null && permanentMessage == null;
    }

    /**
     * Load the kill counter for a {@link fr.flowsqy.customraids.RaidsEvent}
     */
    public void load(World world, int xCenter, int zCenter) {
        if (loaded) {
            throw new IllegalStateException("This top killer feature is already loaded");
        }
        // Don't load the feature if the feature is unused
        if (isUnused()) {
            return;
        }

        // Create a task for the permanent message
        if (permanentMessage != null) {
            taskRunnable = () -> {
                final Map.Entry<UUID, Integer> topKillEntry = getTopKiller();
                if (topKillEntry == null) {
                    return;
                }

                sendActionBarMessage(
                        topKillEntry,
                        world, xCenter, zCenter,
                        ChatMessageType.ACTION_BAR,
                        permanentMessage,
                        permanentRadius);
            };
            task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, taskRunnable, 0L, 40L);
            // 40L Is the maximum amount of ticks before the message start to shade itself
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
        loaded = true;
    }

    /**
     * Unload the kill counter
     */
    public void unload() {
        if (!loaded) {
            return;
        }
        // If the messages are null, the feature is not loaded
        if (isUnused()) {
            return;
        }
        if (task != null) {
            task.cancel();
            task = null;
        }
        HandlerList.unregisterAll(this);
        playerKills.clear();
        loaded = false;
    }

    /**
     * Tell that a raid entity died
     *
     * @param entity The dead raid {@link LivingEntity}
     */
    public void entityDied(LivingEntity entity) {
        // Don't store kill counts if the messages is null
        if (isUnused()) {
            return;
        }

        // Increment a kill count if the kill is made by a player
        final LivingEntity killer = entity.getKiller();
        if (killer instanceof Player player) {
            final int kills = playerKills.getOrDefault(player.getUniqueId(), 0);
            playerKills.put(player.getUniqueId(), kills + 1);

            // If the message is permanent, actualize it
            if (taskRunnable != null) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, taskRunnable);
            }
        }
    }

    /**
     * Get the {@link java.util.Map.Entry} of the top killer of the current
     * {@link fr.flowsqy.customraids.RaidsEvent}
     *
     * @return The {@link java.util.Map.Entry} of the current top killer
     */
    private Map.Entry<UUID, Integer> getTopKiller() {
        Map.Entry<UUID, Integer> topKillerEntry = null;
        for (Map.Entry<UUID, Integer> entry : playerKills.entrySet()) {
            if (topKillerEntry == null || topKillerEntry.getValue() < entry.getValue()) {
                topKillerEntry = entry;
            }
        }
        return topKillerEntry;
    }

    /**
     * Send the top kill message if needed
     *
     * @param world   The {@link World} where the event is
     * @param xCenter The x coordinate of the location where the
     *                {@link fr.flowsqy.customraids.RaidsEvent} take place
     * @param zCenter The z coordinate of the location where the
     *                {@link fr.flowsqy.customraids.RaidsEvent} take place
     */
    public void sendMessage(World world, int xCenter, int zCenter) {
        // Nothing to do if the message is null
        if (message == null) {
            return;
        }

        // Get the top killer before async task because the feature is unloaded before
        // the async task is performed
        final Map.Entry<UUID, Integer> topKillEntry = getTopKiller();
        if (topKillEntry == null) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(
                plugin,
                () -> sendActionBarMessage(
                        topKillEntry,
                        world, xCenter, zCenter,
                        messageType,
                        message,
                        radius));
    }

    /**
     * Send the top kill action bar message
     *
     * @param topKillEntry The {@link java.util.Map.Entry} with the top killer
     *                     {@link UUID} and kill count
     * @param world        The {@link World} where the event is
     * @param xCenter      The x coordinate of the location where the
     *                     {@link fr.flowsqy.customraids.RaidsEvent} take place
     * @param zCenter      The z coordinate of the location where the
     *                     {@link fr.flowsqy.customraids.RaidsEvent} take place
     * @param messageType  The {@link ChatMessageType} type of the message to send
     * @param message      The message to send
     * @param radius       The radius to use
     */
    private void sendActionBarMessage(
            Map.Entry<UUID, Integer> topKillEntry,
            World world,
            int xCenter,
            int zCenter,
            ChatMessageType messageType,
            String message,
            int radius) {
        final OfflinePlayer topKiller = Bukkit.getOfflinePlayer(topKillEntry.getKey());
        final String playerName = topKiller.getName();
        Objects.requireNonNull(playerName, "The name of the top killer is null");

        final BaseComponent messageComponent = TextComponent.fromLegacy(message
                .replace("%count%", String.valueOf(topKillEntry.getValue()))
                .replace("%player%", playerName));
        for (Player player : calculateViewers(world, xCenter, zCenter, radius)) {
            player.spigot().sendMessage(messageType, messageComponent);
        }
    }

}
