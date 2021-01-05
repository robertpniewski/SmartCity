package osmproxy.abstractions;

import osmproxy.buses.data.BusPreparationData;
import osmproxy.elements.OSMWay;
import routing.nodes.RouteNode;
import routing.nodes.StationNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
//TODO:dokumentacja

public interface ICacheWrapper {
    Optional<BusPreparationData> getBusPreparationData();

    void cacheData(BusPreparationData data);

    ArrayList<RouteNode> getBusRoute(List<OSMWay> route,
                                     List<StationNode> stationNodes);

    void cacheData(List<OSMWay> route, List<StationNode> stationNodes, ArrayList<RouteNode> data);
}
