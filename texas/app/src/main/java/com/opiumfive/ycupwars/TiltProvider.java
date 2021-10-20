package com.opiumfive.ycupwars;

import android.content.Context;
import android.hardware.SensorManager;

import org.hitlabnz.sensor_fusion_demo.HardwareChecker;
import org.hitlabnz.sensor_fusion_demo.orientationProvider.ImprovedOrientationSensor2Provider;
import org.hitlabnz.sensor_fusion_demo.representation.Quaternion;

public class TiltProvider {

    private ImprovedOrientationSensor2Provider orientationProvider;
    private Quaternion quaternion = new Quaternion();
    private TiltListener tiltListener;
    private GetTiltDaemon daemon;

    public interface TiltListener {
        void sync(Quaternion angle);
    }

    public TiltProvider(Context context) {
        SensorManager sensorManager = (SensorManager) context.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) {
            return;
        } else {
            HardwareChecker checker = new HardwareChecker(sensorManager);
            if(!checker.IsGyroscopeAvailable()) return;
        }
        this.orientationProvider = new ImprovedOrientationSensor2Provider(sensorManager);
    }

    public void start(TiltListener tiltListener) {
        if (orientationProvider != null) {
            orientationProvider.start();
            this.tiltListener = tiltListener;
            startThread();
        }
    }

    public void stop() {
        if (orientationProvider != null) {
            orientationProvider.stop();
            tiltListener = null;
            stopThread();
        }
    }

    private void startThread() {
        daemon = new GetTiltDaemon(this, tiltListener);
        daemon.start();
    }

    private void stopThread() {
        if (daemon != null && daemon.running) {
            daemon.stopThread();
            daemon.interrupt();
            daemon = null;
        }
    }

    public Quaternion getRotation() {
        if (orientationProvider != null) {
            orientationProvider.getQuaternion(quaternion);
            return quaternion;
        }
        return null;
    }

    static class GetTiltDaemon extends Thread {

        private boolean running = true;
        private TiltProvider tiltProvider;
        private TiltListener tiltListener;
        private long time = 0;
        private long lastResetTime = 0;

        public GetTiltDaemon(TiltProvider tiltProvider, TiltListener tiltListener) {
            this.tiltProvider = tiltProvider;
            this.tiltListener = tiltListener;
        }

        @Override
        public void run() {
            while (running) {
                time = System.nanoTime();
                if (lastResetTime == 0) lastResetTime = System.nanoTime();
                if (tiltListener == null || tiltProvider == null) {
                    running = false;
                } else {
                    Quaternion angles = tiltProvider.getRotation();
                    if (angles == null) continue;
                    tiltListener.sync(angles);
                }
                while(running && System.nanoTime() - time < 16000000); // 60hz
            }
        }

        void stopThread() {
            running = false;
        }
    }
}
