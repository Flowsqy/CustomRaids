package fr.flowsqy.customraids.data;

public record SpawnData(
        String worldName,
        String worldAlias,
        int spawnRadius,
        int minSpawnRadius
) {
}
