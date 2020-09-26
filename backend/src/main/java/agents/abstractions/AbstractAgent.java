package agents.abstractions;

import agents.LightManagerAgent;
import agents.utilities.LoggerLevel;
import agents.utilities.MessageParameter;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.util.leap.Properties;
import jade.wrapper.ControllerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import routing.LightManagerNode;
import smartcity.MasterAgent;
import vehicles.MovingObject;

import java.time.Instant;
import java.util.List;

public abstract class AbstractAgent extends Agent {
    private final int id;
    private final String namePrefix;
    protected final Logger logger;


    public AbstractAgent(int id, String namePrefix) {
        this.id = id;
        this.namePrefix = namePrefix;
        this.logger = LoggerFactory.getLogger(this.getPredictedName());
    }

    public String getPredictedName() {
        return getPredictedName(this.namePrefix, id);
    }

    private String getPredictedName(String prefix, int id) {
        return prefix + id;
    }

    public int getId() {
        return id;
    }

    public void start() {
        try {
            this.getContainerController().getAgent(getLocalName()).start();
        } catch (ControllerException e) {
            logger.warn("Error activating agent", e);
        }
    }

    public void print(String message, LoggerLevel level) {
        switch (level) {
            case TRACE -> logger.trace(message);
            case DEBUG -> logger.debug(message);
            case INFO -> logger.info(message);
            case WARN -> logger.warn(message);
            case ERROR -> logger.error(message);
        }
    }

    // TODO: Protected
    public void print(String message) {
        print(message, LoggerLevel.INFO);
    }

    // TODO: Pass only LightManager here, remove movingObject and pass additional parameters
    protected void informLightManager(MovingObject movingObject) {
        // finds next traffic light and announces his arrival
        LightManagerNode nextManager = movingObject.getNextTrafficLight();
        if (nextManager != null) {
            ACLMessage msg = prepareMessageForManager(nextManager, movingObject);
            send(msg);
            print("Sending INFORM to LightManager" + nextManager.getLightManagerId() + ".");
        }
    }

    private ACLMessage prepareMessageForManager(LightManagerNode managerNode, MovingObject movingObject) {
        ACLMessage msg = createMessage(ACLMessage.INFORM, LightManagerAgent.name, managerNode.getLightManagerId());

        Properties properties = new Properties();
        var agentType = MessageParameter.getTypeByMovingObject(movingObject);
        properties.setProperty(MessageParameter.TYPE, agentType);
        Instant time = MasterAgent.getSimulationTime().toInstant().plusMillis(movingObject.getMillisecondsToNextLight());
        properties.setProperty(MessageParameter.ARRIVAL_TIME, "" + time);
        properties.setProperty(MessageParameter.ADJACENT_OSM_WAY_ID, "" + managerNode.getOsmWayId());
        msg.setAllUserDefinedParameters(properties);

        return msg;
    }

    // TODO: Special class - MessageCreator for all msg-related code, protected, dependency, injected
    protected ACLMessage createMessage(int type, List<String> receivers) {
        ACLMessage msg = new ACLMessage(type);
        for (var name : receivers) {
            msg.addReceiver(new AID(name, AID.ISLOCALNAME));
        }
        return msg;
    }

    protected ACLMessage createMessage(int type, String receiverName) {
        var receiver = new AID(receiverName, AID.ISLOCALNAME);
        return createMessage(type, receiver);
    }

    protected ACLMessage createMessage(int type, String receiverName, int receiverId) {
        var receiver = new AID(getPredictedName(receiverName, receiverId), AID.ISLOCALNAME);
        return createMessage(type, receiver);
    }

    protected ACLMessage createMessage(int type, AID receiver) {
        ACLMessage msg = new ACLMessage(type);
        msg.addReceiver(receiver);
        return msg;
    }

    protected Properties createProperties(String senderType){
        var result = new Properties();
        result.setProperty(MessageParameter.TYPE, senderType);
        return result;
    }
}
