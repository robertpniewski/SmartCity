package routing;

import java.util.Objects;

public interface IZone {
    IGeoPosition getCenter();

    int getRadius();

    default void setZone(ZoneMutator.Mutation mutation, IGeoPosition pos, int radius) {
        Objects.requireNonNull(mutation);
    }

    boolean isInZone(IGeoPosition pos);
}