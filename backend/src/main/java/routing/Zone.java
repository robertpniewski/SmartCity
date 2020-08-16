package routing;

import utilities.NumericHelper;

import java.util.Objects;

public class Zone implements IZone {
    private IGeoPosition center;
    private int radius;

    public Zone(IGeoPosition center, int radius) {
        this.center = center;
        this.radius = radius;
    }

    public Zone(double lat, double lng, int radius) {
        this.center = Position.of(lat, lng);
        this.radius = radius;
    }

    @Override
    public IGeoPosition getCenter() {
        return center;
    }

    @Override
    public int getRadius() {
        return radius;
    }

    @Override
    public void setZone(ZoneMutator.Mutation mutation, IGeoPosition pos, int radius) {
        Objects.requireNonNull(mutation);
        this.center = pos;
        this.radius = radius;
    }

    @Override
    public boolean isInZone(IGeoPosition pos) {
        return NumericHelper.isInCircle(pos, center, radius);
    }
}
