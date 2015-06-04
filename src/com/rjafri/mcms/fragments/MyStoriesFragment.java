package com.rjafri.mcms.fragments;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Video.Thumbnails;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.Toast;

import com.facebook.FacebookRequestError;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.PlusShare;
import com.rjafri.mcms.R;
import com.rjafri.mcms.models.Constants;
import com.rjafri.mcms.views.SquareLayout;

@SuppressLint("InflateParams") 
public class MyStoriesFragment extends Fragment implements OnClickListener, ConnectionCallbacks, OnConnectionFailedListener {

	private ArrayList<String> storyPaths;
	
	private int menuOpenIndex = -1;
	
	private GridView gridViewStories;
	private StoriesAdapter adapter;
	
	private ProgressDialog progressDialog;
	
	private static final int RC_GOOGLEPLUS_SIGN_IN = 0;
	private static final int RC_GOOGLEPLUS_SHARE = 1;
	
	private GoogleApiClient googleApiClient = null;
	private boolean intentInProgress = false;
	private int resolveTryCount = 0;
	
	public MyStoriesFragment() {
		storyPaths = new ArrayList<String>();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_mystories, container, false);
		
		readMyStories();
		
		gridViewStories = (GridView)view.findViewById(R.id.gridViewStories);
		
		adapter = new StoriesAdapter();
		gridViewStories.setAdapter(adapter);
		
		progressDialog = new ProgressDialog(getActivity());
		progressDialog.setIndeterminate(true);
		progressDialog.setMessage("Uploading video...");
		progressDialog.setCancelable(false);
		
