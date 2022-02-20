package fr.flowsqy.customraids.data;

import fr.flowsqy.abstractmenu.item.ItemBuilder;
import fr.flowsqy.abstractmob.AbstractMobPlugin;
import fr.flowsqy.abstractmob.entity.EntityBuilder;

import java.util.List;
import java.util.Map;

public record RaidsData(
        AbstractMobPlugin abstractMobPlugin,
        SpawnData spawnData,
        List<EntityBuilder> raidEntities,
        Map<ItemBuilder, List<Integer>> rewards,
        String startMessage,
        String endMessage
) {
}
