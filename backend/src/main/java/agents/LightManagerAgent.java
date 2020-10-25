package agents;

import agents.abstractions.AbstractAgent;
import agents.utilities.MessageParameter;
import com.google.common.eventbus.EventBus;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.leap.Properties;
import org.javatuples.Pair;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import smartcity.ITimeProvider;
import smartcity.TimeProvider;
import smartcity.lights.OptimizationResult;
import smartcity.lights.abstractions.ICrossroad;
import smartcity.lights.core.Light;
import smartcity.stations.ArrivalInfo;

import java.util.List;

import static agents.message.MessageManager.*;
import static java.lang.Thread.sleep;

public class LightManagerAgent extends AbstractAgent {
    public static final String name = LightManagerAgent.class.getSimpleName().replace("Agent", "");
    private static int cou =0;
    private final ICrossroad crossroad;

    LightManagerAgent(int id, ICrossroad crossroad,
                      ITimeProvider timeProvider,
                      EventBus eventBus) {
        super(id, name, timeProvider, eventBus);
        this.crossroad = crossroad;
    }

    @Override
    protected void setup() {
        print("I'm a traffic manager.");
        crossroad.startLifetime();

        var notifyCarAboutGreen = new TickerBehaviour(this, 1000) {//100 / TimeProvider.TIME_SCALE) {
            @Override
            protected void onTick() {
                //for all Light check
                //check if time from last green > written time
                // if so, put in the queue
                //if not
                // check count of people (remember about 2 person on pedestrian light= 1 car)
                // if queue is empty
                // apply strategy
                //for elements in queue (if there are elements in queue, make green)
                OptimizationResult result = crossroad.requestOptimizations();
                try {
                    handleOptimizationResult(result);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            private void handleOptimizationResult(OptimizationResult result) throws Exception {
                //Expected one agent in the list
                if(cou<1)
                { logger.info("I AM IN!"); sleep(1000);
                    sendMessageAboutTroubleToVehicle(null, "RegularCar1");cou++;}
               /* List<String> agents = result.carsFreeToProceed();
                if(agents.size()!=0) {
                    handleTrafficJams(result, agents.get(0));
                    for (String agentName : agents) {
                        answerCanProceed(agentName);
                    }
                }*/
            }

            private void handleTrafficJams(OptimizationResult result, String nameOfAgent) throws Exception {
                //TODO shouldNotifyCarAboutTrafficJamOnThisLight have an old state to stop the jam
            	if (true){//result.shouldNotifyCarAboutTrafficJamOnThisLight()) {
            		// TODO: use result.getJammedLight(...)
                    sendMessageAboutTroubleToVehicle(result,nameOfAgent);
            	}
            	else{
            	    //stop jam
                }
            }

            private void sendMessageAboutTroubleToVehicle(OptimizationResult result, String nameOfAgent) throws Exception {


                    ACLMessage msg = createMessage(ACLMessage.PROPOSE, nameOfAgent);
                    Properties properties = createProperties(MessageParameter.LIGHT);
                    properties.setProperty(MessageParameter.TYPEOFTROUBLE,MessageParameter.TRAFFIC_JAMS);
                    properties.setProperty(MessageParameter.TROUBLE, MessageParameter.SHOW);
                    properties.setProperty(MessageParameter.LENGTH_OF_JAM,Double.toString(1000));//result.getLengthOfJam()));
                    properties.setProperty(MessageParameter.TROUBLE_LAT, Double.toString(52.23455));//result.getJammedLightPosition().getLat()));
                    properties.setProperty(MessageParameter.TROUBLE_LON, Double.toString(20.99076));//result.getJammedLightPosition().getLng()));
                    msg.setAllUserDefinedParameters(properties);
                    logger.info("Send message to "+nameOfAgent+" for request of EdgeID");
                    //print(" send message about trouble on " + Long.toString(troublePoint.getInternalEdgeId()));
                    send(msg);

            }

            private void answerCanProceed(String carName) {
                print(carName + " can proceed.");
                ACLMessage msg = createMessage(ACLMessage.REQUEST, carName);
                Properties properties = createProperties(MessageParameter.LIGHT);
                msg.setAllUserDefinedParameters(properties);
                send(msg);
            }

        };

        var communicate = new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage rcv = receive();
                if (rcv != null) {
                    handleMessageFromRecipient(rcv);
                }
                else {
                    block();
                }
            }

            private void handleMessageFromRecipient(ACLMessage rcv) {
                String type = rcv.getUserDefinedParameter(MessageParameter.TYPE);
                if (type == null) {
                    logTypeError(rcv);
                    return;
                }
                switch (type) {
                    case MessageParameter.VEHICLE -> handleMessageFromVehicle(rcv);
                    case MessageParameter.PEDESTRIAN -> handleMessageFromPedestrian(rcv);
                }
            }

            private void handleMessageFromVehicle(ACLMessage rcv) {
                // TODO: Should be refactored - too much usage of crossroad methods.
                var agentName = getSender(rcv);
                switch (rcv.getPerformative()) {
                    case ACLMessage.INFORM -> {
                        var time = getDateParameter(rcv, MessageParameter.ARRIVAL_TIME);
                        print(agentName + " is approaching at " + time);

                        var arrivalInfo = ArrivalInfo.of(agentName, time);
                        crossroad.addCarToFarAwayQueue(getIntParameter(rcv, MessageParameter.ADJACENT_OSM_WAY_ID), arrivalInfo);
                    }
                    case ACLMessage.REQUEST_WHEN -> {
                        print(agentName + " is waiting on way " + getIntParameter(rcv,
                                MessageParameter.ADJACENT_OSM_WAY_ID) + ".");
                        crossroad.removeCarFromFarAwayQueue(getIntParameter(rcv,
                                MessageParameter.ADJACENT_OSM_WAY_ID), agentName);
                        ACLMessage agree = createMessage(ACLMessage.AGREE, agentName);
                        Properties properties = createProperties(MessageParameter.LIGHT);
                        agree.setAllUserDefinedParameters(properties);
                        send(agree);
                        crossroad.addCarToQueue(getIntParameter(rcv, MessageParameter.ADJACENT_OSM_WAY_ID), agentName);
                    }
                    case ACLMessage.AGREE -> {
                        print(agentName + " passed the light.");
                        crossroad.removeCarFromQueue(getIntParameter(rcv, MessageParameter.ADJACENT_OSM_WAY_ID));
                    }
                    case ACLMessage.REFUSE -> {
                        print(agentName + " was deleted from queque");
                        //       crossroad.removeCarFromQueue(getIntParameter(rcv, MessageParameter.ADJACENT_OSM_WAY_ID));
                        crossroad.removeCarFromFarAwayQueue(getIntParameter(rcv, MessageParameter.ADJACENT_OSM_WAY_ID), agentName);
                    }
                    default -> logger.info("Wait");
                }
            }

            private void handleMessageFromPedestrian(ACLMessage rcv) {
                // TODO: Should be refactored - too much usage of crossroad methods.
                var agentName = getSender(rcv);
                switch (rcv.getPerformative()) {
                    case ACLMessage.INFORM -> {
                        var time = getDateParameter(rcv, MessageParameter.ARRIVAL_TIME);
                        print(agentName + " is approaching in " + time);
                        var arrivalInfo = ArrivalInfo.of(agentName, time);
                        crossroad.addPedestrianToFarAwayQueue(getIntParameter(rcv, MessageParameter.ADJACENT_OSM_WAY_ID),
                                arrivalInfo);
                    }
                    case ACLMessage.REQUEST_WHEN -> {
                        print(agentName + " is waiting on way " + getIntParameter(rcv,
                                MessageParameter.ADJACENT_OSM_WAY_ID) + ".");
                        crossroad.removePedestrianFromFarAwayQueue(getIntParameter(rcv, MessageParameter.ADJACENT_OSM_WAY_ID),
                                agentName);
                        ACLMessage agree = createMessage(ACLMessage.AGREE, rcv.getSender());
                        Properties properties = createProperties(MessageParameter.LIGHT);
                        agree.setAllUserDefinedParameters(properties);
                        send(agree);
                        crossroad.addPedestrianToQueue(getIntParameter(rcv, MessageParameter.ADJACENT_OSM_WAY_ID), agentName);
                    }
                    case ACLMessage.AGREE -> {
                        print(agentName + " passed the light.");
                        crossroad.removePedestrianFromQueue(getIntParameter(rcv, MessageParameter.ADJACENT_OSM_WAY_ID));
                    }
                    default -> print("Wait");
                }
            }
        };

        addBehaviour(notifyCarAboutGreen);
        addBehaviour(communicate);
    }

    public List<Light> getLights() {
        return crossroad.getLights();
    }

    public void draw(List<Painter<JXMapViewer>> waypointPainter) {
        crossroad.draw(waypointPainter);
    }
}
