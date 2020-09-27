package osmproxy.buses.abstractions;

import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import routing.core.IZone;

import java.util.List;
import java.util.Optional;

public interface IBusApiManager {
    Optional<Document> getBusDataXml(IZone zone);

    Optional<JSONObject> getNodesViaWarszawskieAPI(String busStopId, String busStopNr, String busLine);

    Optional<Document> getBusWays(List<Long> waysIds);
}