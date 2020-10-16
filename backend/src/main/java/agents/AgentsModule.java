package agents;

import agents.abstractions.IAgentsContainer;
import agents.abstractions.IAgentsFactory;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import genesis.AbstractModule;
import jade.wrapper.ContainerController;

public class AgentsModule extends AbstractModule {
    public static final Class<?>[] agentTypes = {
            VehicleAgent.class,
            BusAgent.class,
            LightManagerAgent.class,
            StationAgent.class,
            PedestrianAgent.class
    };

    @Override
    public void configure(Binder binder) {
        super.configure(binder);
        binder.bind(IAgentsFactory.class).to(AgentsFactory.class).in(Singleton.class);
        binder.bind(AgentsCreator.class).asEagerSingleton();
        binder.bind(TroubleManagerAgent.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    @Inject
    IAgentsContainer getAgentsContainer(ContainerController controller) {
        var container = new HashAgentsContainer(controller);
        container.registerAll(agentTypes);
        return container;
    }

    @Provides
    @Singleton
    IdGenerator getIdGenerator() {
        var generator = new IdGenerator();
        generator.registerAll(agentTypes);
        return generator;
    }
}
