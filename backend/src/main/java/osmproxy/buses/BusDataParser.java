package osmproxy.buses;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import osmproxy.buses.abstractions.IApiSerializer;
import osmproxy.buses.abstractions.IBusApiManager;
import osmproxy.buses.abstractions.IBusDataParser;
import osmproxy.buses.abstractions.IDataMerger;
import osmproxy.buses.data.BusInfoData;
import osmproxy.buses.data.BusPreparationData;
import osmproxy.elements.OSMStation;
import osmproxy.elements.OSMWay;
import routing.core.IZone;
import routing.core.Position;
import utilities.IterableNodeList;
import utilities.Siblings;

import java.util.*;
import java.util.function.Predicate;

public class BusDataParser implements IBusDataParser {
    private static final Logger logger = LoggerFactory.getLogger(BusDataParser.class);

    private final IDataMerger busDataMerger;
    private final IApiSerializer apiSerializer;
    private final IBusApiManager busApiManager;
    private final IZone zone;

    @Inject
    BusDataParser(IDataMerger busDataMerger,
                  IApiSerializer apiSerializer, IBusApiManager busApiManager,
                  IZone zone) {
        this.busDataMerger = busDataMerger;
        this.apiSerializer = apiSerializer;
        this.busApiManager = busApiManager;
        this.zone = zone;
    }

    // TODO: Add tests for this function
    @Override
    public BusPreparationData parseBusData(Document busData) {
        Node osmRoot = busData.getFirstChild();
        var osmXMLNodes = IterableNodeList.of(osmRoot.getChildNodes());

        Set<BusInfoData> busInfoDataSet = new LinkedHashSet<>();
        HashMap<Long, OSMStation> busStopsMap = new LinkedHashMap<>();
        int errors = 0;
        for (var osmNode : osmXMLNodes) {
            var nodeName = osmNode.getNodeName();
            if (nodeName.equals("relation")) {
                var busInfo = parseRelation(osmNode);
                if (busInfo.isEmpty()) {
                    if (++errors < 5) {
                        continue;
                    }
                    throw new RuntimeException("Too much errors when parsing busInfo");
                }
                busInfoDataSet.add(busInfo.get());
            }
            else if (nodeName.equals("node")) {
                var station = parseNode(osmNode, busStopsMap::containsKey);
                station.ifPresent(st -> busStopsMap.put(st.getId(), st));
            }
        }

        var busInfos = busDataMerger.getBusInfosWithStops(busInfoDataSet, busStopsMap);

        return new BusPreparationData(busInfos, busStopsMap);
    }

    private Optional<BusInfoData> parseRelation(Node relation) {
        List<Long> stationIds = new ArrayList<>();
        String busLine = "";
        List<Long> wayIds = new ArrayList<>();
        for (var node : IterableNodeList.of(relation.getChildNodes())) {
            if (node.getNodeName().equals("member")) {
                NamedNodeMap attributes = node.getAttributes();
                long id = Long.parseLong(attributes.getNamedItem("ref").getNodeValue());
                if (attributes.getNamedItem("role").getNodeValue().contains("stop") &&
                        attributes.getNamedItem("type").getNodeValue().equals("node")) {
                    stationIds.add(id);
                }
                else if (attributes.getNamedItem("role").getNodeValue().length() == 0 &&
                        attributes.getNamedItem("type").getNodeValue().equals("way")) {
                    wayIds.add(id);
                }
            }
            else if (node.getNodeName().equals("tag")) {
                NamedNodeMap attributes = node.getAttributes();
                Node namedItemID = attributes.getNamedItem("k");
                if (namedItemID.getNodeValue().equals("ref")) {
                    Node lineNumber = attributes.getNamedItem("v");
                    busLine = lineNumber.getNodeValue();
                }
            }
        }

        var waysDoc = busApiManager.getBusWays(wayIds);
        if (waysDoc.isEmpty()) {
            return Optional.empty();
        }
        List<OSMWay> ways = parseOsmWays(waysDoc.get());

        return Optional.of(new BusInfoData(new BusInfo(busLine, ways), stationIds));
    }

