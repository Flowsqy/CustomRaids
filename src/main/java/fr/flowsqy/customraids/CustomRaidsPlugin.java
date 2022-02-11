package fr.flowsqy.customraids;

import fr.flowsqy.customevents.CustomEventsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomRaidsPlugin extends JavaPlugin {

    private RaidsEvent event;

    @Override
    public void onLoad() {
        // Hook to CustomEvents
        final Plugin rawCustomEventsPlugin = Bukkit.getPluginManager().getPlugin("CustomEvents");
        if (rawCustomEventsPlugin instanceof CustomEventsPlugin customEventsPlugin) {
            customEventsPlugin.getEventManager().register(
                    "raids",
                    new RaidsDeserializer(this),
                    false
            );
        } else {
            getLogger().warning("Can not hook to CustomEvents plugin");
            getLogger().warning("Disable the plugin");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(event, this);
    }

    @Override
    public void onDisable() {
        event.killPreviousEntities();
    }

    public void setEvent(RaidsEvent event) {
        if (this.event != null) {
            throw new IllegalStateException("Can not set the event. It's already set");
        }
        this.event = event;
    }

}