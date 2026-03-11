package com.kntrel.mc.territotem.util;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;

public class RayTracing {

    public record PlaneHit(Vector worldHit, double x, double y) {}

    public static @Nullable PlaneHit hitOnPlane(Location origin, Location planeCenter, Vector planeNormal) {
        Vector org = origin.toVector();
        Vector look = origin.getDirection().normalize();
        Vector center = planeCenter.toVector();
        Vector normal = planeNormal.clone();

        if (normal.lengthSquared() < 1.0E-12) {
            return null; // plane normal is undefined
        }
        normal.normalize();

        double denom = look.dot(normal);
        if (Math.abs(denom) < 1.0E-8) {
            return null; // origin is looking parallel to the plane
        }

        double t = center.clone().subtract(org).dot(normal) / denom;
        if (t < 0) {
            return null; // intersection behind the origin
        }

        Vector hit = org.clone().add(look.clone().multiply(t));

        // Build an orthonormal basis on the plane. Use a fallback reference axis
        // when the normal is near world-up to avoid a zero cross-product.
        Vector reference = Math.abs(normal.getY()) < 0.999 ? new Vector(0, 1, 0) : new Vector(1, 0, 0);
        Vector right = reference.clone().crossProduct(normal).normalize();
        Vector planeUp = normal.clone().crossProduct(right).normalize();

        Vector rel = hit.clone().subtract(center);
        double x = rel.dot(right);
        double y = rel.dot(planeUp);

        return new PlaneHit(hit, x, y);
    }

    public static @Nullable PlaneHit hitOnVerticalFacingPlane(Location origin, Location planeCenter) {
        Vector normal = origin.toVector().subtract(planeCenter.toVector()).setY(0);
        return hitOnPlane(origin, planeCenter, normal);
    }

    public static @Nullable PlaneHit hitOnHorizontalPlane(Location origin, Location planeCenter) {
        return hitOnPlane(origin, planeCenter, new Vector(0, 1, 0));
    }
}
