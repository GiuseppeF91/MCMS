package com.rjafri.mcms;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aretha.slidemenu.SlideMenu;
import com.rjafri.mcms.fragments.MyStoriesFragment;
import com.rjafri.mcms.fragments.ProfileFragment;
import com.rjafri.mcms.fragments.WriteStoryFragment;
import com.rjafri.mcms.fragments.WriteStoryNFragment;
import com.rjafri.mcms.models.Constants;

public class MainSlideActivity extends FragmentActivity implements OnClickListener {
	
	private static boolean useNativeEngine = true;
	
	private static String[] navigationTitles = {
		"My Stories", "Profile", "Write Photo Story", "Write Video Story"
	};
	
	private SlideMenu mSlideMenu;
	
	private RelativeLayout mnuItemMyStories;
	private RelativeLayout mnuItemProfile;
	private RelativeLayout mnuItemWritePStory;
	private RelativeLayout mnuItemWriteVStory;
	
	private ImageButton buttonMenu;
	private TextView textViewNavTitle;
	
	private int curFragment = 0;
	private Fragment currentFragment = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_slidemenu);
	}
	
	@Override
	public void onContentChanged() {
		super.onContentChanged();
		
		mSlideMenu = (SlideMenu)findViewById(R.id.slideMenu);
		
		getLayoutInflater().inflate(R.layout.layout_slide_content, mSlideMenu, true);
		getLayoutInflater().inflate(R.layout.layout_primary_menu, mSlideMenu, true);
		
		initControls();
		
		curFragment = 0;
		if (findViewById(R.id.main_content_layout) != null) {
			MyStoriesFragment mystoriesFragment = new MyStoriesFragment();
			getSupportFragmentManager().beginTransaction()
					.add(R.id.main_content_layout, mystoriesFragment).commit();
			currentFragment = mystoriesFragment;
		}
		textViewNavTitle.setText(navigationTitles[0]);
	}
	
	private void initControls() {
		mnuItemMyStories = (RelativeLayout)findViewById(R.id.mnuItemMyStories);
		mnuItemProfile = (RelativeLayout)findViewById(R.id.mnuItemProfile);
		mnuItemWritePStory = (RelativeLayout)findViewById(R.id.mnuItemWritePStory);
		mnuItemWriteVStory = (RelativeLayout)findViewById(R.id.mnuItemWriteVStory);
		
		buttonMenu = (ImageButton)findViewById(R.id.buttonMenu);
		textViewNavTitle = (TextView)findViewById(R.id.textViewNavTitle);
		
		mnuItemMyStories.setOnClickListener(this);
		mnuItemProfile.setOnClickListener(this);
		mnuItemWritePStory.setOnClickListener(this);
		mnuItemWriteVStory.setOnClickListener(this);
		
		buttonMenu.setOnClickListener(this);
		
		textViewNavTitle.setTypeface(Constants.myFontBoldTypeface);
		
		TextView textViewMyStories = (TextView)findViewById(R.id.textViewMyStories);
		textViewMyStories.setTypeface(Constants.myFontTypeface);
		TextView textViewProfile = (TextView)findViewById(R.id.textViewProfile);
		textViewProfile.setTypeface(Constants.myFontTypeface);
		TextView textViewPhotoStory = (TextView)findViewById(R.id.textViewPhotoStory);
		textViewPhotoStory.setTypeface(Constants.myFontTypeface);
		TextView textViewVideoStory = (TextView)findViewById(R.id.textViewVideoStory);
		textViewVideoStory.setTypeface(Constants.myFontTypeface);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.buttonMenu:
		{
			if (mSlideMenu.isOpen()) {
				mSlideMenu.close(true);
			} else {
				mSlideMenu.open(false, true);
			}
		}
			break;
		case R.id.mnuItemMyStories:
		{
			mSlideMenu.close(true);
			if (curFragment == 0) return;
			
			if (curFragment == 2 ||
				curFragment == 3) {
				if (useNativeEngine)
					((WriteStoryNFragment)currentFragment).onReplaceToOtherFragment();
				else
					((WriteStoryFragment)currentFragment).onReplaceToOtherFragment();
			}
			
			curFragment = 0;
			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			MyStoriesFragment fragment = new MyStoriesFragment();
			transaction.replace(R.id.main_content_layout, fragment);
			transaction.addToBackStack(null);
			transaction.commit();
			currentFragment = fragment;
			textViewNavTitle.setText(navigationTitles[0]);
		}
			break;
		case R.id.mnuItemProfile:
		{
			mSlideMenu.close(true);
			if (curFragment == 1) return;
			
			if (curFragment == 2 ||
				curFragment == 3) {
				if (useNativeEngine)
					((WriteStoryNFragment)currentFragment).onReplaceToOtherFragment();
				else
					((WriteStoryFragment)currentFragment).onReplaceToOtherFragment();
			}
			
			curFragment = 1;
			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			ProfileFragment fragment = new ProfileFragment();
			transaction.replace(R.id.main_content_layout, fragment);
			transaction.addToBackStack(null);
			transaction.commit();
			currentFragment = fragment;
			textViewNavTitle.setText(navigationTitles[1]);
		}
			break;
		case R.id.mnuItemWritePStory:
		{
			mSlideMenu.close(true);
			if (curFragment == 2) return;
			
			if (curFragment == 3) {
				if (useNativeEngine)
					((WriteStoryNFragment)currentFragment).onReplaceToOtherFragment();
				else
					((WriteStoryFragment)currentFragment).onReplaceToOtherFragment();
			}
			
			curFragment = 2;
			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			Fragment fragment;
			if (useNativeEngine)
				fragment = new WriteStoryNFragment(Constants.SOURCE_PHOTO);
			else
				fragment = new WriteStoryFragment(Constants.SOURCE_PHOTO);
			transaction.replace(R.id.main_content_layout, fragment);
			transaction.addToBackStack(null);
			transaction.commit();
			currentFragment = fragment;
			textViewNavTitle.setText(navigationTitles[2]);
		}
			break;
		case R.id.mnuItemWriteVStory:
		{
			mSlideMenu.close(true);
			if (curFragment == 3) return;
			
			if (curFragment == 2) {
				if (useNativeEngine)
					((WriteStoryNFragment)currentFragment).onReplaceToOtherFragment();
				else
					((WriteStoryFragment)currentFragment).onReplaceToOtherFragment();
			}
			
			curFragment = 3;
			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			Fragment fragment;
			if (useNativeEngine)
				fragment = new WriteStoryNFragment(Constants.SOURCE_VIDEO);
			else
				fragment = new WriteStoryFragment(Constants.SOURCE_VIDEO);
			transaction.replace(R.id.main_content_layout, fragment);
			transaction.addToBackStack(null);
			transaction.commit();
			currentFragment = fragment;
			textViewNavTitle.setText(navigationTitles[3]);
		}
			break;
		}
	}
	
	@Override
	public void onBackPressed() {
		finish();
		System.exit(0);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (currentFragment != null &&
			curFragment == 0) {
			currentFragment.onActivityResult(requestCode, resultCode, data);
		}
	}
	
	@Override
	public void onConfigurationChanged (Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

}
