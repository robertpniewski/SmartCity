package routing;

import java.util.List;

public interface IRouteTransformer {
    List<RouteNode> uniformRoute(List<RouteNode> route);
}
