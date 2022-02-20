package fr.flowsqy.customraids;

import fr.flowsqy.customevents.CustomEventsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedList;
import java.util.List;

public class CustomRaidsPlugin extends JavaPlugin {

    private final List<RaidsEvent> raidsEvents;

    public CustomRaidsPlugin() {
        this.raidsEvents = new LinkedList<>();
    }

    @Override
    public void onEnable() {
        // Hook to CustomEvents
        final Plugin rawCustomEventsPlugin = Bukkit.getPluginManager().getPlugin("CustomEvents");
        if (rawCustomEventsPlugin instanceof CustomEventsPlugin customEventsPlugin) {
            customEventsPlugin.getEventManager().register(
                    "raids",
                    new RaidsDeserializer(this, raidsEvents),
                    false
            );
        } else {
            getLogger().warning("Can not hook to CustomEvents plugin");
            getLogger().warning("Disable the plugin");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        for (RaidsEvent event : raidsEvents) {
            event.killPreviousEntities();
        }
    }

}