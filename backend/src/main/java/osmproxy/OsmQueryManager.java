package osmproxy;

import java.util.List;

// TODO: Not static, injected
class OsmQueryManager {
    static String getFullTrafficSignalQuery(List<Long> osmWayIds) {
        StringBuilder builder = new StringBuilder();
        builder.append("<osm-script>");
        for (long id : osmWayIds) {
            builder.append(getSingleTrafficSignalQuery(id));
        }
        builder.append("</osm-script>");
        return builder.toString();
    }

    static String getMultipleWayAndItsNodesQuery(List<Long> osmWayIds) {
        StringBuilder builder = new StringBuilder();
        builder.append("<osm-script>");
        for (long id : osmWayIds) {
            builder.append(getSingleWayAndItsNodesQuery(id));
        }
        builder.append("</osm-script>");
        return builder.toString();
    }

    private static String getSingleWayAndItsNodesQuery(long osmWayId) {
        return "<id-query type=\"way\" ref=\"" + osmWayId + "\" into=\"minor\"/>\r\n" +
                "  <item from=\"minor\" into=\"_\"/>\r\n" +
                "  <print e=\"\" from=\"_\" geometry=\"full\" ids=\"yes\" limit=\"\" mode=\"body\" n=\"\" order=\"id\" s=\"\" w=\"\"/>\r\n" +
                "  <recurse from=\"minor\" type=\"way-node\"/>\r\n" +
                "  <print e=\"\" from=\"_\" geometry=\"skeleton\" ids=\"yes\" limit=\"\" mode=\"tags\" n=\"\" order=\"id\" s=\"\" w=\"\"/>\r\n";
    }

    private static String getSingleTrafficSignalQuery(long osmWayId) {
        return "<osm-script>\r\n" +
                "  <id-query type=\"way\" ref=\"" + osmWayId + "\" into=\"minor\"/>\r\n" +
                "  <query into=\"_\" type=\"node\">\r\n" +
                "    <has-kv k=\"highway\" modv=\"\" v=\"traffic_signals\"/>\r\n" +
                "    <recurse from=\"minor\" type=\"way-node\"/>\r\n" +
                "  </query>\r\n" +
                "  <print e=\"\" from=\"_\" geometry=\"skeleton\" ids=\"yes\" limit=\"\" mode=\"skeleton\" n=\"\" order=\"id\" s=\"\" w=\"\"/>\r\n" +
                "  <id-query type=\"way\" ref=\"" + osmWayId + "\"/>\r\n" +
                "  <print e=\"\" from=\"_\" geometry=\"skeleton\" ids=\"yes\" limit=\"\" mode=\"skeleton\" n=\"\" order=\"id\" s=\"\" w=\"\"/>\r\n" +
                "</osm-script>";
    }


    static String getSingleBusWayOverpassQuery(long osmWayId) {
        return "<id-query type=\"way\" ref=\"" + osmWayId + "\"/>\r\n" +
                "  <print e=\"\" from=\"_\" geometry=\"full\" ids=\"yes\" limit=\"\" mode=\"skeleton\" n=\"\" order=\"id\" s=\"\" w=\"\"/>";
    }

    static String getQueryWithPayload(String query) {
        return "<osm-script>\r\n" +
                query +
                "</osm-script>";
    }

    static String getBusOverpassQuery(int radius, double middleLat, double middleLon) {
        return "<osm-script>\r\n" +
                "  <query into=\"_\" type=\"relation\">\r\n" +
                "    <has-kv k=\"route\" modv=\"\" v=\"bus\"/>\r\n" +
                "    <around radius=\"" + radius + "\" lat=\"" + middleLat + "\" lon=\"" + middleLon + "\"/>\r\n" +
                "  </query>\r\n" +
                "  <print e=\"\" from=\"_\" geometry=\"skeleton\" ids=\"yes\" limit=\"\" mode=\"body\" n=\"\" order=\"id\" s=\"\" w=\"\"/>\r\n" +
                "  <recurse type=\"relation-node\"/>\r\n" +
                "  <print e=\"\" from=\"_\" geometry=\"skeleton\" ids=\"yes\" limit=\"\" mode=\"body\" n=\"\" order=\"id\" s=\"\" w=\"\"/>\r\n" +
                "</osm-script>";
    }

    static String getLightsAroundOverpassQuery(int radius, double lat, double lon) {
        return "<osm-script>\r\n" +
                "  <query into=\"_\" type=\"node\">\r\n" +
                "    <has-kv k=\"highway\" modv=\"\" v=\"traffic_signals\"/>\r\n" +
                "    <around radius=\"" + radius + "\" lat=\"" + lat + "\" lon=\"" + lon + "\"/>\r\n" +
                "  </query>\r\n" +
                "  <print e=\"\" from=\"_\" geometry=\"skeleton\" ids=\"yes\" limit=\"\" mode=\"skeleton\" n=\"\" order=\"id\" s=\"\" w=\"\"/>\r\n" +
                "</osm-script>";
    }

    static String getSingleParentWaysOfLightOverpassQuery(final long osmLightId) {
        return "<id-query type=\"node\" ref=\"" + osmLightId + "\" into=\"crossroad\"/>\r\n" +
                "  <union into=\"_\">\r\n" +
                "    <item from=\"crossroad\" into=\"_\"/>\r\n" +
                "    <recurse from=\"crossroad\" type=\"node-way\"/>\r\n" +
                "  </union>\r\n" +
                "  <print e=\"\" from=\"_\" geometry=\"full\" ids=\"yes\" limit=\"\" mode=\"body\" n=\"\" order=\"id\" s=\"\" w=\"\"/>";
    }
}