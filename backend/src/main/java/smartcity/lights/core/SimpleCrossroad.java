package smartcity.lights.core;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import events.LightSwitcherStartedEvent;
import events.SwitchLightsStartEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartcity.config.abstractions.ITroublePointsConfigContainer;
import smartcity.lights.OptimizationResult;
import smartcity.lights.abstractions.ICrossroad;
import smartcity.stations.ArrivalInfo;
import utilities.Siblings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class SimpleCrossroad implements ICrossroad {
    private final Logger logger;
    private final EventBus eventBus;
    private final ITroublePointsConfigContainer configContainer;
    private final int managerId;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Map<Long, Light> wayIdToLightMap;
    private int defaultExecutionDelay = -1;

    SimpleCrossroad(EventBus eventBus,
                    ITroublePointsConfigContainer configContainer,
                    int managerId,
                    Siblings<SimpleLightGroup> lightGroups) {
        this.logger = LoggerFactory.getLogger("SimpleCrossroad" + managerId);
        this.eventBus = eventBus;
        this.configContainer = configContainer;
        this.managerId = managerId;
        this.wayIdToLightMap = new HashMap<>() {{
            putAll(lightGroups.first.prepareMap());
            putAll(lightGroups.second.prepareMap());
        }};
    }

    @Override
    public void startLifetime() {
        eventBus.post(new SwitchLightsStartEvent(managerId, wayIdToLightMap.values()));
    }

    @Subscribe
    public void handle(LightSwitcherStartedEvent e) {
        if (e.managerId == this.managerId) {
            this.defaultExecutionDelay = e.defaultExecutionDelay;
            eventBus.unregister(this);
        }
    }

    @Override
    public OptimizationResult requestOptimizations(int extendTimeSeconds) {
        if (defaultExecutionDelay < 0) {
            logger.warn("Light switcher did not start yet.");
            return OptimizationResult.empty();
        }

        final OptimizationResult result = new OptimizationResult(extendTimeSeconds, defaultExecutionDelay);
        boolean shouldCheckForJams = configContainer.shouldChangeRouteOnTrafficJam();
        for (Light light : wayIdToLightMap.values()) {

            if (shouldCheckForJams) {
                light.checkForTrafficJams(result);
            }

            if (light.isGreen()) {
                for (String carName : light.carQueue) {
                    result.addCarGrantedPassthrough(carName);
                    break;
                }
            }
            else {
                for (String pedestrianName : light.pedestrianQueue) {
                    result.addCarGrantedPassthrough(pedestrianName);
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public List<Light> getLights() {
        return new ArrayList<>(wayIdToLightMap.values());
    }

    private boolean tryConsume(long adjacentWayId, Consumer<Light> consumer) {
        var light = wayIdToLightMap.get(adjacentWayId);
        if (light == null) {
            logAddError(adjacentWayId);
            return false;
        }

        consumer.accept(light);
        return true;
    }

    private void logAddError(long adjacentWayId) {
        logger.warn("Failed to get adjacentWayId: " + adjacentWayId + "\n" + wayIdToLightMap.entrySet().stream()
                .map(entry -> entry.getKey() + ", " + entry.getValue().getOsmLightId())
                .collect(Collectors.joining("\n")));
    }

    @Override
    public boolean addCarToQueue(long adjacentWayId, String agentName) {
        return tryConsume(adjacentWayId, l -> l.addCarToQueue(agentName));
    }

    @Override
    public boolean removeCarFromQueue(long adjacentWayId) {
        return tryConsume(adjacentWayId, Light::removeCarFromQueue);
    }

    @Override
    public boolean addCarToFarAwayQueue(long adjacentWayId, ArrivalInfo arrivalInfo) {
        return tryConsume(adjacentWayId, l -> l.addCarToFarAwayQueue(arrivalInfo));
    }

    @Override
    public boolean removeCarFromFarAwayQueue(long adjacentWayId, String agentName) {
        return tryConsume(adjacentWayId, light -> light.removeCarFromFarAwayQueue(agentName));
    }

    @Override
    public boolean addPedestrianToQueue(long adjacentWayId, String agentName) {
        return tryConsume(adjacentWayId, light -> light.addPedestrianToQueue(agentName));
    }

    @Override
    public boolean addPedestrianToFarAwayQueue(long adjacentWayId, ArrivalInfo arrivalInfo) {
        return tryConsume(adjacentWayId, light -> light.addPedestrianToFarAwayQueue(arrivalInfo));
    }

    @Override
    public boolean removePedestrianFromQueue(long adjacentWayId) {
        return tryConsume(adjacentWayId, Light::removePedestrianFromQueue);
    }

    @Override
    public boolean removePedestrianFromFarAwayQueue(long adjacentWayId, String agentName) {
        return tryConsume(adjacentWayId, l -> l.removePedestrianFromFarAwayQueue(agentName));
    }
}