		return view;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		googleApiClient = new GoogleApiClient.Builder(getActivity())
		.addConnectionCallbacks(this)
		.addOnConnectionFailedListener(this)
		.addApi(Plus.API)
		.addScope(Plus.SCOPE_PLUS_LOGIN)
		.build();
	}
	
	@Override
	public void onStart() {
		super.onStart();
		googleApiClient.connect();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (googleApiClient.isConnected()) {
			googleApiClient.disconnect();
		}
	}
	
	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.buttonStoryItem) {
			int storyIndex = ((Integer)v.getTag()).intValue();
			openMenuForStory(storyIndex, (View)v.getParent());
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (resultCode != Activity.RESULT_OK)
			return;
		
		if (requestCode == RC_GOOGLEPLUS_SIGN_IN) {
			Log.e("MyStoriesFragment", "onActivityResult - RC_GOOGLEPLUS_SIGN_IN");
			intentInProgress = false;
			if (!googleApiClient.isConnecting()) {
				googleApiClient.connect();
			}
		} else if (requestCode == RC_GOOGLEPLUS_SHARE) {
			Log.e("MyStoriesFragment", "onActivityResult - RC_GOOGLEPLUS_SHARE");
		} else {
			Log.e("MyStoriesFragment", "onActivityResult - Facebook");
			if (Session.getActiveSession() == null)
				return;
			Session.getActiveSession().onActivityResult(getActivity(), requestCode, resultCode, data);
		}
	}
	
	@Override
	public void onConnected(Bundle arg0) {
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		googleApiClient.connect();
	}
	
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		if (!intentInProgress && result.hasResolution()) {
			if (resolveTryCount <= 0) {
				try {
					intentInProgress = true;
					resolveTryCount ++;
					getActivity().startIntentSenderForResult(result.getResolution().getIntentSender(), RC_GOOGLEPLUS_SIGN_IN, null, 0, 0, 0);
				} catch (SendIntentException e) {
					intentInProgress = false;
					googleApiClient.connect();
				}
			}
		}
	}
	
	private void refreshStories() {
		readMyStories();
		adapter.notifyDataSetChanged();
	}
	
	private void readMyStories() {
		storyPaths.clear();
		
		File storiesDir = new File(Constants.storiesDirectoryPath);
		File[] storyFiles = storiesDir.listFiles();
		if (storyFiles == null)
			return;
		
		for (int i = 0; i < storyFiles.length; i ++) {
			File storyFile = storyFiles[i];
			if (!storyFile.isFile() ||
				storyFile.isHidden() ||
				!storyFile.canRead())
				continue;
			
			String storyFileName = storyFile.getName();
			Log.i("MyStoriesFragment", "Story File : " + storyFileName);
			if (!storyFileName.toLowerCase(Locale.US).endsWith(".mp4"))
				continue;
			
			storyPaths.add(storyFile.getAbsolutePath());
		}
	}
	
	private void openMenuForStory(int storyIndex, View anchorView) {
		menuOpenIndex = storyIndex;
		PopupMenu popup = new PopupMenu(getActivity(), anchorView);
		popup.getMenuInflater().inflate(R.menu.story_item_menu, popup.getMenu());
		SubMenu shareSubMenu = popup.getMenu().addSubMenu(getResources().getString(R.string.share));
		popup.getMenuInflater().inflate(R.menu.share_sub_menu, shareSubMenu);
		popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				
				if (menuOpenIndex < 0)
					return false;
				
				if (item.getItemId() == R.id.watch_story) {
					
					String storyFilePath = storyPaths.get(menuOpenIndex);
					Intent openIntent = new Intent(Intent.ACTION_VIEW);
					openIntent.setDataAndType(Uri.fromFile(new File(storyFilePath)), "video/*");
					startActivity(openIntent);
					
				} else if (item.getItemId() == R.id.delete_story) {
					
					String storyFilePath = storyPaths.get(menuOpenIndex);
					
					long videoId = getVideoIdFromFilePath(storyFilePath);
					if (videoId != -1) {
						Uri videoUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoId);
						getActivity().getContentResolver().delete(videoUri, null, null);
					}
					
					refreshStories();
					
				} else if (item.getItemId() == R.id.share_facebook) {
					
					shareFacebook(menuOpenIndex);
					
				} else if (item.getItemId() == R.id.share_googleplus) {
					
					shareGooglePlus(menuOpenIndex);
					
				}
				
				return false;
			}
			
		});
		popup.show();
	}
	
	private void shareFacebook(int storyIndex) {
		if (Session.getActiveSession() == null ||
			Session.getActiveSession().isOpened() == false) {
			Log.d("MyStoriesFragment", "Signing in.");
			List<String> permissions = new LinkedList<String>();
			permissions.add("public_profile");
			final int videoIndex = storyIndex;
			Session.openActiveSession(getActivity(), this, true, permissions, new Session.StatusCallback() {
				
				@Override
				public void call(Session session, SessionState state, Exception exception) {
					if (exception != null)
					{
						Toast.makeText(getActivity().getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
						return;
					}
					else
					{
						if (session.isOpened())
						{
							shareFacebook(videoIndex);
							return;
						}
					}
				}
			});
		} else {
			final List<String> PERMISSIONS = Arrays.asList("publish_actions");
			List<String> permissionList = Session.getActiveSession().getPermissions();
			if (!isSubsetOf(PERMISSIONS, permissionList)) {
				Log.d("MyStoriesFragment", "Requesting new permissions.");
				Session.NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(this, PERMISSIONS);
				Session.getActiveSession().requestNewPublishPermissions(newPermissionsRequest);
				return;
			} else {
				Log.d("MyStoriesFragment", "Uploading video.");
				try {
					progressDialog.setMessage("Uploading video...");
					progressDialog.show();
					
					String videoFilePath = storyPaths.get(storyIndex);
					
					Request request = Request.newUploadVideoRequest(Session.getActiveSession(), new File(videoFilePath), new Request.Callback() {
						
						@Override
						public void onCompleted(Response response) {
							progressDialog.dismiss();
							FacebookRequestError error = response.getError();
							if (error == null) {
								Toast.makeText(getActivity().getApplicationContext(), "Posted story on Facebook.", Toast.LENGTH_LONG).show();
							} else {
								Toast.makeText(getActivity().getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();
							}
						}
					});
					Bundle params = request.getParameters();
					params.putString("description", "Written by My City My Stories.");
					request.setParameters(params);
					
					request.executeAsync();
				} catch (FileNotFoundException e) {
					if (progressDialog.isShowing())
						progressDialog.dismiss();
					e.printStackTrace();
					Toast.makeText(getActivity().getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
				}
			}
		}
	}
	
	private boolean isSubsetOf(Collection<String> subset, Collection<String> superset) {
		for (String string : subset) {
			if (!superset.contains(string)) {
				return false;
			}
		}
		return true;
	}
	
	private void shareGooglePlus(int storyIndex) {
		if (googleApiClient == null)
			return;
		if (!googleApiClient.isConnected()) {
			googleApiClient.connect();
		}
		
		String storyVideoPath = storyPaths.get(storyIndex);
		long videoId = getVideoIdFromFilePath(storyVideoPath);
		if (videoId == -1) {
			Intent galleryIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
			Uri outputFileUri = Uri.fromFile(new File(storyVideoPath));
			galleryIntent.setData(outputFileUri);
			getActivity().sendBroadcast(galleryIntent);
			
			new AlertDialog.Builder(getActivity())
			.setMessage("This story is being exported to gallery now. Please try again later.")
			.setTitle("Warning")
			.setNegativeButton("Close", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.create()
			.show();
			return;
		}
		
		Uri videoUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoId);
		
		PlusShare.Builder share = new PlusShare.Builder(getActivity());
		share.setText("Written by My City My Stories.");
		share.addStream(videoUri);
		share.setType("video/mp4");
		startActivityForResult(share.getIntent(), RC_GOOGLEPLUS_SHARE);
	}
	
	private long getVideoIdFromFilePath(String filePath) {
		long videoId = -1;
		Uri videosUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
		Cursor cursor = null;
		try {
			String[] projection = {MediaStore.Video.Media._ID};
			cursor = getActivity().getContentResolver().query(videosUri, projection,
					MediaStore.Video.Media.DATA + " LIKE ?", new String[] {filePath}, null);
			int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
			cursor.moveToFirst();
			videoId = cursor.getLong(column_index);
		} finally {
			if (cursor != null)
				cursor.close();
		}
		
		return videoId;
	}
	
	private class StoriesAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return storyPaths.size();
		}

		@Override
		public Object getItem(int position) {
			return storyPaths.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			SquareLayout layoutItem = null;
			if (convertView == null) {
				layoutItem = (SquareLayout)getActivity().getLayoutInflater().inflate(R.layout.story_item, null);
			} else {
				layoutItem = (SquareLayout)convertView;
			}
			
			Button buttonStoryItem = (Button)layoutItem.findViewById(R.id.buttonStoryItem);
			buttonStoryItem.setTag(Integer.valueOf(position));
			buttonStoryItem.setOnClickListener(MyStoriesFragment.this);
			
			String storyPath = storyPaths.get(position);
			Bitmap thumbBitmap = ThumbnailUtils.createVideoThumbnail(storyPath, Thumbnails.MINI_KIND);
			ImageView imageViewStoryItem = (ImageView)layoutItem.findViewById(R.id.imageViewStoryItem);
			imageViewStoryItem.setImageBitmap(thumbBitmap);
			
			return layoutItem;
		}
		
	}

}
