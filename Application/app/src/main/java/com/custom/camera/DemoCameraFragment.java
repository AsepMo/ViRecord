package com.custom.camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.custom.camera.MainActivity;
import com.custom.camera.engine.app.camera.CameraFragment;
import com.custom.camera.engine.app.camera.CameraHost;
import com.custom.camera.engine.app.camera.CameraUtils;
import com.custom.camera.engine.app.camera.SimpleCameraHost;
import com.custom.camera.engine.app.camera.PictureTransaction;

public class DemoCameraFragment extends CameraFragment implements OnSeekBarChangeListener {

    private MainActivity mActivity;
    private static final String KEY_USE_FFC = "com.custom.camera.USE_FFC";
    private MenuItem singleShotItem = null;
    private MenuItem autoFocusItem = null;
    private MenuItem takePictureItem=null;
    private MenuItem flashItem=null;
    private MenuItem recordItem=null;
    private MenuItem stopRecordItem=null;
    private MenuItem mirrorFFC=null;
    private boolean singleShotProcessing=false;
    private SeekBar zoom=null;
    private long lastFaceToast=0L;
    String flashMode=null;

    public static DemoCameraFragment newInstance(boolean useFFC) {
        DemoCameraFragment f=new DemoCameraFragment();
        Bundle args=new Bundle();

        args.putBoolean(KEY_USE_FFC, useFFC);
        f.setArguments(args);

        return(f);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        setHasOptionsMenu(true);

        SimpleCameraHost.Builder builder =  new SimpleCameraHost.Builder(new DemoCameraHost(getActivity()));

        setCameraHost(builder.useFullBleedPreview(true).build());
    }

    @Override
    public View onCreateView(LayoutInflater inflater,ViewGroup container, Bundle savedInstanceState) {
                                                       
        View cameraView = super.onCreateView(inflater, container, savedInstanceState);
        View results = inflater.inflate(R.layout.layout_camera_fragment, container, false);

        ((ViewGroup)results.findViewById(R.id.camera)).addView(cameraView);
        zoom = (SeekBar)results.findViewById(R.id.zoom);
        zoom.setKeepScreenOn(true);

        setRecordingItemVisibility();

        return(results);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mActivity = (MainActivity)getActivity();
    }
    

    @Override
    public void onPause() {
        super.onPause();

        mActivity.supportInvalidateOptionsMenu();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.camera, menu);

        takePictureItem = menu.findItem(R.id.camera);
        singleShotItem = menu.findItem(R.id.single_shot);
        singleShotItem.setChecked(getContract().isSingleShotMode());
        autoFocusItem = menu.findItem(R.id.autofocus);
        flashItem = menu.findItem(R.id.flash);
        recordItem = menu.findItem(R.id.record);
        stopRecordItem = menu.findItem(R.id.stop);
        mirrorFFC = menu.findItem(R.id.mirror_ffc);

        if (isRecording()) {
            recordItem.setVisible(false);
            stopRecordItem.setVisible(true);
            takePictureItem.setVisible(false);
        }

        setRecordingItemVisibility();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.camera:
                takeSimplePicture();

                return(true);

            case R.id.record:
                try {
                    record();
                    mActivity.supportInvalidateOptionsMenu();
                } catch (Exception e) {
                    Log.e(getClass().getSimpleName(),
                          "Exception trying to record", e);
                    Toast.makeText(getActivity(), e.getMessage(),
                                   Toast.LENGTH_LONG).show();
                }

                return(true);

            case R.id.stop:
                try {
                    stopRecording();
                    mActivity.supportInvalidateOptionsMenu();
                } catch (Exception e) {
                    Log.e(getClass().getSimpleName(),
                          "Exception trying to stop recording", e);
                    Toast.makeText(getActivity(), e.getMessage(),
                                   Toast.LENGTH_LONG).show();
                }

                return(true);

            case R.id.autofocus:
                takePictureItem.setEnabled(false);
                autoFocus();

                return(true);

            case R.id.single_shot:
                item.setChecked(!item.isChecked());
                getContract().setSingleShotMode(item.isChecked());

                return(true);

            case R.id.show_zoom:
                item.setChecked(!item.isChecked());
                zoom.setVisibility(item.isChecked() ? View.VISIBLE : View.GONE);

                return(true);

            case R.id.flash:
            case R.id.mirror_ffc:
                item.setChecked(!item.isChecked());

