package smartcity.task;

import agents.LightManagerAgent;
import agents.abstractions.AbstractAgent;
import agents.abstractions.IAgentsContainer;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import events.ClearSimulationEvent;
import events.SwitchLightsStartEvent;
import events.web.SimulationStartedEvent;
import events.web.StartSimulationEvent;
import smartcity.SimulationState;
import smartcity.config.ConfigContainer;
import smartcity.task.abstractions.ITaskManager;

public class Scheduler {
    private final ITaskManager taskManager;
    private final ConfigContainer configContainer;
    private final IAgentsContainer agentsContainer;
    private final EventBus eventBus;

    @Inject
    public Scheduler(ITaskManager taskManager,
                     ConfigContainer configContainer,
                     IAgentsContainer agentsContainer,
                     EventBus eventBus) {
        this.taskManager = taskManager;
        this.configContainer = configContainer;
        this.agentsContainer = agentsContainer;
        this.eventBus = eventBus;
    }

    @SuppressWarnings("FeatureEnvy")
    @Subscribe
    public void handle(StartSimulationEvent e) {
        configContainer.setExtendLightTime(e.extendLightTime);
        configContainer.setLightStrategyActive(e.lightStrategyActive);

        activateLightManagerAgents();
        if (e.shouldGenerateCars) {
            configContainer.setShouldGenerateConstructionSites(e.shouldGenerateTroublePoints);
            configContainer.setChangeRouteStrategyActive(e.changeRouteStrategyActive);
            taskManager.scheduleCarCreation(e.carsNum, e.testCarId);
        }
        if (configContainer.shouldGeneratePedestriansAndBuses()) {
            configContainer.setStationStrategyActive(e.stationStrategyActive);
            configContainer.setExtendWaitTime(e.extendWaitTime);

            taskManager.schedulePedestrianCreation(configContainer.getPedestriansNumber(), configContainer.getTestPedestrianId());
            taskManager.scheduleBusControl(() -> configContainer.getSimulationState() == SimulationState.RUNNING);
        }

        configContainer.setSimulationState(SimulationState.RUNNING);
        taskManager.scheduleSimulationControl(() -> configContainer.getSimulationState() == SimulationState.RUNNING,
                e.startTime);
        eventBus.post(new SimulationStartedEvent());
    }

    private void activateLightManagerAgents() {
        agentsContainer.forEach(LightManagerAgent.class, AbstractAgent::start);
    }

    @Subscribe
    public void handle(SwitchLightsStartEvent e) {
        taskManager.scheduleSwitchLightTask(e.managerId, e.lights);
    }

    @Subscribe
    public void handle(ClearSimulationEvent e) {
        taskManager.cancelAll();
    }
}
