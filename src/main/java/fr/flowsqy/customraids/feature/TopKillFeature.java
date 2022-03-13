package fr.flowsqy.customraids.feature;

import fr.flowsqy.customraids.CustomRaidsPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class TopKillFeature extends ZonedFeature implements Listener {

    public final static TopKillFeature NULL = new TopKillFeature(false, null, 0, null, null);

    private final CustomRaidsPlugin plugin;
    private final String message;
    private final ChatMessageType messageType;
    private final Map<UUID, Integer> playerKills;

    public TopKillFeature(boolean enable, CustomRaidsPlugin plugin, int radius, String message, ChatMessageType messageType) {
        super(enable, radius);
        this.plugin = plugin;
        this.message = message;
        this.messageType = messageType;
        playerKills = new HashMap<>();
    }


    /**
     * Load the kill counter for a {@link fr.flowsqy.customraids.RaidsEvent}
     */
    public void load() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Unload the kill counter
     */
    public void unload() {
        HandlerList.unregisterAll(this);
        playerKills.clear();
    }

    /**
     * Tell that a raid entity died
     * It works as an entry point
     *
     * @param entityDeathEvent The {@link EntityDeathEvent} that represents the entity death
     */
    public void entityDied(EntityDeathEvent entityDeathEvent) {
        final LivingEntity killer = entityDeathEvent.getEntity().getKiller();
        if (killer instanceof Player player) {
            final int kills = playerKills.getOrDefault(player.getUniqueId(), 0);
            playerKills.put(player.getUniqueId(), kills + 1);
        }
    }

    /**
     * Get the {@link java.util.Map.Entry} of the top killer of the current {@link fr.flowsqy.customraids.RaidsEvent}
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
     * Send the top kill message
     *
     * @param world   The {@link World} where the event is
     * @param xCenter The x coordinate of the location where the {@link fr.flowsqy.customraids.RaidsEvent} take place
     * @param zCenter The z coordinate of the location where the {@link fr.flowsqy.customraids.RaidsEvent} take place
     */
    public void sendMessage(World world, int xCenter, int zCenter) {
        if (message == null) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final Map.Entry<UUID, Integer> topKillEntry = getTopKiller();
            if (topKillEntry == null) {
                return;
            }

            final OfflinePlayer topKiller = Bukkit.getOfflinePlayer(topKillEntry.getKey());
            final String playerName = topKiller.getName();
            Objects.requireNonNull(playerName, "The name of the top killer is null");

            final BaseComponent[] messageComponent = TextComponent.fromLegacyText(message
                    .replace("%count%", String.valueOf(topKillEntry.getValue()))
                    .replace("%player%", playerName)
            );
            for (Player player : calculateViewers(world, xCenter, zCenter)) {
                player.spigot().sendMessage(messageType, messageComponent);
            }
        });
    }

}
