package fr.flowsqy.customraids;

import fr.flowsqy.abstractmenu.item.ItemBuilder;
import fr.flowsqy.abstractmob.AbstractMobPlugin;
import fr.flowsqy.abstractmob.entity.EntityBuilder;

import java.util.List;
import java.util.Map;

public record RaidsData(
        AbstractMobPlugin abstractMobPlugin,
        String worldName,
        String worldAlias,
        int spawnRadius,
        List<EntityBuilder> raidEntities,
        Map<ItemBuilder, List<Integer>> rewards,
        String startMessage,
        String endMessage
) {
}
