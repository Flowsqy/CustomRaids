package fr.flowsqy.customraids;

import fr.flowsqy.abstractmenu.item.ItemBuilder;
import fr.flowsqy.abstractmob.AbstractMobPlugin;
import fr.flowsqy.abstractmob.entity.EntityBuilder;
import fr.flowsqy.abstractmob.entity.EntityBuilderSerializer;
import fr.flowsqy.customevents.api.Event;
import fr.flowsqy.customevents.api.EventData;
import fr.flowsqy.customevents.api.EventDeserializer;
import fr.flowsqy.customraids.data.RaidsData;
import fr.flowsqy.customraids.data.SpawnData;
import fr.flowsqy.customraids.progressionbar.ProgressionBar;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class RaidsDeserializer implements EventDeserializer {

    private final CustomRaidsPlugin customRaidsPlugin;
    private final List<RaidsEvent> raidsEvents;

    public RaidsDeserializer(CustomRaidsPlugin customRaidsPlugin, List<RaidsEvent> raidsEvents) {
        this.customRaidsPlugin = customRaidsPlugin;
        this.raidsEvents = raidsEvents;
    }

    /**
     * Get an enum constant from its name
     *
     * @param enumClass    The enum class
     * @param constantName The name of the enum constant
     * @param defaultValue The default value that will be returned if there is no constant matching the given name
     * @param <T>          The enum type
     * @return The found enum constant or the default value if there is constant matching the given name
     */
    private static <T extends Enum<T>> T getEnumConstant(Class<T> enumClass, String constantName, T defaultValue) {
        if (constantName == null) {
            return defaultValue;
        }
        for (T constant : enumClass.getEnumConstants()) {
            if (constant.name().equals(constantName)) {
                return constant;
            }
        }
        return defaultValue;
    }

    @Override
    public Event deserialize(ConfigurationSection section, Logger logger, String fileName, EventData eventData) {
        // Get AbstractMob instance
        final Plugin rawAbstractMobPlugin = Bukkit.getPluginManager().getPlugin("AbstractMob");
        if (rawAbstractMobPlugin instanceof AbstractMobPlugin abstractMobPlugin) {
            // World
            final ConfigurationSection worldSection = section.getConfigurationSection("world");
            if (worldSection == null) {
                logger.warning("Null world section in " + fileName);
                return null;
            }
            final String worldName = worldSection.getString("name");
            if (worldName == null || worldName.isBlank()) {
                logger.warning("Empty world name in " + fileName);
                return null;
            }
            final String worldAlias = getMessage(worldSection, "alias");
            final int spawnRadius = worldSection.getInt("radius", -1);
            if (spawnRadius < 0) {
                logger.warning("Invalid radius (< 0) in " + fileName);
                return null;
            }
            final int minSpawnRadius = Math.max(worldSection.getInt("min-radius", 0), 0);
            if (minSpawnRadius >= spawnRadius) {
                logger.warning("Invalid min radius, it is greater than the radius");
                return null;
            }

            // Entities
            final List<EntityBuilder> raidEntities = new LinkedList<>();
            final ConfigurationSection entitiesSection = section.getConfigurationSection("entities");
            if (entitiesSection == null) {
                logger.warning("Null entities section in " + fileName);
                return null;
            }
            for (String keySubSection : entitiesSection.getKeys(false)) {
                final ConfigurationSection entitySection = entitiesSection.getConfigurationSection(keySubSection);
                if (entitySection == null) {
                    continue;
                }
                final EntityBuilder builder = EntityBuilderSerializer.deserialize(abstractMobPlugin, entitySection);
                if (builder != null) {
                    raidEntities.add(builder);
                }
            }
            if (raidEntities.isEmpty()) {
                logger.warning("No entity for the raids in " + fileName);
                return null;
            }


            // Rewards
            final Map<ItemBuilder, List<Integer>> rewards = new HashMap<>();

            final ConfigurationSection rewardSection = section.getConfigurationSection("rewards");
            if (rewardSection == null) {
                logger.warning("Null rewards section in " + fileName);
                return null;
            }
            for (String keySubSection : rewardSection.getKeys(false)) {
                final ConfigurationSection slotSection = rewardSection.getConfigurationSection(keySubSection);
                if (slotSection == null) {
                    continue;
                }
                final List<Integer> rawSlots = slotSection.getIntegerList("slots");
                final ConfigurationSection itemSection = slotSection.getConfigurationSection("item");

                final ItemBuilder builder = itemSection == null ? null : ItemBuilder.deserialize(itemSection);

                if (builder != null) {
                    final List<Integer> slots = rawSlots.stream().filter(slot -> slot < 9 * 3).toList();
                    if (!slots.isEmpty()) {
                        rewards.put(builder, slots);
                    }
                }
            }

            // ProgressionBar
            final ConfigurationSection barSection = section.getConfigurationSection("progression-bar");
            final ProgressionBar progressionBar;
            if (barSection != null && barSection.getBoolean("enable")) {
                final String title = barSection.getString("title");
                progressionBar = new ProgressionBar(
                        true,
                        customRaidsPlugin,
                        getEnumConstant(BarColor.class, barSection.getString("color"), BarColor.RED),
                        barSection.getInt("radius", -1),
                        title == null ? null : ChatColor.translateAlternateColorCodes('&', title)
                );
            } else {
                progressionBar = ProgressionBar.NULL;
            }

            // Messages
            final ConfigurationSection messageSection = section.getConfigurationSection("messages");
            if (messageSection == null) {
                logger.warning("Null message section in " + fileName);
                return null;
            }
            final String startMessage = getMessage(messageSection, "start");
            final String endMessage = getMessage(messageSection, "end");

            final RaidsEvent event = new RaidsEvent(
                    eventData,
                    new RaidsData(
                            abstractMobPlugin,
                            new SpawnData(
                                    worldName,
                                    worldAlias,
                                    spawnRadius,
                                    minSpawnRadius
                            ),
                            raidEntities,
                            progressionBar,
                            rewards,
                            startMessage,
                            endMessage
                    )
            );
            Bukkit.getPluginManager().registerEvents(event, customRaidsPlugin);
            raidsEvents.add(event);
            return event;
        } else {
            logger.warning("Can not get the AbstractMob instance");
            return null;
        }
    }

    /**
     * Deserialize a message and set the color
     *
     * @param messageSection The message section
     * @param path           The path of the message
     * @return the message stored in the configuration with colors, null if it does not exist
     */
    private String getMessage(ConfigurationSection messageSection, String path) {
        final String message = messageSection.getString(path);
        return message == null ? null : ChatColor.translateAlternateColorCodes('&', message);
    }

}
