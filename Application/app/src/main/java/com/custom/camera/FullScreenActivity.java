package com.custom.camera;


import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

public class FullScreenActivity extends AppCompatActivity implements DemoCameraFragment.Contract {
  
    public static String TAG = FullScreenActivity.class.getSimpleName();
    DemoCameraFragment current = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_fullscreen);
        // hide actionbar
        getSupportActionBar().hide();
        // hide navigation bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
        current = DemoCameraFragment.newInstance(false);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.camera_preview, current).commit();

    }

    @Override
    public boolean isSingleShotMode() {
        return(false);
    }

    @Override
    public void setSingleShotMode(boolean mode) {
        // hardcoded, unused
    }

    public void takePicture(View v) {
        current.takeSimplePicture();
    }
    


}
