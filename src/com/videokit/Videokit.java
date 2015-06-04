package com.videokit;

public final class Videokit {

  static {
    System.loadLibrary("videokit");
  }

  public native int run(String[] args);
  public native void stop();
  public native String getMetaData(String filepath, String key);

}
