/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.geom;

import java.awt.*;
import java.util.*;

/**
 * See http://www.cs.kuleuven.be/~ares/Publications/LagaeDutre2005AnEfficientRayQuadrilateralIntersectionTest/paper.pdf
 * for a description of the calculations used to compute barycentric and bilinear coordinates.
 *
 * @author tag
 * @version $Id: BarycentricQuadrilateral.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class BarycentricQuadrilateral extends BarycentricTriangle {
    private static final Vec4 g0 = new Vec4(-180, -90, 0);
    private static final Vec4 g1 = new Vec4(180, -90, 0);
    private static final Vec4 g2 = new Vec4(180, 90, 0);
    private static final Vec4 g3 = new Vec4(-180, 90, 0);
    private static final Vec4 i0 = new Vec4(0.0d, 0.0d, 0.0d);
    private static final Vec4 i1 = new Vec4(1.0d, 0.0d, 0.0d);
    private static final Vec4 i2 = new Vec4(2.0d, 2.0d, 0.0d);
    private static final Vec4 i3 = new Vec4(0.0d, 1.0d, 0.0d);
    private static final Iterable<Vec4> testPoints = new ArrayList<>(Arrays.asList(
        BarycentricQuadrilateral.g0, BarycentricQuadrilateral.g1, BarycentricQuadrilateral.g2, BarycentricQuadrilateral.g3,
        BarycentricQuadrilateral.i0, BarycentricQuadrilateral.i1, BarycentricQuadrilateral.i2, BarycentricQuadrilateral.i3,
        new Vec4(-17, 0, 0)
//        new Vec4(-122.4, 34.2, 0),
//        new Vec4(-120.6, 34.2, 0),
//        new Vec4(-120.6, 36, 0),
//        new Vec4(-122.4, 36, 0)
    ));
    protected final Vec4 p11;
    private final double[] w11;

    public BarycentricQuadrilateral(Vec4 p00, Vec4 p10, Vec4 p11, Vec4 p01) {
        super(p00, p10, p01);

        this.p11 = p11;
        this.w11 = this.getBarycentricCoords(this.p11);
    }

    public BarycentricQuadrilateral(LatLon p00, LatLon p10, LatLon p11, LatLon p01) {
        super(p00, p10, p01);

        this.p11 = new Vec4(p11.getLon().radians(), p11.getLat().radians(), 0);
        this.w11 = this.getBarycentricCoords(this.p11);
    }

    public BarycentricQuadrilateral(Point p00, Point p10, Point p11, Point p01) {
        super(p00, p10, p01);

        this.p11 = new Vec4(p11.x, p11.y, 0);
        this.w11 = this.getBarycentricCoords(this.p11);
    }

    public static double[] invertBilinear(Vec4 U, Vec4 X, Vec4 Y, Vec4 Z, Vec4 W) {
        Vec4 s1 = W.subtract3(X);
        Vec4 s2 = Z.subtract3(Y);
        Vec4 UminX = U.subtract3(X);
        Vec4 UminY = U.subtract3(Y);
        Vec4 normal = Z.subtract3(X).cross3(W.subtract3(Y));

        double A = s1.cross3(s2).dot3(normal);
        double B = s2.cross3(UminX).dot3(normal) - s1.cross3(UminY).dot3(normal);
        double C = UminX.cross3(UminY).dot3(normal);

        double descriminant = B * B - 4.0d * A * C;
        if (descriminant < 0)
            return null;
        descriminant = Math.sqrt(descriminant);

        double beta = B > 0 ? (-B - descriminant) / (2.0d * A) : 2.0d * C / (-B + descriminant);

        Vec4 Sbeta1 = Vec4.mix3(beta, X, W);
        Vec4 Sbeta2 = Vec4.mix3(beta, Y, Z);

        double alpha = U.subtract3(Sbeta1).dot3(Sbeta2.subtract3(Sbeta1)) / Sbeta2.subtract3(Sbeta1).dotSelf3();

        return new double[] {alpha, beta};
    }

    public static void main(String[] args) {
        BarycentricPlanarShape bc = new BarycentricQuadrilateral(BarycentricQuadrilateral.i0, BarycentricQuadrilateral.i1, BarycentricQuadrilateral.i2, BarycentricQuadrilateral.i3);

        for (Vec4 point : BarycentricQuadrilateral.testPoints) {
            double[] w = bc.getBarycentricCoords(point);
            Vec4 p = bc.getPoint(w);
            double[] uv = bc.getBilinearCoords(w[1], w[2]);

            System.out.printf("%s, %s: ( %f, %f, %f) : ( %f, %f), %s\n",
                point, p, w[0], w[1], w[2], uv[0], uv[1], p.equals(point) ? "true" : "false");
        }
    }

    public Vec4 getP11() {
        return p11;
    }

    @Override
    public boolean contains(Vec4 p) {
        return this.invertBilinear(p) != null;
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public double[] getBilinearCoords(double alpha, double beta) {
        // TODO: this method isn't always finding the correct -- or any -- roots
        double eps = 1.0e-9;
        double u, v;

        double alpha11 = this.w11[1];
        double beta11 = this.w11[2];

        if (Math.abs(alpha11 - 1) < eps) // if alpha11 == 1
        {
            u = alpha;
            if (Math.abs(beta11 - 1) < eps) // if beta == 1
                v = beta;
            else
                v = beta / (u * (beta11 - 1) + 1);
        } else if (Math.abs(beta11 - 1) < eps) // if beta = 1
        {
            v = beta;
            u = alpha / (v * (alpha11 - 1) + 1);
        } else {
            double a = 1.0d - beta11;
            double b = alpha * (beta11 - 1) - beta * (alpha11 - 1) - 1;
            double c = alpha;
            double b24ac = b * b - 4 * a * c;

            if (a == 0 || b24ac < 0)
                return new double[] {-1, -1}; // TODO: Warn.

            double q = -0.5 * (b + (b != 0 ? Math.signum(b) : 1) * Math.sqrt(b24ac));
            u = q / a;
            double ualt = c / q;
            u = Math.abs(u) <= Math.abs(ualt) ? u : ualt;
            if (u < 0 || u > 1)
                u = c / q;

            v = u * (beta11 - 1) + 1;
            v = Math.abs(v) >= eps ? beta / v : -1;
        }

        return new double[] {u, v};
    }

    public double[] getBilinearCoords(Vec4 point) {
        double[] w = this.getBarycentricCoords(point);
        return this.getBilinearCoords(w[1], w[2]);
    }

    public double[] invertBilinear(Vec4 U) {
        return BarycentricQuadrilateral.invertBilinear(U, this.p00, this.p10, this.p11, this.p01);
    }
}