package smartcity.lights.core;

import agents.utilities.LightColor;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import org.w3c.dom.Node;
import osmproxy.elements.OSMNode;
import smartcity.lights.abstractions.ICrossroad;
import smartcity.lights.abstractions.ICrossroadFactory;
import utilities.Siblings;

import java.util.List;

public class CrossroadFactory implements ICrossroadFactory {
    private final EventBus eventBus;
    private final ICrossroadParser ICrossroadParser;

    @Inject
    public CrossroadFactory(EventBus eventBus,
                            ICrossroadParser ICrossroadParser) {
        this.eventBus = eventBus;
        this.ICrossroadParser = ICrossroadParser;
    }

    @Override
    public ICrossroad create(Node crossroad) {
        var lightGroups = getLightGroups(crossroad);
        return create(lightGroups);
    }

    @Override
    public ICrossroad create(OSMNode centerCrossroadNode) {
        var lightGroups = getLightGroups(centerCrossroadNode);
        return create(lightGroups);
    }

    private ICrossroad create(Siblings<SimpleLightGroup> lightGroups) {
        return new SimpleCrossroad(eventBus, lightGroups);
    }

    private Siblings<SimpleLightGroup> getLightGroups(Node crossroad) {
        var lightGroups = ICrossroadParser.getLightGroups(crossroad);
        return getLightGroups(lightGroups.first, lightGroups.second);
    }

    private Siblings<SimpleLightGroup> getLightGroups(List<LightInfo> groupA, List<LightInfo> groupB) {
        var lightGroupA = new SimpleLightGroup(groupA, LightColor.RED);
        var lightGroupB = new SimpleLightGroup(groupB, LightColor.GREEN);

        return Siblings.of(lightGroupA, lightGroupB);
    }

    private Siblings<SimpleLightGroup> getLightGroups(OSMNode centerCrossroadNode) {
        var lightGroups = ICrossroadParser.getLightGroups(centerCrossroadNode);
        return getLightGroups(lightGroups.first, lightGroups.second);
    }
}