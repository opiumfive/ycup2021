package com.opiumfive.ycupwifi;



public class GeographicalCalculator {

    public static class InMeters {
        public static double getNorthwardsDisplacement(Location location1, Location location2) {
            return (location2.getY() - location1.getY());
        }

        public static double getEastwardsDisplacement(Location location1, Location location2) {
            return (location2.getX() - location1.getX());
        }
    }

}
