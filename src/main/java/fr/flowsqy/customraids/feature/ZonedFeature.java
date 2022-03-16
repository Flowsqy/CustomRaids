package fr.flowsqy.customraids.feature;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class ZonedFeature extends Feature {

    protected final int radius;

    public ZonedFeature(boolean enable, int radius) {
        super(enable);
        this.radius = radius;
    }

    /**
     * Calculate which players must use the feature
     *
     * @param world   The {@link World} where the event is
     * @param xCenter The x coordinate of the location where the {@link fr.flowsqy.customraids.RaidsEvent} take place
     * @param zCenter The z coordinate of the location where the {@link fr.flowsqy.customraids.RaidsEvent} take place
     */
    protected Set<Player> calculateViewers(World world, int xCenter, int zCenter) {
        return calculateViewers(world, xCenter, zCenter, radius);
    }

    /**
     * Calculate which players must use the feature
     *
     * @param world   The {@link World} where the event is
     * @param xCenter The x coordinate of the location where the {@link fr.flowsqy.customraids.RaidsEvent} take place
     * @param zCenter The z coordinate of the location where the {@link fr.flowsqy.customraids.RaidsEvent} take place
     * @param radius  The radius of the circle where players must use the feature.
     *                A radius of 0 take the whole world player list and a negative radius every players on the server
     */
    protected Set<Player> calculateViewers(World world, int xCenter, int zCenter, int radius) {
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

        return players;
    }

}
