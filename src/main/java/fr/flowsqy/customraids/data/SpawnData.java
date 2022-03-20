package fr.flowsqy.customraids.data;

import org.bukkit.block.Biome;

import java.util.Set;

public record SpawnData(
        String worldName,
        String worldAlias,
        int spawnRadius,
        int minSpawnRadius,
        Set<Biome> cancelledBiomes
) {
}
