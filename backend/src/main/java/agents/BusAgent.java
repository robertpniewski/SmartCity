package agents;

import agents.abstractions.AbstractAgent;
import agents.utilities.LoggerLevel;
import agents.utilities.MessageParameter;
import com.google.common.eventbus.EventBus;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.leap.Properties;
import jade.wrapper.AgentState;
import routing.RoutingConstants;
import routing.nodes.LightManagerNode;
import routing.nodes.RouteNode;
import routing.nodes.StationNode;
import smartcity.ITimeProvider;
import smartcity.SmartCityAgent;
import utilities.ConditionalExecutor;
import utilities.Siblings;
import vehicles.Bus;
import vehicles.DrivingState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import static agents.message.MessageManager.createMessage;
import static agents.message.MessageManager.createProperties;

@SuppressWarnings("serial")
public class BusAgent extends AbstractAgent {
    public static final String name = BusAgent.class.getSimpleName().replace("Agent", "");
    private final ITimeProvider timeProvider;
    private final Bus bus;

    BusAgent(int busId, Bus bus,
             ITimeProvider timeProvider,
             EventBus eventBus) {
        super(busId, name, timeProvider, eventBus);
        this.timeProvider = timeProvider;
        this.bus = bus;
    }

    @Override
    protected void setup() {
        informNextStation();
        var firstStationOpt = bus.getCurrentStationNode();
        if (firstStationOpt.isEmpty()) {
            print("No stations on route!", LoggerLevel.ERROR);
            return;
        }

        var firstStation = firstStationOpt.get();
        print("Started at station " + firstStation.getAgentId() + ".");
        bus.setState(DrivingState.MOVING);

        // TODO: Executed each x = 3600 / bus.getSpeed() = 3600m / (40 * TIME_SCALE) = 3600 / 400 = 9ms
        //   Maybe decrease the interval? - I don't think processor can keep up with all of this.
        Behaviour move = new TickerBehaviour(this, RoutingConstants.STEP_CONSTANT / bus.getSpeed()) {
            @Override
            public void onTick() {
                if (bus.isAtTrafficLights()) {
                    switch (bus.getState()) {
                        case MOVING:
                            var light = bus.getCurrentTrafficLightNode();
                            ACLMessage msg = createMessageById(ACLMessage.REQUEST_WHEN, LightManagerAgent.name,
                                    light.getLightManagerId());
                            // TODO: Should it be Vehicle?
                            Properties properties = createProperties(MessageParameter.VEHICLE);
                            properties.setProperty(MessageParameter.ADJACENT_OSM_WAY_ID, Long.toString(bus.getAdjacentOsmWayId()));
                            msg.setAllUserDefinedParameters(properties);
                            send(msg);
                            bus.setState(DrivingState.WAITING_AT_LIGHT);
                            print("Asking LightManager" + light.getLightManagerId() + " for right to passage.");

                            break;
                        case WAITING_AT_LIGHT:

                            break;
                        case PASSING_LIGHT:
                            print("Passing the light.");
                            bus.move();
                            bus.setState(DrivingState.MOVING);
                            break;
                    }
                }
                else if (bus.isAtStation()) {
                    switch (bus.getState()) {
                        case MOVING:
                            var stationOpt = bus.getCurrentStationNode();
                            if (stationOpt.isEmpty()) {
                                logger.error("Bus in not at station, but function returned that it is.");
                                return;
                            }
                            var station = stationOpt.get();
                            List<String> passengerNames = bus.getPassengers(station.getAgentId());

                            if (passengerNames.size() > 0) {
                                ACLMessage leave = createMessage(ACLMessage.REQUEST, passengerNames);
                                Properties properties = createProperties(MessageParameter.BUS);
                                properties.setProperty(MessageParameter.STATION_ID, String.valueOf(station.getAgentId()));
                                leave.setAllUserDefinedParameters(properties);
                                send(leave);
                            }

                            ACLMessage msg = createMessageById(ACLMessage.REQUEST_WHEN, StationAgent.name, station.getAgentId());
                            Properties properties = createProperties(MessageParameter.BUS);

                            var timeOnStation = bus.getTimeOnStation(station.getOsmId());
                            timeOnStation.ifPresent(time -> properties.setProperty(MessageParameter.SCHEDULE_ARRIVAL, time
                                    .toString()));
                            properties.setProperty(MessageParameter.ARRIVAL_TIME, timeProvider.getCurrentSimulationTime()
                                    .toString());
                            msg.setAllUserDefinedParameters(properties);
                            print("Send REQUEST_WHEN to station");
                            send(msg);

                            print("Arrived at station " + station.getAgentId() + ".");
                            bus.setState(DrivingState.WAITING_AT_STATION);
                            break;
                        case WAITING_AT_STATION:
                            // waiting for passengers...

                            // if you want to skip waiting (for tests) use this:
                            // bus.setState(DrivingState.PASSING_STATION);
                            break;
                        case PASSING_STATION:
                            RouteNode node = bus.findNextStop();
                            if (node instanceof LightManagerNode) {
                                informLightManager(bus);
                            }
                            informNextStation();

                            bus.setState(DrivingState.MOVING);
                            bus.move();
                            break;
                    }
                }
                else if (bus.isAtDestination()) {
                    bus.setState(DrivingState.AT_DESTINATION);
                    print("Reached destination.");

                    ACLMessage msg = createMessage(ACLMessage.INFORM, SmartCityAgent.name);
                    Properties prop = createProperties(MessageParameter.BUS);
                    prop.setProperty(MessageParameter.AT_DESTINATION, String.valueOf(Boolean.TRUE));
                    msg.setAllUserDefinedParameters(prop);
                    send(msg);
                    doDelete();
                }
                else {
                    bus.move();
                }
            }
        };

        Behaviour communication = new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage rcv = receive();
                if (rcv == null) {
                    return;
                }

                String type = rcv.getUserDefinedParameter(MessageParameter.TYPE);
                if (type == null) {
                    logTypeError(rcv);
                    return;
                }

                switch (type) {
                    case MessageParameter.LIGHT:
                        if (rcv.getPerformative() == ACLMessage.REQUEST) {
                            if (bus.getState() == DrivingState.WAITING_AT_LIGHT) {
                                ACLMessage response = createMessage(ACLMessage.AGREE, rcv.getSender());
                                // TODO: Should it be Vehicle?
                                Properties properties = createProperties(MessageParameter.VEHICLE);
                                properties.setProperty(MessageParameter.ADJACENT_OSM_WAY_ID, Long.toString(bus.getAdjacentOsmWayId()));
                                response.setAllUserDefinedParameters(properties);
                                send(response);
                                if (bus.findNextStop() instanceof LightManagerNode) {
                                    informLightManager(bus);
                                }
                                bus.setState(DrivingState.PASSING_LIGHT);
                            }
                        }
                        break;
                    case MessageParameter.STATION:
                        if (rcv.getPerformative() == ACLMessage.REQUEST) {
                            if (bus.getState() == DrivingState.WAITING_AT_STATION) {
                                ACLMessage response = createMessage(ACLMessage.AGREE, rcv.getSender());

                                Properties properties = createProperties(MessageParameter.BUS);
                                response.setAllUserDefinedParameters(properties);
                                send(response);
                                logger.info("BUS: get REQUEST from station");
                                informNextStation();
                                bus.setState(DrivingState.PASSING_STATION);
                            }
                        }
                        break;
                    case MessageParameter.PEDESTRIAN:
                        switch (rcv.getPerformative()) {
                            case ACLMessage.REQUEST_WHEN:
                                int stationId =
                                        Integer.parseInt(rcv.getUserDefinedParameter(MessageParameter.STATION_ID));
                                print("Passenger " + rcv.getSender().getLocalName() + " entered the bus.",
                                        LoggerLevel.DEBUG);
                                bus.addPassengerToStation(stationId, rcv.getSender().getLocalName());

                                ACLMessage response = createMessage(ACLMessage.AGREE, rcv.getSender());
                                Properties properties = createProperties(MessageParameter.BUS);
                                response.setAllUserDefinedParameters(properties);
                                send(response);
                                print("Passengers: " + bus.getPassengersCount(), LoggerLevel.DEBUG);
                                break;
                            case ACLMessage.AGREE:
                                stationId = Integer.parseInt(rcv.getUserDefinedParameter(MessageParameter.STATION_ID));
                                print("Passenger " + rcv.getSender().getLocalName() + " left the bus.");
                                if (bus.removePassengerFromStation(stationId, rcv.getSender().getLocalName())) {
                                    print("Passengers: " + bus.getPassengersCount());
                                }
                                else {
                                    print("Removing passenger failed");
                                }
                                break;
                        }

                        break;
                }
                block(100);
            }
        };

        addBehaviour(move);
        addBehaviour(communication);
    }

    private void informNextStation() {
        // finds next station and announces his arrival
        var stationOpt = bus.findNextStation();
        if (stationOpt.isPresent()) {
            var station = stationOpt.get();
            var stationId = station.getAgentId();
            ACLMessage msg = createMessageById(ACLMessage.INFORM, StationAgent.name, stationId);
            var properties = createProperties(MessageParameter.BUS);
            var currentTime = timeProvider.getCurrentSimulationTime();
            var predictedTime = currentTime.plusNanos(bus.getMillisecondsToNextStation() * 1_000_000);
            properties.setProperty(MessageParameter.ARRIVAL_TIME, predictedTime.toString());
            properties.setProperty(MessageParameter.BUS_LINE, bus.getLine());

            var osmId = station.getOsmId();
            var timeOnStation = bus.getTimeOnStation(osmId);
            if (timeOnStation.isPresent()) {
                properties.setProperty(MessageParameter.SCHEDULE_ARRIVAL, timeOnStation.get().toString());
            }
            else {
                print("Could not retrieve time for " + stationId, LoggerLevel.ERROR);
                ConditionalExecutor.debug(this::logAllStations);
            }

            msg.setAllUserDefinedParameters(properties);
            print("Sending INFORM to Station" + stationId);
            send(msg);
        }
    }

    private void logAllStations() {
        print("Printing station nodes: ", LoggerLevel.DEBUG);
        var stations = bus.getStationNodesOnRoute();
        for (int i = 0; i < stations.size(); ++i) {
            var station = stations.get(i);
            var osmId = station.getOsmId();
            var stationId = station.getAgentId();
            var timeOnStation = bus.getTimeOnStation(osmId);
            var timeString = timeOnStation.map(LocalDateTime::toString).orElse("");
            print(i + ": [" + osmId + "][" + stationId + "] on '" + timeString + "'", LoggerLevel.DEBUG);
        }
        print("Printing station nodes finished.", LoggerLevel.DEBUG);
    }

    public Bus getBus() {
        return bus;
    }

    public final String getLine() {
        return bus.getLine();
    }

    // TODO: Fix situation where bus route contains only one station and pedestrians tries to choose two
    public final Siblings<StationNode> getTwoSubsequentStations(final Random random) {
        List<StationNode> stationsOnRoute = bus.getStationNodesOnRoute();
        final int halfIndex = stationsOnRoute.size() / 2;
        return Siblings.of(stationsOnRoute.get(random.nextInt(halfIndex)),
                stationsOnRoute.get(halfIndex + random.nextInt(halfIndex)));
    }

    /**
     * @return If busAgent finished execution
     */
    public boolean runBasedOnTimetable() {
        var state = this.getAgentState().getValue();
        if (state != AgentState.cAGENT_STATE_INITIATED) {
            if (state == AgentState.cAGENT_STATE_ACTIVE && bus.isAtDestination()) {
                print("Stopping!");
                return true;
            }
        }

        if (shouldStart()) {
            print("Running!");
            start();
        }

        return false;
    }

    public boolean shouldStart(){
        return bus.shouldStart();
    }
}