    @SuppressWarnings("FeatureEnvy")
    @Override
    public List<OSMWay> parseOsmWays(Document nodes) {
        List<OSMWay> route = new ArrayList<>();
        Node osmRoot = nodes.getFirstChild();
        var osmNodes = IterableNodeList.of(osmRoot.getChildNodes());
        var nodesIter = osmNodes.iterator();
        var twoFirstWaysOpt = findTwoFirstWaysInZone(nodesIter);
        if (twoFirstWaysOpt.isEmpty()) {
            logger.error("Didn't find two connected ways in provided zone");
            return route;
        }

        var twoFirstWays = twoFirstWaysOpt.get();
        var firstWay = twoFirstWays.first;
        var secondWay = twoFirstWays.second;

        route.add(firstWay);
        route.add(secondWay);
        String adjacentNodeRef = firstWay.orientateWith(secondWay);
        while (nodesIter.hasNext()) {
            Node item = nodesIter.next();
            if (item.getNodeName().equals("way")) {
                OSMWay way = new OSMWay(item);
                // TODO: CORRECT POTENTIAL BUGS CAUSING ROUTE TO BE CUT INTO PIECES BECAUSE OF RZĄŻEWSKI CASE
                if (way.startsInZone(zone)) {
                    var referenceOpt = way.reverseTowardsNode(adjacentNodeRef);
                    if (referenceOpt.isEmpty()) {
                        logger.warn("Failed to match way:\n" + way + " with " + adjacentNodeRef);
                        continue;
                    }

                    adjacentNodeRef = referenceOpt.get();
                    route.add(way);
                }
            }
        }

        return route;
    }

    private Optional<Siblings<OSMWay>> findTwoFirstWaysInZone(Iterator<Node> nodeIterator) {
        OSMWay firstWay = null;
        OSMWay secondWay = null;
        while (nodeIterator.hasNext() && secondWay == null) {
            Node item = nodeIterator.next();
            if (item.getNodeName().equals("way")) {
                OSMWay way = new OSMWay(item);
                if (firstWay == null) {
                    if (way.isInZone(zone)) {
                        firstWay = way;
                    }
                }
                else if (firstWay.isConnectedTo(way)) {
                    secondWay = way;
                }
            }
        }

        if (secondWay == null) {
            return Optional.empty();
        }

        return Optional.of(new Siblings<>(firstWay, secondWay));
    }

    private Optional<OSMStation> parseNode(Node node, Predicate<Long> isPresent) {
        NamedNodeMap attributes = node.getAttributes();
        long osmId = Long.parseLong(attributes.getNamedItem("id").getNodeValue());
        double lat = Double.parseDouble(attributes.getNamedItem("lat").getNodeValue());
        double lon = Double.parseDouble(attributes.getNamedItem("lon").getNodeValue());

        boolean isPresentVal = isPresent.test(osmId);
        if (!isPresentVal && zone.contains(Position.of(lat, lon))) {
            var stationNumber = searchForStationNumber(node.getChildNodes());
            if (stationNumber.isPresent()) {
                logger.debug("Parsing station with number: " + stationNumber.get());
                return Optional.of(new OSMStation(osmId, lat, lon, stationNumber.get()));
            }
        }

        logger.debug("Station: " + osmId + " won't be included. IsPresent: " + isPresentVal);

        return Optional.empty();
    }

    private Optional<String> searchForStationNumber(NodeList nodes) {
        return IterableNodeList.of(nodes)
                .stream()
                .filter(n -> n.getNodeName().equals("tag"))
                .map(Node::getAttributes)
                .dropWhile(attr -> !attr.getNamedItem("k").getNodeValue().equals("public_transport"))
                .filter(attr -> attr.getNamedItem("k").getNodeValue().equals("ref"))
                .findFirst()
                .map(attr -> attr.getNamedItem("v").getNodeValue());
    }
}
