/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.util;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.*;
import gov.nasa.worldwind.terrain.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;

@Ignore
@RunWith(JUnit4.class)
public class HighResolutionTerrainTest
{
    private final int GRID_SIZE = 50;

    @Test
    public void testConsistencyOfBulkPositions()
    {
        Sector sector = Sector.boundingSector(
            Position.fromDegrees(34.42391, -119.75557),
            Position.fromDegrees(34.51959, -119.63282));

        ArrayList<Position> referencePositions = generateReferenceLocations(sector, GRID_SIZE, GRID_SIZE);

        final ConcurrentHashMap<Position, Intersection[]> currentIntersections
            = new ConcurrentHashMap<>();
        final HashMap<Position, Intersection[]> previousIntersections = new HashMap<>();

        Globe globe = new Earth();
        CompoundElevationModel cem = (CompoundElevationModel) globe.getElevationModel();
        cem.addElevationModel(EllipsoidalGlobe.makeElevationModel("", "config/Earth/EarthMergedElevationModel.xml"));

        for (int i = 0; i < 5; i++)
        {
            HighResolutionTerrain hrt = new HighResolutionTerrain(globe, sector, null, 1.0);
            try
            {
                hrt.intersect(referencePositions, new HighResolutionTerrain.IntersectionCallback()
                {
                    @Override
                    public void intersection(Position pA, Position pB, Intersection[] intersections)
                    {
                        currentIntersections.put(pB, intersections);
                    }

                    public void exception(Exception e)
                    {
                        e.printStackTrace();
                    }
                });
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            if (!previousIntersections.isEmpty())
            {
                if (currentIntersections.size() != previousIntersections.size())
                {
                    String msg = "Different intersection counts: current %d, previous %d\n";
                    currentIntersections.size();
                    previousIntersections.size();
                    fail(msg);
                }

                int item = -1;
                for (Map.Entry<Position, Intersection[]> entry : currentIntersections.entrySet())
                {
                    Position position = entry.getKey();
                    ++item;
                    Intersection[] currentIntersection = entry.getValue();
                    Intersection[] previousIntersection = previousIntersections.get(position);

                    if (previousIntersection == null)
                    {
                        fail("Missing intersection: " + item + ", " + position);
                    }

                    if (currentIntersection.length != previousIntersection.length)
                    {
                        fail("Different number of intersections: " + item + ", " + currentIntersection.length + ", " +
                            previousIntersection.length + ", " + position);
                    }

                    for (int j = 0; j < currentIntersection.length; j++)
                    {
                        if (!currentIntersection[j].equals(previousIntersection[j]))
                        {
                            String msg = "Different intersection points: " + j + ", "
                                + currentIntersection[j] + ", " + previousIntersection[j] + ", " + position;
                            assertEquals(msg, currentIntersection[j], previousIntersection[j]);
                        }
                    }
                }
            }

            if (i == 1)
                previousIntersections.putAll(currentIntersections);

            currentIntersections.clear();
        }
    }

    @Test
    public void testConsistencyOfIndividualPositions()
    {
        Sector sector = Sector.boundingSector(
            Position.fromDegrees(34.42391, -119.75557),
            Position.fromDegrees(34.51959, -119.63282));

        ArrayList<Position> referencePositions = generateReferenceLocations(sector, GRID_SIZE, GRID_SIZE);

        final ConcurrentHashMap<Position, Intersection[]> currentIntersections
            = new ConcurrentHashMap<>();
        final HashMap<Position, Intersection[]> previousIntersections = new HashMap<>();

        Globe globe = new Earth();
//            CompoundElevationModel cem = (CompoundElevationModel) globe.getElevationModel();
//            cem.addElevationModel(EllipsoidalGlobe.makeElevationModel(
//                "", "config/Earth/EarthMergedElevationModel.xml"));

        for (int i = 0; i < 5; i++)
        {
            ThreadPoolExecutor threadPool = new ThreadPoolExecutor(10, 10, 200,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

            try
            {
                final HighResolutionTerrain hrt = new HighResolutionTerrain(globe, sector, null, 1.0);

                for (int j = 0; j < referencePositions.size(); j += 2)
                {
                    final Position pA = referencePositions.get(j);
                    final Position pB = referencePositions.get(j + 1);

                    threadPool.execute(() -> {
                        Intersection[] intersections = hrt.intersect(pA, pB);
                        if (intersections != null && intersections.length > 0)
                        {
                            currentIntersections.put(pB, intersections);
                        }
                    });
                }

                threadPool.shutdown();
                threadPool.awaitTermination(120, TimeUnit.MINUTES);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            if (!previousIntersections.isEmpty())
            {
                if (currentIntersections.size() != previousIntersections.size())
                {
                    String msg = "Different intersection counts: current %d, previous %d\n";
                    currentIntersections.size();
                    previousIntersections.size();
                    fail(msg);
                }

                int item = -1;
                for (Map.Entry<Position, Intersection[]> entry : currentIntersections.entrySet())
                {
                    Position position = entry.getKey();
                    ++item;
                    Intersection[] currentIntersection = entry.getValue();
                    Intersection[] previousIntersection = previousIntersections.get(position);

                    if (previousIntersection == null)
                    {
                        fail("Missing intersection: " + item + ", " + position);
                    }

                    if (currentIntersection.length != previousIntersection.length)
                    {
                        fail("Different number of intersections: " + item + ", " + currentIntersection.length + ", " +
                            previousIntersection.length + ", " + position);
                    }

                    for (int j = 0; j < currentIntersection.length; j++)
                    {
                        if (!currentIntersection[j].equals(previousIntersection[j]))
                        {
                            String msg = "Different intersection points: " + j + ", "
                                + currentIntersection[j] + ", " + previousIntersection[j] + ", " + position;
                            assertEquals(msg, currentIntersection[j], previousIntersection[j]);
                        }
                    }
                }
            }

            if (i == 1)
                previousIntersections.putAll(currentIntersections);

            currentIntersections.clear();
        }
    }

    private static ArrayList<Position> generateReferenceLocations(Sector sector, int numLats, int numLons)
    {
        int decimalPlaces = 5;
        ArrayList<Position> locations = new ArrayList<>();
        double dLat = (sector.latMax - sector.latMin) / (numLats - 1);
        double dLon = (sector.lonMax - sector.lonMin) / (numLons - 1);

        Position p0 = Position.fromDegrees(
            round(decimalPlaces, sector.latMin),
            round(decimalPlaces, sector.lonMin), 0);
        for (int j = 1; j < numLats; j++)
        {
            double lat = sector.latMin + j * dLat;

            for (int i = 0; i < numLons; i++)
            {
                double lon = sector.lonMin + i * dLon;

                locations.add(p0);
                locations.add(Position.fromDegrees(round(decimalPlaces, lat), round(decimalPlaces, lon), 0));
            }
        }

        return locations;
    }

    private static double round(int decimalPlaces, double value)
    {
        double scale = Math.pow(10, decimalPlaces);
        return Math.round(value * scale) / scale;
    }
}
