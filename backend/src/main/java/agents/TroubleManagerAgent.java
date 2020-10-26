package agents;

import agents.abstractions.IAgentsContainer;
import agents.utilities.MessageParameter;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import events.web.TroublePointCreatedEvent;
import events.web.TroublePointVanishedEvent;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.leap.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import routing.core.IGeoPosition;
import routing.core.Position;

import java.util.HashMap;

import static agents.message.MessageManager.createProperties;

public class TroubleManagerAgent extends Agent {
    public static final String name = TroubleManagerAgent.class.getSimpleName().replace("Agent", "");
    private final static Logger logger = LoggerFactory.getLogger(TroubleManagerAgent.class);

    private final IAgentsContainer agentsContainer;
    private final EventBus eventBus;
    private final HashMap<IGeoPosition, Integer> troublePointsMap;
    private int latestTroublePointId;

    @Inject
    TroubleManagerAgent(IAgentsContainer agentsContainer,
                        EventBus eventBus) {
        this.agentsContainer = agentsContainer;
        this.eventBus = eventBus;
        this.troublePointsMap = new HashMap<>();
    }

    @Override
    protected void setup() {

        //TODO: wysłać broadcact kiedy trouble się skończy
        Behaviour communication = new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage rcv = receive();
                if (rcv != null) {
                    switch (rcv.getPerformative()) {
                        case ACLMessage.INFORM -> {

                            if (rcv.getUserDefinedParameter(MessageParameter.TROUBLE).equals(MessageParameter.SHOW)) {
                                //parsing received message
                                if (rcv.getUserDefinedParameter(MessageParameter.TYPEOFTROUBLE).equals(MessageParameter.CONSTRUCTION)) {
                                    constructionAppearedHandle(rcv);
                                }
                                else if (rcv.getUserDefinedParameter(MessageParameter.TYPEOFTROUBLE).equals(MessageParameter.TRAFFIC_JAMS)) {
                                    trafficJamsAppearedHandle(rcv);
                                }
                            }
                            else if (rcv.getUserDefinedParameter(MessageParameter.TROUBLE).equals(MessageParameter.STOP)) {
                                //TODO: FOR FUTURE CHANGE ROOT AGAIN OF THE CAR?
                                if (rcv.getUserDefinedParameter(MessageParameter.TYPEOFTROUBLE).equals(MessageParameter.CONSTRUCTION)) {
                                    constructionHideHandle(rcv);
                                }
                            }
                        }
                    }
                }
                block(100);
            }

            private void trafficJamsAppearedHandle(ACLMessage rcv) {
                //TODO: Rysowanie w LightManager - Przemek
                logger.info("Got message about trouble - TRAFFIC JAM");
                sendBroadcast(generateMessageAboutTrouble(rcv, MessageParameter.TRAFFIC_JAMS));

            }

            private void sendBroadcast(ACLMessage response) {
                agentsContainer.forEach(VehicleAgent.class, vehicleAgent -> {
                    response.addReceiver(vehicleAgent.getAID());
                });
                send(response);
                logger.info("Sent broadcast");
            }

            private ACLMessage generateMessageAboutTrouble(ACLMessage rcv, String typeOfTrouble) {
                long edgeId = Long.parseLong(rcv.getUserDefinedParameter(MessageParameter.EDGE_ID));
                logger.info("trouble edge: " + edgeId);
                // broadcasting to everybody
                ACLMessage response = new ACLMessage(ACLMessage.PROPOSE);
                Properties properties = createProperties(MessageParameter.TROUBLE_MANAGER);
                properties.setProperty(MessageParameter.EDGE_ID, Long.toString(edgeId));
                properties.setProperty(MessageParameter.TYPEOFTROUBLE, typeOfTrouble);
                if (typeOfTrouble.equals(MessageParameter.TRAFFIC_JAMS)) {
                    properties.setProperty(MessageParameter.LENGTH_OF_JAM, rcv.getUserDefinedParameter(MessageParameter.LENGTH_OF_JAM));
                }
                response.setAllUserDefinedParameters(properties);
                return response;
            }

            private void constructionAppearedHandle(ACLMessage rcv) {
                var troublePoint = Position.of(Double.parseDouble(rcv.getUserDefinedParameter(MessageParameter.TROUBLE_LAT)),
                        Double.parseDouble(rcv.getUserDefinedParameter(MessageParameter.TROUBLE_LON)));
                troublePointsMap.put(troublePoint, ++latestTroublePointId);
                eventBus.post(new TroublePointCreatedEvent(latestTroublePointId, troublePoint));

                logger.info("Got message about trouble - CONSTRUCTION\n"
                        + "    troublePoints: " + troublePoint.toString());
                sendBroadcast(generateMessageAboutTrouble(rcv, MessageParameter.CONSTRUCTION));
            }

            private void constructionHideHandle(ACLMessage rcv) {
                var troublePoint = Position.of(Double.parseDouble(rcv.getUserDefinedParameter(MessageParameter.TROUBLE_LAT)),
                        Double.parseDouble(rcv.getUserDefinedParameter(MessageParameter.TROUBLE_LON)));
                var id = troublePointsMap.remove(troublePoint);

                eventBus.post(new TroublePointVanishedEvent(id));
                logger.info("Hiding construction" + id);
            }

        };
        addBehaviour(communication);
    }
}
