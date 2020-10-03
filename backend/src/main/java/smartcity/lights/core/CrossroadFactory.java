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
    private final CrossroadParser crossroadParser;

    @Inject
    public CrossroadFactory(EventBus eventBus,
                            CrossroadParser crossroadParser) {
        this.eventBus = eventBus;
        this.crossroadParser = crossroadParser;
    }

    @Override
    public ICrossroad create(Node crossroad, int managerId) {
        var lightGroups = getLightGroups(crossroad, managerId);
        return create(lightGroups);
    }

    @Override
    public ICrossroad create(OSMNode centerCrossroadNode, int managerId) {
        var lightGroups = getLightGroups(centerCrossroadNode, managerId);
        return create(lightGroups);
    }

    private ICrossroad create(Siblings<SimpleLightGroup> lightGroups) {
        return new SimpleCrossroad(eventBus, lightGroups);
    }

    private Siblings<SimpleLightGroup> getLightGroups(Node crossroad, int managerId) {
        var lightGroups = crossroadParser.getLightGroups(crossroad);
        return getLightGroups(lightGroups.first, lightGroups.second, managerId);
    }

    private Siblings<SimpleLightGroup> getLightGroups(List<LightInfo> groupA, List<LightInfo> groupB, int managerId){
        var lightGroupA = new SimpleLightGroup(groupA, LightColor.RED, managerId);
        var lightGroupB = new SimpleLightGroup(groupB, LightColor.GREEN, managerId);

        return Siblings.of(lightGroupA, lightGroupB);
    }

    private Siblings<SimpleLightGroup> getLightGroups(OSMNode centerCrossroadNode, int managerId) {
        var lightGroups = crossroadParser.getLightGroups(centerCrossroadNode);
        return getLightGroups(lightGroups.first, lightGroups.second, managerId);
    }
}
