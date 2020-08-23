package agents;

import agents.abstractions.IRegistrable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: Move all static methods to getId/resetId
public class IdGenerator implements IRegistrable {
    public static final int resetValue = 1;
    private final ConcurrentMap<Class<?>, AtomicInteger> idMap;
    
    private static final AtomicInteger stationAgentId = new AtomicInteger();
    private static final AtomicInteger pedestrianId = new AtomicInteger();
    private static final AtomicInteger vehicleId = new AtomicInteger();

    IdGenerator() {
        this.idMap = new ConcurrentHashMap<>();
    }

    private void register(Class<?> type) {
        idMap.putIfAbsent(type, new AtomicInteger());
    }

    @Override
    public final void register(Class<?>... types) {
        for (var type : types) {
            register(type);
        }
    }

    @Override
    public void registerAll(Class<?>[] types) {
        for (var type : types) {
            register(type);
        }
    }

    public int get(Class<?> type) {
        return idMap.get(type).getAndIncrement();
    }

    public void reset(Class<?> type) {
        idMap.get(type).set(resetValue);
    }

    @Deprecated
    public static int getStationAgentId() {
        return stationAgentId.getAndIncrement();
    }

    @Deprecated
    public static void resetStationAgentId() {
        stationAgentId.set(resetValue);
    }
}
