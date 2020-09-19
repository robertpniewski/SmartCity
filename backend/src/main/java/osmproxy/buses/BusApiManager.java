package osmproxy.buses;

import com.google.inject.Inject;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import osmproxy.OsmQueryManager;
import osmproxy.abstractions.IMapAccessManager;
import osmproxy.buses.abstractions.IBusApiManager;
import routing.core.IZone;
import utilities.ConditionalExecutor;
import utilities.FileWriterWrapper;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class BusApiManager implements IBusApiManager {
    private static final Logger logger = LoggerFactory.getLogger(BusApiManager.class);
    private static final JSONParser jsonParser = new JSONParser();

    private final IMapAccessManager mapAccessManager;

    @Inject
    public BusApiManager(IMapAccessManager mapAccessManager) {
        this.mapAccessManager = mapAccessManager;
    }

    @Override
    public Optional<Document> getBusDataXml(IZone zone) {
        var query = OsmQueryManager.getBusQuery(zone.getCenter(), zone.getRadius());
        var overpassInfo = mapAccessManager.getNodesDocument(query);

        ConditionalExecutor.debug(() -> {
            overpassInfo.ifPresent(info -> {
                logger.info("Writing bus-data to: " + FileWriterWrapper.DEFAULT_OUTPUT_PATH_XML);
                FileWriterWrapper.write(info);
            });
        });

        return overpassInfo;
    }

    @Override
    public Optional<JSONObject> getNodesViaWarszawskieAPI(String busStopId, String busStopNr, String busLine) {
        var query = getBusWarszawskieQuery(busStopId, busStopNr, busLine);
        JSONObject jObject;
        try {
            URL url = new URL(query);
            Scanner scanner = new Scanner(url.openStream());
            StringBuilder jsonBuilder = new StringBuilder();
            while (scanner.hasNext()) {
                jsonBuilder.append(scanner.nextLine());
            }
            jObject = (JSONObject) jsonParser.parse(jsonBuilder.toString());
        } catch (Exception e) {
            logger.warn("Error trying to get 'Warszawskie busy'", e);
            return Optional.empty();
        }

        ConditionalExecutor.debug(() -> {
            String path = "target/brigade" + busStopId + ".json";
            logger.info("Writing bus-brigade-date to: " + path);
            FileWriterWrapper.write(jObject, path);
        });

        return Optional.of(jObject);
    }

    @Override
    public Optional<Document> getBusWays(List<Long> waysIds) {
        var query = buildWaysQuery(waysIds);
        var resultOpt = mapAccessManager.getNodesDocument(query);

        ConditionalExecutor.debug(() -> {
            resultOpt.ifPresent(result -> {
                String path = "target/busWays_" + waysIds.get(0) + "_" +
                        waysIds.get(waysIds.size() - 1) + ".xml";
                logger.info("Writing bus-ways to: " + path);
                FileWriterWrapper.write(result, path);
            });
        });

        return resultOpt;
    }

    private String buildWaysQuery(List<Long> waysIds) {
        StringBuilder busWayQueryBuilder = new StringBuilder();
        for (var id : waysIds) {
            busWayQueryBuilder.append(OsmQueryManager.getSingleBusWayQuery(id));
        }

        return OsmQueryManager.getQueryWithPayload(busWayQueryBuilder.toString());
    }

    private static String getBusWarszawskieQuery(String busStopId, String busStopNr, String busLine) {
        return "https://api.um.warszawa.pl/api/action/dbtimetable_get/?id=e923fa0e-d96c-43f9-ae6e-60518c9f3238&busstopId=" +
                busStopId + "&busstopNr=" + busStopNr + "&line=" + busLine + "&apikey=400dacf8-9cc4-4d6c-82cc-88d9311401a5";
    }
}
