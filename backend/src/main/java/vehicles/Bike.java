package vehicles;

import routing.nodes.RouteNode;
import vehicles.enums.VehicleType;

import java.util.List;

public class Bike extends MovingObject {
    public Bike(int agentId, List<RouteNode> displayRoute, List<RouteNode> uniformRoute) {
        super(agentId, 50, uniformRoute, displayRoute);
    }

    Bike(Bike bike) {
        super(bike.agentId, bike.speed, bike.uniformRoute, bike.simpleRoute);
    }

    @Override
    public String getVehicleType() {
        return VehicleType.BIKE.toString();
    }
}