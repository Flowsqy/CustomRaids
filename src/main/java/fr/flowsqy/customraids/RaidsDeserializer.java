package fr.flowsqy.customraids;

import fr.flowsqy.customevents.api.Event;
import fr.flowsqy.customevents.api.EventDeserializer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.logging.Logger;

public class RaidsDeserializer implements EventDeserializer {

    @Override
    public Event deserialize(ConfigurationSection section, Logger logger, String fileName) {
        return null;
    }

}
