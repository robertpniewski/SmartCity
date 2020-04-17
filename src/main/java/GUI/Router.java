package GUI;

import SmartCity.MapAccessManager;

import org.javatuples.Pair;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.CompoundPainter;
import org.jxmapviewer.viewer.GeoPosition;

import com.graphhopper.util.PointList;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class Router {
    public static RouteInfo generateRouteInfo(GeoPosition pointA, GeoPosition pointB) {
        Pair<List<Long>, PointList> osmWayIdsAndPointList = findRoute(pointA, pointB);
        List<OSMNode> lights_list = MapAccessManager.sendTrafficSignalOverpassQuery(osmWayIdsAndPointList.getValue0());
        return new RouteInfo(osmWayIdsAndPointList.getValue1(), new LinkedHashSet<>(lights_list));
    }

    private static Pair<List<Long>, PointList> findRoute(GeoPosition pointA, GeoPosition pointB) {
        Pair<List<Long>, PointList> osmWayIdsAndPointList = com.graphhopper.osmidexample.HighwayAccessor.getOsmWayIdsAndPointList(new String[]{"config=config.properties", "datareader.file=mazowieckie-latest.osm.pbf"},
                pointA.getLatitude(), pointA.getLongitude(), pointB.getLatitude(), pointB.getLongitude());
        return osmWayIdsAndPointList;
    }
}