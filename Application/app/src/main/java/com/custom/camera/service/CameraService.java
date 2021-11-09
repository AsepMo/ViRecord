package com.custom.camera.service;

import java.io.IOException;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import com.custom.camera.R;
import com.custom.camera.engine.app.connections.JpegFactory;
import com.custom.camera.engine.widget.MjpegServer;

public class CameraService extends Service {
    
    public static String TAG = CameraService.class.getSimpleName();
    private LinearLayout mOverlay = null;
    private SurfaceView mSurfaceView;
    
    private Camera mCamera;
    private MjpegServer mMjpegServer;
    SharedPreferences mSharedPreferences;
    
    private String mPort;
    
    
    public CameraService() {
    }
    
    @Override
    public void onCreate() {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(CameraService.this);
        if (!initialize()) {
            Toast.makeText(this, "Can not initialize parameters", Toast.LENGTH_LONG).show();
        }
        SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
            public void surfaceCreated(SurfaceHolder holder) {
                Log.v(TAG, "surfaceCreated()");
                
                int cameraId;
                int previewWidth;
                int previewHeight;
                int rangeMin;
                int rangeMax;
                int quality;
                int port;
                
                String cameraIdString = mSharedPreferences.getString("settings_camera", null);       
                String previewSizeString = mSharedPreferences.getString("settings_size", null);       
                String rangeString = mSharedPreferences.getString("settings_range", null);
                String qualityString = mSharedPreferences.getString("settings_quality", "50");
                String portString = mSharedPreferences.getString("settings_port", "8080");
                
                // if failed, it means settings is broken.
                assert(cameraIdString != null && previewSizeString != null && rangeString != null);
                
                int xIndex = previewSizeString.indexOf("x");
                int tildeIndex = rangeString.indexOf("~");
                
                // if failed, it means settings is broken.
                assert(xIndex > 0 && tildeIndex > 0);
                
                try {
                    cameraId = Integer.parseInt(cameraIdString);
                    
                    previewWidth = Integer.parseInt(previewSizeString.substring(0, xIndex - 1));
                    previewHeight = Integer.parseInt(previewSizeString.substring(xIndex + 2));
                    
                    rangeMin = Integer.parseInt(rangeString.substring(0, tildeIndex - 1));
                    rangeMax = Integer.parseInt(rangeString.substring(tildeIndex + 2));
                    
                    quality = Integer.parseInt(qualityString);
                    port = Integer.parseInt(portString);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Settings is broken");
                    Toast.makeText(CameraService.this, "Settings is broken", Toast.LENGTH_SHORT).show();
                    
                    stopSelf();
                    return;
                }
                
                mCamera = Camera.open(cameraId);
                if (mCamera == null) {
                    Log.v(TAG, "Can't open camera" + cameraId);
                    
                    Toast.makeText(CameraService.this, getString(R.string.camera_cannot_open),
                            Toast.LENGTH_SHORT).show();
                    stopSelf();
                    
                    return;
                }
                
                try {
                    mCamera.setPreviewDisplay(holder);
                } catch (IOException e) {
                    Log.v(TAG, "SurfaceHolder is not available");
                    
                    Toast.makeText(CameraService.this, "SurfaceHolder is not available",
                            Toast.LENGTH_SHORT).show();
                    stopSelf();
                    
                    return;
                }
                
                Parameters parameters = mCamera.getParameters();
                parameters.setPreviewSize(previewWidth, previewHeight);
                parameters.setPreviewFpsRange(rangeMin, rangeMax);
                mCamera.setParameters(parameters);
                mCamera.startPreview();
                
                JpegFactory jpegFactory = new JpegFactory(previewWidth, previewHeight, quality);                     
                mCamera.setPreviewCallback(jpegFactory);
                
                mMjpegServer = new MjpegServer(jpegFactory);
                try {
                    mMjpegServer.start(port);
                } catch (IOException e) {
                    String message = "Port: " + port + " is not available";
                    Log.v(TAG, message);
                    
                    Toast.makeText(CameraService.this, message, Toast.LENGTH_SHORT).show();
                    stopSelf();
                }
                
                Toast.makeText(CameraService.this, "Port: " + port, Toast.LENGTH_SHORT).show();
            }

            public void surfaceChanged(SurfaceHolder holder, int format, int width,
                    int height) {
                Log.v(TAG, "surfaceChanged()");
            }

            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.v(TAG, "surfaceDestroyed()");
            }
        };
        
        createOverlay();
        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        surfaceHolder.addCallback(callback);
        
        mPort = PreferenceManager.getDefaultSharedPreferences(this).getString("settings_port", "8080");

        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();
        
        
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        
        // We want BackgroundService.this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        // mNM.cancel(NOTIFICATION);
        
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
        }
        
        destroyOverlay();
        
        if (mMjpegServer != null) {
            mMjpegServer.close();
        } getExternalCacheDir();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void showNotification() {
        // In BackgroundService.this sample, we'll use the same text for the ticker and the expanded notification
        // CharSequence text = getText(R.string.service_started);
        CharSequence text = "View webcam at " + getIpAddr() + ":" + mPort;

        // Set the icon, scrolling text and timestamp
        // Notification notification = new Notification(R.drawable.ic_stat_webcam, text, System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects BackgroundService.this notification
        // PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        // notification.setLatestEventInfo(this, getText(R.string.app_name), text, contentIntent);

        // Send the notification.
        // startForeground( R.string.service_started, notification);
//        showNotification();

    }
    
    public String getIpAddr() {
           WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
           WifiInfo wifiInfo = wifiManager.getConnectionInfo();
           int ip = wifiInfo.getIpAddress();

           String ipString = String.format(
                   "%d.%d.%d.%d",
                   (ip & 0xff),
                   (ip >> 8 & 0xff),
                   (ip >> 16 & 0xff),
                   (ip >> 24 & 0xff));

        Log.v(TAG, ipString);

           return ipString;
        }
    
    /**
     * Create a surface view overlay (for the camera's preview surface).
     */
    private void createOverlay() {
        assert (mOverlay == null);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(4, 4,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,  // technically automatically set by FLAG_NOT_FOCUSABLE
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.BOTTOM;

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        mOverlay = (LinearLayout) inflater.inflate(R.layout.layout_camera_service, null);
        mSurfaceView = (SurfaceView) mOverlay.findViewById(R.id.surface_view);

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.addView(mOverlay, params);
    }
    
    private void destroyOverlay() {
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.removeView(mOverlay);
    }
    
    private boolean initialize() {
        
        boolean firstRun = !mSharedPreferences.contains("settings_photo_url");
        if (firstRun) {
            Log.v(TAG, "First run");

            SharedPreferences.Editor editor = mSharedPreferences.edit();

            int cameraNumber = Camera.getNumberOfCameras();
            Log.v(TAG, "Camera number: " + cameraNumber);

            /*
             * Get camera name set 
             */
            TreeSet<String> cameraNameSet = new TreeSet<String>();
            if (cameraNumber == 1) {
                cameraNameSet.add("back");
            } else if (cameraNumber == 2) {
                cameraNameSet.add("back");
                cameraNameSet.add("front");
            } else if (cameraNumber > 2) {           // rarely happen
                for (int id = 0; id < cameraNumber; id++) {
                    cameraNameSet.add(String.valueOf(id));
                }
            } else {                                 // no camera available
                Log.v(TAG, "No camera available");
                Toast.makeText(this, "No camera available", Toast.LENGTH_SHORT).show();

                return false;
            }

            /* 
             * Get camera id set
             */
            String[] cameraIds = new String[cameraNumber];
            TreeSet<String> cameraIdSet = new TreeSet<String>();
            for (int id = 0; id < cameraNumber; id++) {
                cameraIdSet.add(String.valueOf(id));
            }

            /*
             * Save camera name set and id set
             */
            editor.putStringSet("camera_name_set", cameraNameSet);
            editor.putStringSet("camera_id_set", cameraIdSet);

            /*
             * Get and save camera parameters
             */
            for (int id = 0; id < cameraNumber; id++) {
                Camera camera = Camera.open(id);
                if (camera == null) {
                    String msg = "Camera " + id + " is not available";
                    Log.v(TAG, msg);
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

                    return false;
                }

                Parameters parameters = camera.getParameters();

                /*
                 * Get and save preview / mJPEG stream resolution sizes
                 */
                List<Size> sizes = parameters.getSupportedPreviewSizes();

                TreeSet<String> sizeSet = new TreeSet<String>(new Comparator<String>() {
                        @Override
                        public int compare(String s1, String s2) {
                            int spaceIndex1 = s1.indexOf(" ");
                            int spaceIndex2 = s2.indexOf(" ");
                            int width1 = Integer.parseInt(s1.substring(0, spaceIndex1));
                            int width2 = Integer.parseInt(s2.substring(0, spaceIndex2));

                            return width2 - width1;
                        }
                    });
                for (Size size : sizes) {
                    sizeSet.add(size.width + " x " + size.height);
                }
                editor.putStringSet("preview_sizes_" + id, sizeSet);

                Log.v(TAG, "Stream Resolutions: ");
                Log.v(TAG, sizeSet.toString());

                /*
                 * Set default preview size, use camera 0
                 */
                if (id == 0) {
                    Log.v(TAG, "Set default preview size");

                    Size defaultSize = parameters.getPreviewSize();
                    editor.putString("settings_size", defaultSize.width + " x " + defaultSize.height);
                }


                /*
                 * Get and save CAPTURE sizes
                 */
                sizes = parameters.getSupportedPictureSizes();
                sizeSet = new TreeSet<String>(new Comparator<String>() {
                        @Override
                        public int compare(String s1, String s2) {
                            int spaceIndex1 = s1.indexOf(" ");
                            int spaceIndex2 = s2.indexOf(" ");
                            int width1 = Integer.parseInt(s1.substring(0, spaceIndex1));
                            int width2 = Integer.parseInt(s2.substring(0, spaceIndex2));

                            return width2 - width1;
                        }
                    });
                for (Size size : sizes) {
                    sizeSet.add(size.width + " x " + size.height);
                }
                Log.v(TAG, "Photo Resolutions: ");
                editor.putStringSet("photo_sizes_" + id, sizeSet);

                Log.v(TAG, sizeSet.toString());

                /*
                 * Set default preview size, use camera 0
                 */
                if (id == 0) {
                    Log.v(TAG, "Set default picture size");

                    Size defaultSize = parameters.getPictureSize();
                    editor.putString("settings_photo_size", defaultSize.width + " x " + defaultSize.height);
                }

                /*
                 * Get and save preview FPS range
                 */
                List<int[]> ranges = parameters.getSupportedPreviewFpsRange();
                TreeSet<String> rangeSet = new TreeSet<String>();
                for (int[] range : ranges) {
                    rangeSet.add(range[0] + " ~ " + range[1]);
                }
                editor.putStringSet("preview_ranges_" + id, rangeSet);

                if (id == 0) {
                    Log.v(TAG, "Set default fps range");

                    int[] defaultRange = new int[2];
                    parameters.getPreviewFpsRange(defaultRange);
                    editor.putString("settings_range", defaultRange[0] + " ~ " + defaultRange[1]);
                }

                camera.release();

            }

            editor.putString("settings_camera", "0");
            editor.commit();
        }

        return true;
    }
}
