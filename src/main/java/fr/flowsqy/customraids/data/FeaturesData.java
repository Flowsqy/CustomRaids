package fr.flowsqy.customraids.data;

import fr.flowsqy.customraids.feature.ProgressionFeature;
import fr.flowsqy.customraids.feature.TopKillFeature;

public record FeaturesData(
        ProgressionFeature progression,
        TopKillFeature topKill
) {
}
