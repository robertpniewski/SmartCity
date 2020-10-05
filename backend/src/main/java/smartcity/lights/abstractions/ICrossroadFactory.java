package smartcity.lights.abstractions;

import org.w3c.dom.Node;
import osmproxy.elements.OSMNode;

public interface ICrossroadFactory {
    ICrossroad create(Node crossroad);

    ICrossroad create(OSMNode centerCrossroadNode);
}