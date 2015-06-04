package com.videokit;

public class ColorConverter {

	static {
		System.loadLibrary("I420colorconvert");
	}
	
	public static final int COLOR_FormatYUV420Planar = 0x13;			// CodecCapabilities.COLOR_FormatYUV420Planar
	public static final int COLOR_FormatYUV420PackedPlanar = 0x14;		// CodecCapabilities.COLOR_FormatYUV420PackedPlanar
	public static final int COLOR_FormatYUV420SemiPlanar = 0x15;		// CodecCapabilities.COLOR_FormatYUV420SemiPlanar
	public static final int COLOR_FormatYUV420PackedSemiPlanar = 0x27;	// CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar
	
	public static native void setEncoderInputColorFormat(int colorFormat);
	public static native void setDecoderOutputColorFormat(int colorFormat);

}
