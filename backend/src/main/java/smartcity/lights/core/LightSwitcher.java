package smartcity.lights.core;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import events.LightSwitcherStartedEvent;
import events.web.SwitchLightsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartcity.ITimeProvider;
import smartcity.config.abstractions.ILightConfigContainer;
import smartcity.task.data.ISwitchLightsContext;
import utilities.Siblings;

import java.util.function.Function;

public class LightSwitcher implements Function<ISwitchLightsContext, Integer> {
    private static final int CAR_TO_PEDESTRIAN_LIGHT_RATE = 2;
    private final Logger logger;

    private final ITimeProvider timeProvider;
    private final ILightConfigContainer configContainer;
    private final EventBus eventBus;

    private final int extendTimeSeconds;
    private final int defaultExecutionDelay;

    private Long lightOsmId;
    private SimpleLightGroup greenLights;
    private SimpleLightGroup redLights;

    @Inject
    LightSwitcher(ITimeProvider timeProvider,
                  ILightConfigContainer configContainer,
                  EventBus eventBus,
                  @Assisted("managerId") int managerId,
                  @Assisted("extendTime") int extendTimeSeconds,
                  @Assisted Siblings<SimpleLightGroup> lights) {
        this.timeProvider = timeProvider;
        this.configContainer = configContainer;
        this.eventBus = eventBus;

        this.extendTimeSeconds = extendTimeSeconds;
        this.defaultExecutionDelay = extendTimeSeconds * 1000 / timeProvider.getTimeScale();
        setLights(lights);

        this.logger = LoggerFactory.getLogger("LightSwitcher" + managerId);
        eventBus.post(new LightSwitcherStartedEvent(managerId, defaultExecutionDelay));
    }

    private void setLights(Siblings<SimpleLightGroup> lights) {
        var firstIter = lights.first.iterator();
        var secondIter = lights.second.iterator();

        Light anyLight;
        boolean isFirstGroupGreen;
        if (firstIter.hasNext()) {
            anyLight = firstIter.next();
            isFirstGroupGreen = anyLight.isGreen();
        }
        else if (secondIter.hasNext()) {
            anyLight = secondIter.next();
            isFirstGroupGreen = !anyLight.isGreen();
        }
        else {
            throw new IllegalArgumentException("Both groups does not have any lights");
        }

        if (isFirstGroupGreen) {
            this.greenLights = lights.first;
            this.redLights = lights.second;
        }
        else {
            this.redLights = lights.first;
            this.greenLights = lights.second;
        }

        this.lightOsmId = anyLight.getOsmLightId();
    }

    @Override
    public Integer apply(ISwitchLightsContext context) {
        if (shouldExtend(!context.haveNotExtendedYet())) {
            context.setNotExtendedGreen(false);
            return defaultExecutionDelay;
        }

        context.setNotExtendedGreen(true);
        switchLights();

        return defaultExecutionDelay;
    }

    private boolean shouldExtend(boolean hadPreviouslyExtended) {
        if (configContainer.isLightStrategyActive()) {

            // TODO: Get group sizes and ensure that red is 0 when going to next condition
            if (shouldExtendGreenLightBecauseOfObjectsOnLight(hadPreviouslyExtended)) {
                logger.debug("-------------------------------------shouldExtendGreenLightBecauseOfCarsOnLight--------------");
                return true;
            }
            else if (shouldExtendBecauseOfFarAwayQueue(hadPreviouslyExtended)) {
                logger.debug("-------------------------------------shouldExtendBecauseOfFarAwayQueue--------------");
                return true;
            }
        }

        return false;
    }


    private boolean shouldExtendGreenLightBecauseOfObjectsOnLight(boolean hadPreviouslyExtended) {
        int greenGroupObjects = 0;
        int redGroupObjects = 0;

        for (Light light : greenLights) {
            redGroupObjects += light.pedestrianQueue.size();
            greenGroupObjects += CAR_TO_PEDESTRIAN_LIGHT_RATE * light.carQueue.size();
        }
        // Reverse is intended - each group is opposite to one another
        for (Light light : redLights) {
            greenGroupObjects += light.pedestrianQueue.size();
            redGroupObjects += CAR_TO_PEDESTRIAN_LIGHT_RATE * light.carQueue.size();
        }

        boolean result;
        if (hadPreviouslyExtended) {
            result = greenGroupObjects > 0 && redGroupObjects == 0;
        }
        else {
            result = greenGroupObjects > redGroupObjects;
        }


        if (result) {
            logger.debug("LM:CROSSROAD HAS PROLONGED GREEN LIGHT FOR " + greenGroupObjects + " CARS AS OPPOSED TO " + redGroupObjects);
        }

        return result;
    }


    private boolean shouldExtendBecauseOfFarAwayQueue(boolean hadPreviouslyExtended) {
        var currentTime = timeProvider.getCurrentSimulationTime();
        var currentTimePlusExtend = currentTime.plusSeconds(extendTimeSeconds);

        int greenGroupObjects = 0;
        int redGroupObjects = 0;
        for (Light light : greenLights) {
            for (var time : light.farAwayCarMap.values()) {
                if (time.isAfter(currentTime) && time.isBefore(currentTimePlusExtend)) {
                    greenGroupObjects += CAR_TO_PEDESTRIAN_LIGHT_RATE;
                }
            }
            for (var time : light.farAwayPedestrianMap.values()) {
                if (time.isAfter(currentTime) && time.isBefore(currentTimePlusExtend)) {
                    ++redGroupObjects;
                }
            }
        }

        for (Light light : redLights) {
            // TODO: getFarawayCarsAndPedestriansWithinInterval
            for (var time : light.farAwayPedestrianMap.values()) {
                if (time.isAfter(currentTime) && time.isBefore(currentTimePlusExtend)) {
                    ++greenGroupObjects;
                }
            }
            for (var time : light.farAwayCarMap.values()) {
                if (time.isAfter(currentTime) && time.isBefore(currentTimePlusExtend)) {
                    redGroupObjects += CAR_TO_PEDESTRIAN_LIGHT_RATE;
                }
            }
        }

        boolean result;
        if (hadPreviouslyExtended) {
            result = greenGroupObjects > 0 && redGroupObjects == 0;
        }
        else {
            result = greenGroupObjects > redGroupObjects;
        }

        return result;
    }

    private void switchLights() {
        // TODO: Can include yellow somehow?
        greenLights.switchLights();
        redLights.switchLights();

        var tmp = greenLights;
        greenLights = redLights;
        redLights = tmp;

        logger.debug("Switched light at: " + timeProvider.getCurrentSimulationTime());
        eventBus.post(new SwitchLightsEvent(lightOsmId));
    }
}