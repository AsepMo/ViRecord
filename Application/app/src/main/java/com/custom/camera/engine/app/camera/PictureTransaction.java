package com.custom.camera.engine.app.camera;

import android.hardware.Camera;

public class PictureTransaction implements Camera.ShutterCallback {
  CameraHost host=null;
  boolean needBitmap=false;
  boolean needByteArray=true;
  private Object tag=null;
  boolean mirrorFFC=false;
  boolean useSingleShotMode=false;
  int displayOrientation=0;
  String flashMode=null;
  CameraView cameraView=null;

  public PictureTransaction(CameraHost host) {
    this.host=host;
  }

  public PictureTransaction needBitmap(boolean needBitmap) {
    this.needBitmap=needBitmap;

    return(this);
  }

  public PictureTransaction needByteArray(boolean needByteArray) {
    this.needByteArray=needByteArray;

    return(this);
  }

  public Object getTag() {
    return(tag);
  }

  public PictureTransaction tag(Object tag) {
    this.tag=tag;

    return(this);
  }

  boolean useSingleShotMode() {
    return(useSingleShotMode || host.useSingleShotMode());
  }

  boolean mirrorFFC() {
    return(mirrorFFC || host.mirrorFFC());
  }

  public PictureTransaction useSingleShotMode(boolean useSingleShotMode) {
    this.useSingleShotMode=useSingleShotMode;

    return(this);
  }

  public PictureTransaction mirrorFFC(boolean mirrorFFC) {
    this.mirrorFFC=mirrorFFC;

    return(this);
  }

  public PictureTransaction flashMode(String flashMode) {
    this.flashMode=flashMode;

    return(this);
  }

  PictureTransaction displayOrientation(int displayOrientation) {
    this.displayOrientation=displayOrientation;

    return(this);
  }

  @Override
  public void onShutter() {
    Camera.ShutterCallback cb=host.getShutterCallback();

    if (cb != null) {
      cb.onShutter();
    }
  }
}
