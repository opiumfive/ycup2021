package com.opiumfive.ycupwifi;


import android.content.res.Resources;
import android.util.TypedValue;

public class GridInfo {
    private final double cellWidth; // as from east to west (in meters)
    private final double cellHeight; // as from north to south (in meters)

    private final int columnsCount;
    private final int rowsCount;

    // center point of the grid, mapping it to the position on the earth
    private final Location centerLocation;

    GridInfo(Location centerLocation) {

        columnsCount = 20;
        rowsCount = 20;

        cellWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, Resources.getSystem().getDisplayMetrics()) / columnsCount;
        cellHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, Resources.getSystem().getDisplayMetrics()) / rowsCount;

        this.centerLocation = centerLocation;
    }

    public Location getCenterLocation() {
        return centerLocation;
    }

    public double getCellWidth() {
        return cellWidth;
    }

    public double getCellHeight() {
        return cellHeight;
    }

    public int getColumnsCount() {
        return columnsCount;
    }

    public int getRowsCount() {
        return rowsCount;
    }

    public double getWidth() {
        return cellWidth * (double) columnsCount;
    }

    public double getHeight() {
        return cellHeight * (double) rowsCount;
    }

    public boolean containsCellPosition(CellPosition cellPosition) {
        return cellPosition.getRow() >= 0 && cellPosition.getRow() < rowsCount &&
                cellPosition.getColumn() >= 0 && cellPosition.getColumn() < columnsCount;
    }

    public CellPosition computeCellPosition(Location location) {
        Location centerLocation = getCenterLocation();

        double yOffset = cellHeight * GeographicalCalculator.InMeters.getNorthwardsDisplacement(centerLocation, location) * 2;
        double xOffset = cellWidth * GeographicalCalculator.InMeters.getEastwardsDisplacement(centerLocation, location) * 2;

        yOffset += getHeight() / 2;
        xOffset += getWidth() / 2;

        int row = (int) (yOffset / getRowsCount());
        int column = (int) (xOffset / getColumnsCount());

        return new CellPosition(row, column);
    }
}
