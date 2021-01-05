package agents.singletons;

import agents.TroubleManagerAgent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import events.web.SimulationStartedEvent;
import jade.core.Agent;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartcity.SmartCityAgent;

public class SingletonAgentsActivator {
    private static final Logger logger = LoggerFactory.getLogger(SingletonAgentsActivator.class);
    private final ContainerController controller;
    private final TroubleManagerAgent troubleManagerAgent;
    private final EventBus eventBus;

    @Inject
    //TODO:dokumentacja
    public SingletonAgentsActivator(ContainerController controller,
                                    EventBus eventBus,
                                    SmartCityAgent smartCityAgent,
                                    TroubleManagerAgent troubleManagerAgent) {
        this.controller = controller;
        this.eventBus = eventBus;

        this.troubleManagerAgent = troubleManagerAgent;
        activate(SmartCityAgent.name, smartCityAgent);
    }

    private void activate(String name, Agent agent) {
        try {
            var agentController = controller.acceptNewAgent(name, agent);
            agentController.activate();
            agentController.start();
        } catch (StaleProxyException e) {
            logger.error("Error accepting main agent", e);
            System.exit(-1);
        }
    }

    // Need timeScale, so possible only after simulation start
    @Subscribe
    //TODO:dokumentacja
    public void handle(SimulationStartedEvent e) {
        activate(TroubleManagerAgent.name, troubleManagerAgent);
        this.eventBus.unregister(this);
    }
}
