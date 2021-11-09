package com.custom.camera;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;


import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import com.custom.camera.service.CameraService;

public class MainActivity extends AppCompatActivity implements ActionBar.OnNavigationListener, DemoCameraFragment.Contract {
    private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
    private static final String STATE_SINGLE_SHOT = "single_shot";
    private static final String STATE_LOCK_TO_LANDSCAPE = "lock_to_landscape";
    private static final int CONTENT_REQUEST = 1337;
    private DemoCameraFragment std=null;
    private DemoCameraFragment ffc=null;
    private DemoCameraFragment current=null;
    private boolean hasTwoCameras=(Camera.getNumberOfCameras() > 1);
    private boolean singleShot=false;
    private boolean isLockedToLandscape=false;
    
    SharedPreferences mSharedPreferences;
    
    public static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_application);

		Toolbar toolbar=(Toolbar)findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

        if (hasTwoCameras) {
            final ActionBar actionBar = getSupportActionBar();

            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(actionBar.getThemedContext(),
                                                R.array.nav,
                                                android.R.layout.simple_list_item_1);

            actionBar.setListNavigationCallbacks(adapter, this);
        } else {
            current = DemoCameraFragment.newInstance(false);

            getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, current).commit();
        }
        
        
        
                      
    }

    @Override
    protected void onResume() {
        super.onResume();                   
    }

    
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (hasTwoCameras) {
            if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
                getSupportActionBar().setSelectedNavigationItem(savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
            }
        }

        setSingleShotMode(savedInstanceState.getBoolean(STATE_SINGLE_SHOT));
        isLockedToLandscape = savedInstanceState.getBoolean(STATE_LOCK_TO_LANDSCAPE);

        if (current != null) {
            current.lockToLandscape(isLockedToLandscape);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (hasTwoCameras) {
            outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getSupportActionBar().getSelectedNavigationIndex());
        }

        outState.putBoolean(STATE_SINGLE_SHOT, isSingleShotMode());
        outState.putBoolean(STATE_LOCK_TO_LANDSCAPE, isLockedToLandscape);
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        if (position == 0) {
            if (std == null) {
                std = DemoCameraFragment.newInstance(false);
            }

            current = std;
        } else {
            if (ffc == null) {
                ffc = DemoCameraFragment.newInstance(true);
            }

            current = ffc;
        }

        getSupportFragmentManager().beginTransaction()
            .replace(R.id.content_frame, current).commit();

        findViewById(R.id.root_view).post(new Runnable() {
                @Override
                public void run() {
                    current.lockToLandscape(isLockedToLandscape);
                }
            });

        return(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(this).inflate(R.menu.main, menu);

        menu.findItem(R.id.landscape).setChecked(isLockedToLandscape);

        return(super.onCreateOptionsMenu(menu));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.content) {
            /*Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            File output=new File(dir, "CameraContentDemo.jpeg");

            i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(output));

            startActivityForResult(i, CONTENT_REQUEST);*/
            startService(new Intent(this, CameraService.class)); 
        } else if (item.getItemId() == R.id.landscape) {
            item.setChecked(!item.isChecked());
            current.lockToLandscape(item.isChecked());
            isLockedToLandscape = item.isChecked();
        } else if (item.getItemId() == R.id.fullscreen) {
            startActivity(new Intent(this, FullScreenActivity.class));
        }

        return(super.onOptionsItemSelected(item));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == CONTENT_REQUEST) {
            if (resultCode == RESULT_OK) {
                // do nothing
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_CAMERA && current != null
            && !current.isSingleShotProcessing()) {
            current.takePicture();

            return(true);
        }

        return(super.onKeyDown(keyCode, event));
    }

    @Override
    public boolean isSingleShotMode() {
        return(singleShot);
    }

    @Override
    public void setSingleShotMode(boolean mode) {
        singleShot = mode;
    }

    private boolean ismJPEGServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ((CameraService.class.getName()).equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    private boolean initialize() {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        
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
