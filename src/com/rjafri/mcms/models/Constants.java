package com.rjafri.mcms.models;

import android.graphics.Typeface;

public class Constants {

	public static final int SOURCE_PHOTO = 0;
	public static final int SOURCE_VIDEO = 1;
	
	public static final int MIN_CONTENTS = 6;
	public static final int MAX_CONTENTS = 24;
	public static final float STORY_DURATION = 60.f;
	public static final int STORY_DURATION_MS = 60000;
	public static final float TRANSITION_DURATION = 1.f;
	public static final int TRANSITION_DURATION_MS = 1000;
	
	public static final int STORY_WIDTH = 640;
	public static final int STORY_HEIGHT = 480;
	public static final int THUMB_WIDTH = 256;
	public static final int THUMB_HEIGHT = 166;
	
	public static final float OVERLAY_FONT_SIZE = 40.f;
	
	public static final int OVERLAY_LINE_LENGTH = 50;
	public static final int OVERLAY_MAX_LINES = 2;
	
	public static String workDirectoryPath = "";
	public static String storiesDirectoryPath = "";
	public static String defaultFontPath = "";
	
	public static Typeface myFontTypeface = null;
	public static Typeface myFontBoldTypeface = null;

}
