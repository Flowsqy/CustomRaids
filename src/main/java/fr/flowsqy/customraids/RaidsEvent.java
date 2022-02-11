package fr.flowsqy.customraids;

import fr.flowsqy.abstractmenu.item.ItemBuilder;
import fr.flowsqy.abstractmob.entity.EntityBuilder;
import fr.flowsqy.customevents.api.Event;

import java.util.List;
import java.util.Map;

public class RaidsEvent implements Event {

    private final String worldName;
    private final String worldAlias;
    private final List<EntityBuilder> raidEntities;
    private final Map<ItemBuilder, List<Integer>> rewards;
    private final String startMessage;
    private final String endMessage;
    private final String processingMessage;

    public RaidsEvent(
            String worldName,
            String worldAlias,
            List<EntityBuilder> raidEntities,
            Map<ItemBuilder, List<Integer>> rewards,
            String startMessage,
            String endMessage,
            String processingMessage
    ) {
        this.worldName = worldName;
        this.worldAlias = worldAlias;
        this.raidEntities = raidEntities;
        this.rewards = rewards;
        this.startMessage = startMessage;
        this.endMessage = endMessage;
        this.processingMessage = processingMessage;
    }

    @Override
    public void perform() {

    }


}
