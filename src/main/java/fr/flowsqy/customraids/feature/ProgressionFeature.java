package fr.flowsqy.customraids.feature;

import fr.flowsqy.customraids.CustomRaidsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;

public class ProgressionFeature extends ZonedFeature implements Listener {

    public final static ProgressionFeature NULL = new ProgressionFeature(false, null, 0, null, null);

    private final CustomRaidsPlugin plugin;
    private final BarColor color;
    private final String title;
    private boolean loaded;
    private int maxEntity, xCenter, zCenter;
    private BukkitTask task;
    private BossBar bossBar;

    public ProgressionFeature(boolean enable, CustomRaidsPlugin plugin, int radius, BarColor color, String title) {
        super(enable, radius);
        this.plugin = plugin;
        this.color = color;
        this.title = title;
    }

    /**
     * Load the bar for a {@link fr.flowsqy.customraids.RaidsEvent}
     *
     * @param world     The {@link World} where the event is
     * @param xCenter   The x coordinate of the location where the {@link fr.flowsqy.customraids.RaidsEvent} take place
     * @param zCenter   The z coordinate of the location where the {@link fr.flowsqy.customraids.RaidsEvent} take place
     * @param maxEntity The number of entity to kill to end the {@link fr.flowsqy.customraids.RaidsEvent}
     */
    public void load(World world, int xCenter, int zCenter, int maxEntity) {
        if (loaded) {
            throw new IllegalStateException("This progression feature is already loaded");
        }
        bossBar = Bukkit.createBossBar(title, color, BarStyle.SOLID);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                () -> refreshViewers(world, xCenter, zCenter),
                0L,
                20L
        );
        this.maxEntity = maxEntity;
        this.xCenter = xCenter;
        this.zCenter = zCenter;
        formatTitle(maxEntity);
        loaded = true;
    }

    /**
     * Unload the bar
     */
    public void unload() {
        if (!loaded) {
            return;
        }
        HandlerList.unregisterAll(this);
        task.cancel();
        task = null;
        this.maxEntity = 0;
        bossBar.setVisible(false);
        for (Player player : bossBar.getPlayers()) {
            bossBar.removePlayer(player);
        }
        bossBar = null;
        loaded = false;
    }

    /**
     * Refresh the list of player that must see the boss bar
     *
     * @param world   The {@link World} where the event is
     * @param xCenter The x coordinate of the location where the {@link fr.flowsqy.customraids.RaidsEvent} take place
     * @param zCenter The z coordinate of the location where the {@link fr.flowsqy.customraids.RaidsEvent} take place
     */
    private void refreshViewers(World world, int xCenter, int zCenter) {
        final Set<Player> players = calculateViewers(world, xCenter, zCenter);

        // Remove old viewers
        for (Player player : bossBar.getPlayers()) {
            if (!players.remove(player)) {
                bossBar.removePlayer(player);
            }
        }

        // Add new viewers
        for (Player player : players) {
            bossBar.addPlayer(player);
        }
    }

    /**
     * Refresh the bar progression
     *
     * @param remainingEntity The number of remaining entity
     */
    public void refresh(int remainingEntity) {
        bossBar.setProgress((double) remainingEntity / maxEntity);
        formatTitle(remainingEntity);
    }

    /**
     * Modify the current number original entities
     *
     * @param modifier The modifier to apply. It will be added to the current number
     */
    public void modifyMaxEntity(int modifier) {
        this.maxEntity += modifier;
    }

    /**
     * Refresh the {@link BossBar} title
     *
     * @param remainingEntity The count of remaining entity
     */
    private void formatTitle(int remainingEntity) {
        if (title != null) {
            // Get the new title
            final double proportion = (double) remainingEntity / maxEntity;
            final String formattedTitle = title
                    .replace("%x%", String.valueOf(xCenter))
                    .replace("%z%", String.valueOf(zCenter))
                    .replace("%max_entities%", String.valueOf(maxEntity))
                    .replace("%entities%", String.valueOf(remainingEntity))
                    .replace("%proportion%", String.valueOf(proportion))
                    .replace("%percentage%", String.valueOf((int) (proportion * 100)));

            // Send it if necessary
            if (!formattedTitle.equals(title)) {
                bossBar.setTitle(formattedTitle);
            }
        }
    }


}
