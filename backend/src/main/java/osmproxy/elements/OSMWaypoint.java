package osmproxy.elements;

import osmproxy.MapAccessManager;
import org.jxmapviewer.viewer.GeoPosition;

public class OSMWaypoint {
    private final String osmNodeRef;
    private final GeoPosition geoPos;

    public OSMWaypoint(String nodeRef, double lat, double lng) {
        osmNodeRef = nodeRef;
        geoPos = new GeoPosition(lat, lng);
    }

    public final String getOsmNodeRef() {
        return osmNodeRef;
    }

    public final double getLat() {
        return geoPos.getLatitude();
    }

    public final double getLon() {
        return geoPos.getLongitude();
    }

    public final GeoPosition getPosition() {
        return geoPos;
    }

    boolean containedInCircle(int radius, double middleLat, double middleLon) {
        return MapAccessManager.belongsToCircle(geoPos.getLatitude(), geoPos.getLongitude(), new GeoPosition(middleLat, middleLon), radius);
    }
}