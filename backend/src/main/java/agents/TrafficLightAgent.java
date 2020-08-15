package agents;

import agents.utils.LightColor;
import behaviourfactories.BasicLightsBehaviourFactory;
import behaviourfactories.IBehaviourFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import routing.IGeoPosition;

import java.util.ArrayList;
import java.util.List;

public class TrafficLightAgent extends AbstractAgent {
    private static final Logger logger = LoggerFactory.getLogger(TrafficLightAgent.class);
    // TODO: Inject as dependency
    private final IBehaviourFactory<TrafficLightAgent> behaviourFactory;
    private final IGeoPosition position;
    private LightColor lightColor;
    private final List<String> agentsQueue;

    public TrafficLightAgent(int id, IGeoPosition position) {
        super(id);
        this.position = position;
        this.behaviourFactory = new BasicLightsBehaviourFactory();
        this.lightColor = LightColor.RED;
        this.agentsQueue = new ArrayList<>();
    }

    @Override
    public String getNamePrefix() {
        return "TrafficLight";
    }

    @Override
    protected void setup() {
        print("I'm a traffic light.");
        print("Red light.");
        addBehaviour(behaviourFactory.createCyclicBehaviour(this));
        addBehaviour(behaviourFactory.createTickerBehaviour(this));
    }

    public IGeoPosition getPosition() {
        return position;
    }

    public LightColor getLightColor() {
        return lightColor;
    }

    public void setLightColor(LightColor lightColor) {
        this.lightColor = lightColor;
    }

    // TODO: Is the name of method correct??
    public Iterable<String> getWaitingAgents() {
        return agentsQueue;
    }

    public void addAgentToQueue(String name) {
        agentsQueue.add(name);
    }

    public void removeAgentFromQueue(String name) {
        agentsQueue.remove(name);
    }
}
