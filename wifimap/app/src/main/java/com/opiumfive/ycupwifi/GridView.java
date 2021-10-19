package com.opiumfive.ycupwifi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import android.view.View;

public class GridView extends View {

    private final MainData mainData;
    private final Paint paint;
    private final Paint textPaint;
    private Location currentLocation;
    private boolean showNumbers = false;

    public GridView(Context context, MainData mainData) {
        super(context);

        this.mainData = mainData;
        this.paint = new Paint();

        this.paint.setStyle(Paint.Style.FILL);

        this.textPaint = new Paint();

        this.textPaint.setStyle(Paint.Style.FILL);
        this.textPaint.setTextSize(18);
        this.textPaint.setColor(Color.BLACK);
    }

    public boolean isShowNumbers() {
        return showNumbers;
    }

    public void setShowNumbers(boolean showNumbers) {
        this.showNumbers = showNumbers;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paint.setColor(Color.rgb(255, 255, 255));
        canvas.drawPaint(paint);

        if (mainData == null || mainData.getGridInfo() == null)
            return;

        int width = getWidth();
        int height = getHeight();

        int rectangleWidth = (int) (width / (double) mainData.getGridInfo().getColumnsCount());
        int rectangleHeight = (int) (height / (double) mainData.getGridInfo().getRowsCount());

        for (int row = 0; row < mainData.getGridInfo().getRowsCount(); ++row)
            for (int column = 0; column < mainData.getGridInfo().getColumnsCount(); ++column) {

                SignalGrid.SignalInfo signalInfo = mainData.getSignalGrids().getSignalInfo(new CellPosition(row, column));

                paint.setColor(getMappedColor(signalInfo.getAverageSignalLevel()));

                // flip rows to have north on top:
                int flippedRow = mainData.getGridInfo().getRowsCount() - 1 - row;

                canvas.drawRect(
                        rectangleWidth * column,
                        rectangleHeight * flippedRow,
                        rectangleWidth * column + rectangleWidth,
                        rectangleHeight * flippedRow + rectangleHeight,
                        paint);

                if (showNumbers && signalInfo.getAverageSignalLevel() != -100) {
                    canvas.drawText(String.valueOf((int) signalInfo.getAverageSignalLevel()), rectangleWidth * column + rectangleWidth / 4, rectangleHeight * flippedRow + rectangleHeight / 3, textPaint);
                }
            }

        if (currentLocation != null) {
            CellPosition currentPosition = mainData.getGridInfo().computeCellPosition(currentLocation);

            int currentRow = currentPosition.getRow();
            int currentColumn = currentPosition.getColumn();

            // flip rows to have north on top:
            currentRow = mainData.getGridInfo().getRowsCount() - 1 - currentRow;

            paint.setColor(Color.rgb(0, 105, 191));

            canvas.drawCircle(
                    rectangleWidth * currentColumn + rectangleWidth / 2,
                    rectangleHeight * currentRow + rectangleHeight / 2,
                    3,
                    paint);
        }
    }

    public void update(Location currentLocation) {

        if (currentLocation != null)
            this.currentLocation = currentLocation;

        invalidate();
    }

    private int getMappedColor(double signalLevel) {
        int redComplement;

        if (signalLevel <= -100.0)
            redComplement = 255;

        else if (signalLevel >= -30.0)
            redComplement = 0;

        else
            redComplement = (int) ((-20 / 7.0) * signalLevel - (600 / 7.0));

        return Color.rgb(redComplement, 255, redComplement);
    }
}
