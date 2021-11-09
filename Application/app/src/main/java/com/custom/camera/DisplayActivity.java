package com.custom.camera;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

public class DisplayActivity extends AppCompatActivity {
    public static String TAG = DisplayActivity.class.getSimpleName();
    public static byte[] imageToShow = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_display);

        Toolbar toolbar=(Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        ImageView iv = (ImageView)findViewById(R.id.image_display);
        
        if (imageToShow == null) {
            Toast.makeText(this, R.string.no_image, Toast.LENGTH_LONG).show();
            finish();
        }
        else {
            BitmapFactory.Options opts=new BitmapFactory.Options();

            opts.inPurgeable=true;
            opts.inInputShareable=true;
            opts.inMutable=false;
            opts.inSampleSize=2;

            iv.setImageBitmap(BitmapFactory.decodeByteArray(imageToShow,0, imageToShow.length,opts));
                                                        
            imageToShow = null;
        }
    }


}
