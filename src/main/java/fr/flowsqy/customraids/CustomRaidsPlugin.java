package fr.flowsqy.customraids;

import fr.flowsqy.customevents.CustomEventsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomRaidsPlugin extends JavaPlugin {

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

}