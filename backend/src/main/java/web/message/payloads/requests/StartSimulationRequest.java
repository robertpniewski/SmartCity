package web.message.payloads.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import web.message.payloads.AbstractPayload;

import java.time.ZonedDateTime;

public class StartSimulationRequest extends AbstractPayload {
    public final int carsLimit;
    public final int testCarId;
    public final boolean generateCars;
    public final boolean generateTroublePoints;
    public final ZonedDateTime startTime;
    public final boolean lightStrategyActive;
    public final int extendLightTime;
    public final boolean stationStrategyActive;
    public final int extendWaitTime;
    public final boolean changeRouteStrategyActive;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public StartSimulationRequest(@JsonProperty("carsLimit") int carsLimit,
                                  @JsonProperty("testCarId") int testCarId,
                                  @JsonProperty("generateCars") boolean generateCars,
                                  @JsonProperty("generateTroublePoints") boolean generateTroublePoints,
                                  @JsonProperty("startTime") ZonedDateTime startTime,
                                  @JsonProperty("lightStrategyActive") boolean lightStrategyActive,
                                  @JsonProperty("extendLightTime") int extendLightTime,
                                  @JsonProperty("stationStrategyActive") boolean stationStrategyActive,
                                  @JsonProperty("extendWaitTime") int extendWaitTime,
                                  @JsonProperty("changeRouteStrategyActive") boolean changeRouteStrategyActive) {

        this.carsLimit = carsLimit;
        this.testCarId = testCarId;
        this.generateCars = generateCars;
        this.generateTroublePoints = generateTroublePoints;
        this.startTime = startTime;
        this.lightStrategyActive = lightStrategyActive;
        this.extendLightTime = extendLightTime;
        this.stationStrategyActive = stationStrategyActive;
        this.extendWaitTime = extendWaitTime;
        this.changeRouteStrategyActive = changeRouteStrategyActive;
    }
}
