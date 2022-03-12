package fr.flowsqy.customraids.progressionbar;

import fr.flowsqy.customraids.CustomRaidsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;

public class ProgressionBar implements Listener {

    public final static ProgressionBar NULL = new ProgressionBar(false, null, null, 0, null);

    private final boolean enable;
    private final CustomRaidsPlugin plugin;
    private final BarColor color;
    private final int radius;
    private final String title;
    private int maxEntity;
    private BukkitTask task;
    private BossBar bossBar;

    public ProgressionBar(boolean enable, CustomRaidsPlugin plugin, BarColor color, int radius, String title) {
        this.enable = enable;
        this.plugin = plugin;
        this.color = color;
        this.radius = radius;
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
        bossBar = Bukkit.createBossBar(title, color, BarStyle.SOLID);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                () -> calculateViewers(world, xCenter, zCenter),
                0L,
                20L
        );
        this.maxEntity = maxEntity;
    }

    /**
     * Unload the bar
     */
    public void unload() {
        HandlerList.unregisterAll(this);
        task.cancel();
        task = null;
        this.maxEntity = 0;
        bossBar.setVisible(false);
        for (Player player : bossBar.getPlayers()) {
            bossBar.removePlayer(player);
        }
        bossBar = null;
    }

    /**
     * Refresh the list of player that must see the boss bar
     *
     * @param world   The {@link World} where the event is
     * @param xCenter The x coordinate of the location where the {@link fr.flowsqy.customraids.RaidsEvent} take place
     * @param zCenter The z coordinate of the location where the {@link fr.flowsqy.customraids.RaidsEvent} take place
     */
    private void calculateViewers(World world, int xCenter, int zCenter) {
        // Get all players that must see the boss bar
        final Set<Player> players;
        if (radius > 0) {
            players = new HashSet<>();
            final int squaredRadius = radius * radius;
            for (Player player : world.getPlayers()) {
                final Location pLoc = player.getLocation();
                final int xDistance = pLoc.getBlockX() - xCenter;
                final int zDistance = pLoc.getBlockZ() - zCenter;
                final int squaredDistance = xDistance * xDistance + zDistance * zDistance;
                if (squaredDistance < squaredRadius) {
                    players.add(player);
                }
            }
        } else if (radius == 0) {
            players = new HashSet<>(world.getPlayers());
        } else {
            players = new HashSet<>(Bukkit.getOnlinePlayers());
        }

        // Refresh the bar viewers

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
     * Check if the bar is enabled
     *
     * @return Whether the bar should be used
     */
    public boolean isEnable() {
        return enable;
    }

    /**
     * Refresh the bar progression
     *
     * @param remainingEntity The number of remaining entity
     */
    public void refresh(double remainingEntity) {
        bossBar.setProgress(remainingEntity / maxEntity);
    }

}
