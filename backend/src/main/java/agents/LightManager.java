package agents;

import behaviourfactories.IBehaviourFactory;
import behaviourfactories.LightManagerBehaviourFactory;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import osmproxy.elements.OSMNode;
import smartcity.lights.ICrossroad;
import smartcity.lights.SimpleCrossroad;

import java.util.List;

public class LightManager extends AbstractAgent {
    private static final Logger logger = LoggerFactory.getLogger(LightManager.class);
    // TODO: Inject it as dependency
    private final IBehaviourFactory<LightManager> behaviourFactory;
    private final ICrossroad crossroad;

    public LightManager(Node node, int id) {
        super(id);
        behaviourFactory = new LightManagerBehaviourFactory();
        crossroad = new SimpleCrossroad(node, id);
    }

    public LightManager(OSMNode centerCrossroadNode, int id) {
        super(id);
        behaviourFactory = new LightManagerBehaviourFactory();
        crossroad = new SimpleCrossroad(centerCrossroadNode, id);
    }

    @Override
    public String getNamePrefix() {
        return "LightManager";
    }

    @Override
    protected void setup() {
        print("I'm a traffic manager.");
        crossroad.startLifetime();
        addBehaviour(behaviourFactory.createCyclicBehaviour(this));
        addBehaviour(behaviourFactory.createTickerBehaviour(this));
    }

    public ICrossroad getCrossroad() {
        return crossroad;
    }

    public List<GeoPosition> getLightsPositions() {
        return crossroad.getLightsPositions();
    }

    public void print(String message) {
        logger.info(getLocalName() + ": " + message);
    }

    public void draw(List<Painter<JXMapViewer>> waypointPainter) {
        crossroad.draw(waypointPainter);
    }
}