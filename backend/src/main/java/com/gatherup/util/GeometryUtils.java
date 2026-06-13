package com.gatherup.util;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public final class GeometryUtils {

    private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private GeometryUtils() {}

    // JTS Coordinate takes (x=longitude, y=latitude) — not (lat, lng)
    public static Point createPoint(double latitude, double longitude) {
        return FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    // Returns [latitude, longitude]
    public static double[] extractCoordinates(Point point) {
        return new double[]{point.getY(), point.getX()};
    }
}
