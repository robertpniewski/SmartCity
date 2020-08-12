package agents;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import genesis.AbstractModule;
import jade.wrapper.ContainerController;
import vehicles.Pedestrian;

public class AgentsModule extends AbstractModule {
    private static final Class<?>[] agentTypes = {
            VehicleAgent.class,
            BusAgent.class,
            LightManager.class,
            StationAgent.class,
            Pedestrian.class
    };

    @Override
    public void configure(Binder binder) {
        super.configure(binder);
    }

    @Provides
    @Singleton
    @Inject
    IAgentsContainer<AbstractAgent> getAgentsContainer(ContainerController controller) {
        var container = new HashAgentsContainer<>(controller);
        container.registerAll(agentTypes);
        return container;
    }

    @Provides
    @Singleton
    IdGenerator<AbstractAgent> getIdGenerator() {
        var generator = new IdGenerator<>();
        generator.registerAll(agentTypes);
        return generator;
    }
}