package agents.abstractions;

import agents.*;
import org.w3c.dom.Node;

import osmproxy.buses.BusInfo;
import osmproxy.buses.Timetable;
import osmproxy.elements.OSMNode;
import osmproxy.elements.OSMStation;
import routing.nodes.RouteNode;
import routing.nodes.StationNode;

import java.util.HashSet;
import java.util.List;

public interface IAgentsFactory {
    CarAgent create(List<RouteNode> route, boolean testCar);

    CarAgent create(List<RouteNode> route);

    BikeAgent create(List<RouteNode> route, boolean testCar, String check);

    BikeAgent create(List<RouteNode> route, String check);

    StationAgent create(OSMStation station);

	BusManagerAgent create(HashSet<BusInfo> busInfos);

    BusAgent create(List<RouteNode> route, Timetable timetable, String busLine,
                    String brigadeNr);

    @Deprecated
    LightManagerAgent create(Node crossroad);

    LightManagerAgent create(OSMNode centerCrossroad);

    PedestrianAgent create(List<RouteNode> routeToStation, List<RouteNode> routeFromStation,
                          StationNode startStation, StationNode finishStation,
                           boolean testPedestrian);

    PedestrianAgent create(List<RouteNode> routeToStation, List<RouteNode> routeFromStation,
                           StationNode startStation, StationNode finishStation);
}
