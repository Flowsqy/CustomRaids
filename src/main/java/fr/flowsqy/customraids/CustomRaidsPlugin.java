package fr.flowsqy.customraids;

import fr.flowsqy.abstractmob.AbstractMobPlugin;
import fr.flowsqy.customevents.CustomEventsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomRaidsPlugin extends JavaPlugin {

    private AbstractMobPlugin abstractMobPlugin;

    @Override
    public void onLoad() {
        // Hook to AbstractMob
        // Theoretically can be in onEnable but must be before CustomEvents hook that need to be in onLoad
        final Plugin rawAbstractMobPlugin = Bukkit.getPluginManager().getPlugin("AbstractMob");
        if (rawAbstractMobPlugin instanceof AbstractMobPlugin abstractMobPlugin) {
            this.abstractMobPlugin = abstractMobPlugin;
        } else {
            getLogger().warning("Can not hook to AbstractMob plugin");
            getLogger().warning("Disable the plugin");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        // Hook to CustomEvents
        final Plugin rawCustomEventsPlugin = Bukkit.getPluginManager().getPlugin("CustomEvents");
        if (rawCustomEventsPlugin instanceof CustomEventsPlugin customEventsPlugin) {
            customEventsPlugin.getEventManager().register(
                    "raids",
                    new RaidsDeserializer(),
                    false
            );
        } else {
            getLogger().warning("Can not hook to CustomEvents plugin");
            getLogger().warning("Disable the plugin");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

}