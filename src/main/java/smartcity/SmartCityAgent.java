package smartcity;

import agents.*;
import gui.MapWindow;
import org.jxmapviewer.viewer.DefaultWaypointRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import osmproxy.elements.OSMNode;
import osmproxy.elements.OSMStation;
import osmproxy.LightAccessManager;
import osmproxy.MapAccessManager;
import routing.LightManagerNode;
import routing.RouteNode;
import routing.Router;
import routing.StationNode;
import smartcity.buses.BusInfo;
import smartcity.buses.Timetable;
import vehicles.MovingObjectImpl;
import vehicles.Pedestrian;
import vehicles.TestCar;
import vehicles.TestPedestrian;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentContainer;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import org.javatuples.Pair;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;
import org.w3c.dom.Node;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class SmartCityAgent extends Agent {
    private static final Logger logger = LoggerFactory.getLogger(SmartCityAgent.class);
    public final static String LIGHT_MANAGER = "LightManager";
    public final static String BUS = "Bus";
    public final static String STATION = "Station";
    public final static String PEDESTRIAN = "Pedestrian";
    public final static boolean USE_DEPRECATED_XML_FOR_LIGHT_MANAGERS = false;
    public static final String STEPS = "6";
    public static boolean SHOULD_GENERATE_PEDESTRIANS_AND_BUSES = false;
    public static boolean SHOULD_GENERATE_CARS = true;
    public static List<PedestrianAgent> pedestrians = new ArrayList<>();
    public static Set<LightManager> lightManagers = new HashSet<>();
    public static Set<StationAgent> stationAgents = new HashSet<>();
    public static boolean lightManagersUnderConstruction = false;
    public static Map<Pair<Long, Long>, LightManagerNode> wayIdLightIdToLightManagerNode = new HashMap<>();
    public static Map<Long, LightManagerNode> crossingOsmIdToLightManagerNode = new HashMap<>();
    public static Map<Long, StationNode> osmStationIdToStationNode = new HashMap<>();
    public static Map<Long, OSMStation> osmIdToStationOSMNode = new HashMap<>();
    public static Set<BusAgent> buses = new LinkedHashSet<>();
    private static long nextLightManagerId;
    private static long nextStationAgentId;
    private static int nextBusId;
    private static int nextPedestrianAgentId;
    private static AgentContainer container;
    private static MapWindow window;
    public List<VehicleAgent> Vehicles = new ArrayList<>();
    public int carId = 0;
    public int pedestrianId = 0;
    private JXMapViewer mapViewer;
    private CyclicBehaviour receiveMessage = new CyclicBehaviour() {
        @Override
        public void action() {
            ACLMessage rcv = receive();
            if (rcv != null) {
                logger.info("SmartCity: " + rcv.getSender().getLocalName() + " arrived at destination."); // TODO: Does it work?? (can't see it in the logs)
                String type = rcv.getUserDefinedParameter(MessageParameter.TYPE);
                switch (type) {
                    case MessageParameter.VEHICLE:
                        onReceiveVehicle(rcv);
                        break;
                    case MessageParameter.PEDESTRIAN:
                        onReceivePedestrian(rcv);
                        break;
                    case MessageParameter.BUS:
                        buses.removeIf(v -> v.getLocalName().equals(rcv.getSender().getLocalName()));
                        break;
                }
            }
            block(1000);
        }
    };

    private void onReceivePedestrian(ACLMessage rcv) {
        for (int i = 0; i < pedestrians.size(); i++) {
            PedestrianAgent v = pedestrians.get(i);
            if (v.getLocalName().equals(rcv.getSender().getLocalName())) {
                if (v.getPedestrian() instanceof TestPedestrian) {
                    TestPedestrian pedestrian = (TestPedestrian) v.getPedestrian();
                    setResultTime(pedestrian.start, pedestrian.end);
                }
                pedestrians.remove(i);
                break;
            }
        }
    }

    private void setResultTime(Instant start, Instant end) {
        long seconds = Duration.between(start, end).getSeconds();
        String time = String.format(
                "%d:%02d:%02d",
                seconds / 3600,
                (seconds % 3600) / 60,
                seconds % 60);
        window.setResultTime(time);
    }


    private void onReceiveVehicle(ACLMessage rcv) {
        for (int i = 0; i < Vehicles.size(); i++) {
            VehicleAgent v = Vehicles.get(i);
            if (v.getLocalName().equals(rcv.getSender().getLocalName())) {
                if (v.getVehicle() instanceof TestCar) {
                    TestCar car = (TestCar) v.getVehicle();
                    setResultTime(car.start, car.end);
                }
                Vehicles.remove(i);
                break;
            }
        }
    }


    public static Date getSimulationTime() {
        return window.getSimulationStartTime();
    }

    public static void ActivateAgent(Agent agent) {
        try {
            agent.getContainerController().getAgent(agent.getLocalName()).start();
        } catch (ControllerException e) {
            e.printStackTrace();
        }
    }

    private static long nextLightManagerId() {
        return nextLightManagerId++;
    }

    private static long nextStationAgentId() {
        return nextStationAgentId++;
    }

    private static int nextBusId() {
        return nextBusId++;
    }

    private static int nextPedestrianAgentId() {
        return nextPedestrianAgentId++;
    }

    private static void tryAddAgent(Agent agent, String agentName) {
        try {
            container.acceptNewAgent(agentName, agent);
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

    public static void tryAddNewBusAgent(final Timetable timetable, List<RouteNode> route,
                                         final String busLine, final String brigadeNr) {
        BusAgent agent = new BusAgent();
        final int busAgentId = nextBusId();
        agent.setArguments(new Object[]{route, timetable, busLine, brigadeNr, busAgentId});
        smartcity.SmartCityAgent.buses.add(agent);
        tryAddAgent(agent, BUS + busAgentId);
    }

    public static void tryAddNewLightManagerAgent(Node crossroad) {
        LightManager manager = new LightManager(crossroad, nextLightManagerId());
        smartcity.SmartCityAgent.lightManagers.add(manager);
        tryAddAgent(manager, LIGHT_MANAGER + manager.getId());
    }

    public static void tryAddNewLightManagerAgent(final OSMNode centerCrossroadNode) {
        LightManager manager = new LightManager(centerCrossroadNode, nextLightManagerId());
        smartcity.SmartCityAgent.lightManagers.add(manager);
        tryAddAgent(manager, LIGHT_MANAGER + manager.getId());
    }

    public static Agent tryAddNewStationAgent(OSMStation stationOSMNode) {
        StationAgent stationAgent = new StationAgent(stationOSMNode, nextStationAgentId());
        smartcity.SmartCityAgent.osmIdToStationOSMNode.put(stationOSMNode.getId(), stationOSMNode);
        tryAddAgent(stationAgent, STATION + stationAgent.getAgentId());
        return stationAgent;
    }

    public static Agent tryAddNewPedestrianAgent(Pedestrian pedestrian) {
        PedestrianAgent pedestrianAgent = new PedestrianAgent(pedestrian, nextPedestrianAgentId());
        smartcity.SmartCityAgent.pedestrians.add(pedestrianAgent);
        tryAddAgent(pedestrianAgent, PEDESTRIAN + pedestrianAgent.getAgentId());
        return pedestrianAgent;
    }

    protected void setup() {
        container = getContainerController();
        displayGUI();
        addBehaviour(receiveMessage);
    }

    private void displayGUI() {
        window = new MapWindow(this);
        mapViewer = window.MapViewer;
        JFrame frame = new JFrame("Smart City by Katherine & Dominic & Robert");
        frame.getContentPane().add(window.MainPanel);
        JMenuBar menuBar = new JMenuBar();
        JMenu view = new JMenu("View");

        final JCheckBoxMenuItem cars = new JCheckBoxMenuItem("Render cars", window.renderCars);
        cars.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                window.renderCars = cars.getState();
            }
        });
        view.add(cars);

        final JCheckBoxMenuItem routes = new JCheckBoxMenuItem("Render car routes", window.renderCarRoutes);
        routes.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                window.renderCarRoutes = routes.getState();
            }
        });
        view.add(routes);

        final JCheckBoxMenuItem buses = new JCheckBoxMenuItem("Render buses", window.renderBuses);
        buses.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                window.renderBuses = buses.getState();
            }
        });
        view.add(buses);

        final JCheckBoxMenuItem busRoutes = new JCheckBoxMenuItem("Render bus routes", window.renderBusRoutes);
        busRoutes.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                window.renderBusRoutes = busRoutes.getState();
            }
        });
        view.add(busRoutes);

        final JCheckBoxMenuItem pedestrian = new JCheckBoxMenuItem("Render pedestrians", window.renderPedestrians);
        pedestrian.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                window.renderPedestrians = pedestrian.getState();
            }
        });
        view.add(pedestrian);

        final JCheckBoxMenuItem pedestrianRoutes = new JCheckBoxMenuItem("Render pedestrian routes", window.renderPedestrianRoutes);
        pedestrianRoutes.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                window.renderPedestrianRoutes = pedestrianRoutes.getState();
            }
        });
        view.add(pedestrianRoutes);

        final JCheckBoxMenuItem lights = new JCheckBoxMenuItem("Render lights", window.renderLights);
        lights.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                window.renderLights = lights.getState();
            }
        });
        view.add(lights);

        final JCheckBoxMenuItem zone = new JCheckBoxMenuItem("Render zone", window.renderZone);
        zone.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                window.renderZone = zone.getState();
            }
        });
        view.add(zone);

        final JCheckBoxMenuItem stations = new JCheckBoxMenuItem("Render stations", window.renderStations);
        stations.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                window.renderStations = stations.getState();
            }
        });
        view.add(stations);

        menuBar.add(view);

        JMenu debug = new JMenu("Debug");

        JMenuItem runTest = new JMenuItem("Test crossroad");
        runTest.addActionListener(new ActionListener() {
            private void prepareCar(List<RouteNode> info) {
                VehicleAgent vehicle = new VehicleAgent();
                MovingObjectImpl car = new MovingObjectImpl(info);
                vehicle.setVehicle(car);
                try {
                    AddNewVehicleAgent(car.getVehicleType() + carId, vehicle);
                    carId++;


                } catch (StaleProxyException ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                window.setInputEnabled(false);
                double lat = 52.23702507833161;
                double lon = 21.017934679985046;
                mapViewer.setAddressLocation(new GeoPosition(lat, lon));
                mapViewer.setZoom(1);
                window.prepareAgentsAndSetZone(lat, lon, 100);
                GeoPosition N = new GeoPosition(52.23758683540269, 21.017720103263855);
                GeoPosition S = new GeoPosition(52.23627934304847, 21.018092930316925);
                GeoPosition E = new GeoPosition(52.237225472020704, 21.019399166107178);
                GeoPosition W = new GeoPosition(52.23678526174392, 21.016663312911987);

                // N to S
                List<RouteNode> NS;
                try {
                    NS = Router.generateRouteInfo(N, S);

                    for (int i = 0; i < 5; i++) {
                        prepareCar(NS);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return;
                }

                // S to N
                List<RouteNode> SN;
                try {
                    SN = Router.generateRouteInfo(S, N);

                    for (int i = 0; i < 5; i++) {
                        prepareCar(SN);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return;
                }

                // E to W
                List<RouteNode> EW;
                try {
                    EW = Router.generateRouteInfo(E, W);

                    for (int i = 0; i < 5; i++) {
                        prepareCar(EW);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return;
                }

                // W to E
                List<RouteNode> WE;
                try {
                    WE = Router.generateRouteInfo(W, E);
                    for (int i = 0; i < 5; i++) {
                        prepareCar(WE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return;
                }


                activateLightManagerAgents();

                // start all
                for (VehicleAgent agent : Vehicles) {
                    ActivateAgent(agent);
                }

            }
        });

        debug.add(runTest);

        menuBar.add(debug);

        addGenerationMenu(menuBar);

        frame.setJMenuBar(menuBar);
        frame.setSize(1200, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private void addGenerationMenu(JMenuBar menuBar) {
        JMenu generation = new JMenu("Generation");

        final JCheckBoxMenuItem car_gen = new JCheckBoxMenuItem("Cars", SHOULD_GENERATE_CARS);
        car_gen.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                SHOULD_GENERATE_CARS = car_gen.getState();
            }
        });
        generation.add(car_gen);

        final JCheckBoxMenuItem pedestrians = new JCheckBoxMenuItem("Pedestrians", SHOULD_GENERATE_PEDESTRIANS_AND_BUSES);
        pedestrians.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                SHOULD_GENERATE_PEDESTRIANS_AND_BUSES = pedestrians.getState();
            }
        });
        generation.add(pedestrians);

        menuBar.add(generation);
    }

    public void AddNewVehicleAgent(String name, VehicleAgent agent) throws StaleProxyException {
        container.acceptNewAgent(name, agent);
        Vehicles.add(agent);
    }

    public void activateLightManagerAgents() {
        for (LightManager lightManager : lightManagers) {
            ActivateAgent(lightManager);
        }
    }

    public void prepareLightManagers(GeoPosition middlePoint, int radius) {
        resetIdGenerator();
        lightManagersUnderConstruction = true;
        if (USE_DEPRECATED_XML_FOR_LIGHT_MANAGERS) {
            MapAccessManager.prepareLightManagersInRadiusAndLightIdToLightManagerIdHashSet(this, middlePoint, radius);
        }
        else {
            tryPrepareLightManagersInRadiusAndLightIdToLightManagerIdHashSetBeta(this, middlePoint, radius);
        }
        lightManagersUnderConstruction = false;
    }

    private void tryPrepareLightManagersInRadiusAndLightIdToLightManagerIdHashSetBeta(SmartCityAgent smartCityAgent,
                                                                                      GeoPosition middlePoint, int radius) {
        try {
            LightAccessManager.prepareLightManagersInRadiusAndLightIdToLightManagerIdHashSetBeta(this, middlePoint, radius);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void resetIdGenerator() {
        nextLightManagerId = 1;
    }

    private void resetBusIdGen() {
        nextBusId = 1;
    }

    private void resetStationAgentIdGenerator() {
        nextStationAgentId = 1;
    }

    private void resetPedestrianAgentIdGenerator() {
        nextPedestrianAgentId = 1;
    }

    public void prepareStationsAndBuses(GeoPosition middlePoint, int radius) {
        resetStationAgentIdGenerator();
        logger.info("STEP 1/" + SmartCityAgent.STEPS + ": Starting bus preparation");
        resetBusIdGen();
        buses = new LinkedHashSet<>();
        Set<BusInfo> busInfoSet = MapAccessManager.getBusInfo(radius, middlePoint.getLatitude(), middlePoint.getLongitude());
        logger.info("STEP 5/" + SmartCityAgent.STEPS + ": Starting agent preparation based on queries");
        int i = 0;
        for (BusInfo info : busInfoSet) {
            logger.info("STEP 5/" + SmartCityAgent.STEPS + " (SUBSTEP " + (++i) + "/" + busInfoSet.size() + "): Agent preparation substep");
            info.prepareAgents(container);
        }
        logger.info("STEP 6/" + SmartCityAgent.STEPS + ": Buses are created!");
        logger.info("NUMBER OF BUS AGENTS: " + buses.size());
    }
}
