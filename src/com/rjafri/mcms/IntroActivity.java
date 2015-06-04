package com.rjafri.mcms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.Window;

import com.rjafri.mcms.models.Constants;

public class IntroActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_intro);
		
		try {
			PackageInfo info = getPackageManager().getPackageInfo("com.rjafri.mcms", PackageManager.GET_SIGNATURES);
			for (Signature signature : info.signatures) {
				MessageDigest md = MessageDigest.getInstance("SHA");
				md.update(signature.toByteArray());
				Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
			}
		} catch (NameNotFoundException e) {
		} catch (NoSuchAlgorithmException e) {
		}
		
		if (!Environment.MEDIA_MOUNTED.equalsIgnoreCase(Environment.getExternalStorageState())) {
			new AlertDialog.Builder(this)
			.setMessage("Please insert SD card and try again.")
			.setTitle("Error")
			.setNegativeButton("Close", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					IntroActivity.this.finish();
					System.exit(0);
				}
			})
			.create()
			.show();
			
			return;
		}
		
		File workDirectory = new File(Environment.getExternalStorageDirectory(), ".mcms_work");
		workDirectory.mkdirs();
		Constants.workDirectoryPath = workDirectory.getAbsolutePath();
		
		File storiesDirectory = new File(Environment.getExternalStorageDirectory(), "mcms_stories");
		storiesDirectory.mkdirs();
		Constants.storiesDirectoryPath = storiesDirectory.getAbsolutePath();
		
		Constants.myFontTypeface = Typeface.createFromAsset(getAssets(), "myriadpro_cond.ttf");
		Constants.myFontBoldTypeface = Typeface.createFromAsset(getAssets(), "myriadpro_boldcond.ttf");
		
		File fontFile = new File(Constants.workDirectoryPath, "myriadpro_cond.ttf");
		if (fontFile.exists())
			fontFile.delete();
		
		InputStream is = null;
		OutputStream os = null;
		try {
			is = getAssets().open("myriadpro_cond.ttf");
			os = new FileOutputStream(fontFile);
			copyFile(is, os);
			Constants.defaultFontPath = fontFile.getAbsolutePath();
		} catch (IOException e) {
			e.printStackTrace();
			new AlertDialog.Builder(this)
			.setMessage("Cannot copy the default font file.")
			.setTitle("Error")
			.setNegativeButton("Close", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					IntroActivity.this.finish();
					System.exit(0);
				}
			})
			.create()
			.show();
		} finally {
			try {
				if (is != null)
					is.close();
				if (os!= null)
					os.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	@Override
    public void onStart() {
    	super.onStart();
    	
    	// Moves to MainActivity after 2 sec.
    	new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				Intent mainIntent = new Intent(IntroActivity.this, MainSlideActivity.class);
				IntroActivity.this.startActivity(mainIntent);
				IntroActivity.this.overridePendingTransition(R.anim.fadein, R.anim.fadeout);
				finish();
			}
    		
    	}, 1000);
    }
	
	@Override
	public void onConfigurationChanged (Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
	
	private void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while ((read = in.read(buffer)) != -1) {
			out.write(buffer, 0, read);
		}
	}
	
}
