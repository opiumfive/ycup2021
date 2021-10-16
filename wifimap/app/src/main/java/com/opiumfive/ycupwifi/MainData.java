package com.opiumfive.ycupwifi;

import android.net.wifi.ScanResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainData {
    private GridInfo gridInfo;
    private SignalGrid signalGrids;
    private boolean started = false;

    public MainData() {

    }

    public boolean isStarted() {
        return started;
    }

    public void startMeasurement(Location location) {
        started = true;
        gridInfo = new GridInfo(location);
        signalGrids = new SignalGrid(gridInfo);

    }

    // the only data-updating method:
    public boolean addMeasurement(Location location, int strength) {
        if (gridInfo == null)
            return false;


        signalGrids.addMeasurement(location, strength);


        return true;
    }

    public GridInfo getGridInfo() {
        return gridInfo;
    }

    public SignalGrid getSignalGrids() {
        return signalGrids;
    }
}
