package fr.flowsqy.customraids.feature;

import fr.flowsqy.customraids.CustomRaidsPlugin;
import fr.flowsqy.customraids.RaidsEvent;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class TimerFeature extends Feature {

    public final static TimerFeature NULL = new TimerFeature(
            false,
            null,
            -1,
            null
    );

    private final CustomRaidsPlugin plugin;
    private final long timer;
    private final String message;
    private boolean loaded;
    private BukkitTask task;

    public TimerFeature(boolean enable, CustomRaidsPlugin plugin, long timer, String message) {
        super(enable);
        this.plugin = plugin;
        this.timer = timer;
        this.message = message;
    }


    public void load(RaidsEvent raidsEvent) {
        if (loaded) {
            throw new IllegalStateException("This timer feature is already loaded");
        }

        final Runnable runnable = () -> {
            raidsEvent.sendMessage(message);

            // Run it on another instance to prevent it to be auto cancelled
            Bukkit.getScheduler().runTask(plugin, raidsEvent::forceEnd);
        };

        task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, timer * 20L);
        loaded = true;
    }

    public void unload() {
        if (!loaded) {
            return;
        }
        if (task != null) {
            task.cancel();
            task = null;
        }
        loaded = false;
    }
}
