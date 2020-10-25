package vehicles;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import routing.RoutingConstants;
import routing.core.IGeoPosition;
import routing.nodes.LightManagerNode;
import routing.nodes.RouteNode;
import smartcity.TimeProvider;
import vehicles.enums.DrivingState;

import java.util.List;

// TODO: Change name to IVehicle/AbstractVehicle
public abstract class MovingObject {
    final Logger logger;
    final int agentId;
    final int speed;
    List<RouteNode> simpleRoute;
    List<RouteNode> uniformRoute;
    int moveIndex;
    int closestLightIndex;
    DrivingState state;

    MovingObject(int agentId, int speed, List<RouteNode> uniformRoute, List<RouteNode> simpleRoute) {
        this.logger = LoggerFactory.getLogger(this.getClass().getSimpleName() + "Object" + agentId);
        this.agentId = agentId;
        this.speed = speed;
        this.uniformRoute = uniformRoute;
        this.simpleRoute = simpleRoute;
        this.moveIndex = 0;
        this.closestLightIndex = Integer.MAX_VALUE;
        this.state = DrivingState.STARTING;
    }

    MovingObject(int agentId, int speed, List<RouteNode> uniformRoute) {
        this.logger = LoggerFactory.getLogger(this.getClass().getSimpleName() + "Object" + agentId);
        this.agentId = agentId;
        this.speed = speed;
        this.uniformRoute = uniformRoute;
        this.moveIndex = 0;
        this.closestLightIndex = Integer.MAX_VALUE;
        this.state = DrivingState.STARTING;
    }

    public int getAgentId() {
        return agentId;
    }

    /**
     * @return Scaled speed in KM/H
     */
    public int getSpeed() {
        return speed * TimeProvider.TIME_SCALE;
    }

    public void move() {
        ++moveIndex;
        if (moveIndex > uniformRoute.size()) {
            throw new ArrayIndexOutOfBoundsException("MovingObject exceeded its route: " + moveIndex + "/" + uniformRoute.size());
        }
    }

    public int getMoveIndex() {
        return moveIndex;
    }

    public void setRoutes(final List<RouteNode> simpleRoute, final List<RouteNode> uniformRoute) {
        this.simpleRoute = simpleRoute;
        this.uniformRoute = uniformRoute;
    }

    public IGeoPosition getStartPosition() {
        return uniformRoute.get(0);
    }

    public IGeoPosition getEndPosition() {
        return uniformRoute.get(uniformRoute.size() - 1);
    }
    
    public IGeoPosition getPositionOnIndex(int index) {
        if (index >= uniformRoute.size()) {
            return uniformRoute.get(uniformRoute.size() - 1);
        }

        return uniformRoute.get(index);
    }
    public RouteNode getCurrentRouteNode() {
        if (moveIndex >= uniformRoute.size()) {
            return uniformRoute.get(uniformRoute.size() - 1);
        }

        return uniformRoute.get(moveIndex);
    }

    public IGeoPosition getPosition() {
    	return getPositionOnIndex(moveIndex);
    }

    public IGeoPosition getPositionFarOnIndex(int index) {
        return getPositionOnIndex(moveIndex + index);
    }

    public int getFarOnIndex(int index) {
        if (moveIndex + index >= uniformRoute.size()) {
            return uniformRoute.size() - 1;
        }

        return moveIndex + index;
    }

    /**
     * Checks whether an edge exists on the uniformRoute
     * @param ID of the edge checked for existence
     * @return Index of the RouteNode on uniformRoute
     * which contains the edge if edge is found, otherwise null
     */
    public Integer findIndexOfEdgeOnRoute(Long edgeId, int thresholdUntilIndexChange) {
        for (int counter = 0; counter < uniformRoute.size(); ++counter) {
            if (uniformRoute.get(counter).getInternalEdgeId() == edgeId) {
                if (moveIndex + thresholdUntilIndexChange <= counter) {
                    return counter;
                }
            }
        }
        return null;
    }


    public List<RouteNode> getUniformRoute() { return uniformRoute; }

    public abstract String getVehicleType();

    public LightManagerNode switchToNextTrafficLight() {
        for (int i = moveIndex + 1; i < uniformRoute.size(); ++i) {
            var node = uniformRoute.get(i);
            if (node instanceof LightManagerNode) {
                closestLightIndex = i;
                return (LightManagerNode) node;
            }
        }

        closestLightIndex = Integer.MAX_VALUE;
        return null;
    }

    public boolean isAtTrafficLights() {
        if (isAtDestination()) {
            return false;
        }

        return uniformRoute.get(moveIndex) instanceof LightManagerNode;
    }

    public LightManagerNode getCurrentTrafficLightNode() {
        if (closestLightIndex == Integer.MAX_VALUE) {
            return null;
        }
        return (LightManagerNode) (uniformRoute.get(closestLightIndex));
    }

    public long getAdjacentOsmWayId(int indexFar) {
        int index = moveIndex + indexFar;
        while (!(uniformRoute.get(index) instanceof LightManagerNode)) {
            --index;
        }

        return ((LightManagerNode) uniformRoute.get(index)).getAdjacentWayId();
    }

    public long getAdjacentOsmWayId(){
        return getAdjacentOsmWayId(0);
    }

    public boolean isAtDestination() {
        return moveIndex == uniformRoute.size();
    }

    public DrivingState getState() {
        return state;
    }

    public void setState(DrivingState state) {
        this.state = state;
    }

    public int getMillisecondsToNextLight() {
        return ((closestLightIndex - moveIndex) * RoutingConstants.STEP_CONSTANT) / getSpeed();
    }

    public int getMillisecondsFromAToB(int startIndex, int finishIndex) {
        return ((startIndex - startIndex) * RoutingConstants.STEP_CONSTANT) / getSpeed();
    }
    public List<RouteNode> getSimpleRoute() { return simpleRoute; }

	public boolean currentTrafficLightNodeWithinAlternativeRouteThreshold(int thresholdUntilIndexChange) {
		return moveIndex + thresholdUntilIndexChange >= closestLightIndex;
	}

}