                return(true);
        }

        return(super.onOptionsItemSelected(item));
    }

    boolean isSingleShotProcessing() {
        return(singleShotProcessing);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        if (fromUser) {
            zoom.setEnabled(false);
            zoomTo(zoom.getProgress()).onComplete(new Runnable() {
                    @Override
                    public void run() {
                        zoom.setEnabled(true);
                    }
                }).go();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // ignore
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // ignore
    }

    void setRecordingItemVisibility() {
        if (zoom != null && recordItem != null) {
            if (getDisplayOrientation() != 0
                && getDisplayOrientation() != 180) {
                recordItem.setVisible(false);
            }
        }
    }

    Contract getContract() {
        return((Contract)getActivity());
    }

    void takeSimplePicture() {
        if (singleShotItem != null && singleShotItem.isChecked()) {
            singleShotProcessing = true;
            takePictureItem.setEnabled(false);
        }

        PictureTransaction xact=new PictureTransaction(
            getCameraHost());

        if (flashItem != null && flashItem.isChecked()) {
            xact.flashMode(flashMode);
        }

        takePicture(xact);
    }

    interface Contract {
        boolean isSingleShotMode();

        void setSingleShotMode(boolean mode);
    }

    class DemoCameraHost extends SimpleCameraHost implements
    Camera.FaceDetectionListener {
        boolean supportsFaces=false;

        public DemoCameraHost(Context _ctxt) {
            super(_ctxt);
        }

        @Override
        public boolean useFrontFacingCamera() {
            if (getArguments() == null) {
                return(false);
            }

            return(getArguments().getBoolean(KEY_USE_FFC));
        }

        @Override
        public boolean useSingleShotMode() {
            if (singleShotItem == null) {
                return(false);
            }

            return(singleShotItem.isChecked());
        }

        @Override
        public void saveImage(PictureTransaction xact, byte[] image) {
            if (useSingleShotMode()) {
                singleShotProcessing = false;

                mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            takePictureItem.setEnabled(true);
                        }
                    });

                DisplayActivity.imageToShow = image;
                startActivity(new Intent(mActivity, DisplayActivity.class));
            } else {
                super.saveImage(xact, image);
            }
        }

        @Override
        public void autoFocusAvailable() {
            if (autoFocusItem != null) {
                autoFocusItem.setEnabled(true);

                if (supportsFaces)
                    startFaceDetection();
            }
        }

        @Override
        public void autoFocusUnavailable() {
            if (autoFocusItem != null) {
                stopFaceDetection();

                if (supportsFaces)
                    autoFocusItem.setEnabled(false);
            }
        }

        @Override
        public void onCameraFail(CameraHost.FailureReason reason) {
            super.onCameraFail(reason);

            Toast.makeText(mActivity,
                           "Sorry, but you cannot use the camera now!",
                           Toast.LENGTH_LONG).show();
        }

        @Override
        public Parameters adjustPreviewParameters(Parameters parameters) {
            flashMode =
                CameraUtils.findBestFlashModeMatch(parameters,
                                                   Camera.Parameters.FLASH_MODE_RED_EYE,
                                                   Camera.Parameters.FLASH_MODE_AUTO,
                                                   Camera.Parameters.FLASH_MODE_ON);

            if (doesZoomReallyWork() && parameters.getMaxZoom() > 0) {
                zoom.setMax(parameters.getMaxZoom());
                zoom.setOnSeekBarChangeListener(DemoCameraFragment.this);
            } else {
                zoom.setEnabled(false);
            }

            if (parameters.getMaxNumDetectedFaces() > 0) {
                supportsFaces = true;
            } else {
                Toast.makeText(mActivity,
                               "Face detection not available for this camera",
                               Toast.LENGTH_LONG).show();
            }

            return(super.adjustPreviewParameters(parameters));
        }

        @Override
        public void onFaceDetection(Face[] faces, Camera camera) {
            if (faces.length > 0) {
                long now=SystemClock.elapsedRealtime();

                if (now > lastFaceToast + 10000) {
                    Toast.makeText(mActivity, "I see your face!",
                                   Toast.LENGTH_LONG).show();
                    lastFaceToast = now;
                }
            }
        }

        @Override
        @TargetApi(16)
        public void onAutoFocus(boolean success, Camera camera) {
            super.onAutoFocus(success, camera);

            takePictureItem.setEnabled(true);
        }

        @Override
        public boolean mirrorFFC() {
            return(mirrorFFC.isChecked());
        }
    }
}
