package com.rjafri.mcms.fragments;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.provider.MediaStore.Video.Thumbnails;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.rjafri.mcms.R;
import com.rjafri.mcms.models.Constants;
import com.rjafri.mcms.models.StoryBuilder;
import com.rjafri.mcms.models.StoryBuilder.OnCompleteListener;
import com.rjafri.mcms.models.StoryContent;
import com.videokit.Videokit;

@SuppressLint("InflateParams") 
public class WriteStoryFragment extends Fragment implements OnClickListener {
	
	private static final int PICK_CONTENT_REQ = 1;
	private static final int PICK_AUDIO_REQ = 2;
	
	private int sourceMode = Constants.SOURCE_PHOTO;
	private int menuOpenIndex = -1;
	
	private StoryBuilder storyBuilder = null;
	
	private ArrayList<StoryContent> contentArray = new ArrayList<StoryContent>();
	
	private TextView textViewInstruction;
	private HorizontalScrollView scrollContents;
	private LinearLayout contentsContainer;
	private ImageButton buttonAddContent;
	private EditText editTextAudioPath;
	private ImageButton buttonBrowseAudio;
	private Spinner spinnerTransition;
	private ImageButton buttonDone;
	private RelativeLayout layoutFilters;
	
	private ProgressDialog progressDialog;
	
	public WriteStoryFragment() {
		this.sourceMode = Constants.SOURCE_PHOTO;
	}
	
	public WriteStoryFragment(int sourceMode) {
		this.sourceMode = sourceMode;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_writestory, container, false);
		
		textViewInstruction = (TextView)view.findViewById(R.id.textViewInstruction);
		scrollContents = (HorizontalScrollView)view.findViewById(R.id.scrollContents);
		contentsContainer = (LinearLayout)view.findViewById(R.id.contentsContainer);
		buttonAddContent = (ImageButton)view.findViewById(R.id.buttonAddContent);
		editTextAudioPath = (EditText)view.findViewById(R.id.editTextAudioPath);
		buttonBrowseAudio = (ImageButton)view.findViewById(R.id.buttonBrowseAudio);
		spinnerTransition = (Spinner)view.findViewById(R.id.spinnerTransition);
		buttonDone = (ImageButton)view.findViewById(R.id.buttonDone);
		layoutFilters = (RelativeLayout)view.findViewById(R.id.layoutFilters);
		
		if (sourceMode == Constants.SOURCE_VIDEO)
			textViewInstruction.setText(getResources().getString(R.string.instruction_video));
		else
			textViewInstruction.setText(getResources().getString(R.string.instruction_photo));
		
		buttonAddContent.setOnClickListener(this);
		
		editTextAudioPath.setText("");
		editTextAudioPath.setTypeface(Constants.myFontTypeface);
		
		buttonBrowseAudio.setOnClickListener(this);
		buttonDone.setOnClickListener(this);
		
		ArrayAdapter<CharSequence> transitionAdapter = ArrayAdapter.createFromResource(
                getActivity(), R.array.transitions, android.R.layout.simple_spinner_item);
        transitionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTransition.setAdapter(transitionAdapter);
        spinnerTransition.setSelection(0);
        
        layoutFilters.setVisibility(View.INVISIBLE);
        
        progressDialog = new ProgressDialog(getActivity());
		progressDialog.setIndeterminate(true);
		progressDialog.setMessage("Writing your story...");
		progressDialog.setCancelable(false);
		progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Log.e("WriteStoryFragment", "onCancel");
				if (storyBuilder == null)
					return;
				storyBuilder.stop();
				storyBuilder = null;
			}
		});
		
		TextView textViewInstruction = (TextView)view.findViewById(R.id.textViewInstruction);
		textViewInstruction.setTypeface(Constants.myFontBoldTypeface);
		TextView textViewAudioPath = (TextView)view.findViewById(R.id.textViewAudioPath);
		textViewAudioPath.setTypeface(Constants.myFontBoldTypeface);
		TextView textViewTransition = (TextView)view.findViewById(R.id.textViewTransition);
		textViewTransition.setTypeface(Constants.myFontBoldTypeface);
		TextView textViewFilterNone = (TextView)view.findViewById(R.id.textViewFilterNone);
		textViewFilterNone.setTypeface(Constants.myFontTypeface);
		TextView textViewFilterVintage = (TextView)view.findViewById(R.id.textViewFilterVintage);
		textViewFilterVintage.setTypeface(Constants.myFontTypeface);
		TextView textViewFilterVignette = (TextView)view.findViewById(R.id.textViewFilterVignette);
		textViewFilterVignette.setTypeface(Constants.myFontTypeface);
		TextView textViewFilterGrayscale = (TextView)view.findViewById(R.id.textViewFilterGrayscale);
		textViewFilterGrayscale.setTypeface(Constants.myFontTypeface);
		TextView textViewFilterSepiaTones = (TextView)view.findViewById(R.id.textViewFilterSepiaTones);
		textViewFilterSepiaTones.setTypeface(Constants.myFontTypeface);
		
		Button buttonCloseFilters = (Button)view.findViewById(R.id.buttonCloseFilters);
		Button buttonFilterNone = (Button)view.findViewById(R.id.buttonFilterNone);
		Button buttonFilterVintage = (Button)view.findViewById(R.id.buttonFilterVintage);
		Button buttonFilterVignette = (Button)view.findViewById(R.id.buttonFilterVignette);
		Button buttonFilterGrayscale = (Button)view.findViewById(R.id.buttonFilterGrayscale);
		Button buttonFilterSepiaTones = (Button)view.findViewById(R.id.buttonFilterSepiaTones);
		buttonCloseFilters.setOnClickListener(this);
		buttonFilterNone.setOnClickListener(this);
		buttonFilterVintage.setOnClickListener(this);
		buttonFilterVignette.setOnClickListener(this);
		buttonFilterGrayscale.setOnClickListener(this);
		buttonFilterSepiaTones.setOnClickListener(this);
		
		return view;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.buttonAddContent:
			addContent();
			break;
		case R.id.buttonContentItem:
			ImageButton buttonContentItem = (ImageButton)v;
			int contentIndex = ((Integer)buttonContentItem.getTag()).intValue();
			openMenuForContent(contentIndex);
			break;
		case R.id.buttonContentFilter:
			ImageButton buttonContentFilter = (ImageButton)v;
			menuOpenIndex = ((Integer)buttonContentFilter.getTag()).intValue();
			openFilterScroll();
			break;
		case R.id.buttonBrowseAudio:
			browseAudioFile();
			break;
		case R.id.buttonDone:
			onButtonDone();
			break;
		case R.id.buttonCloseFilters:
			layoutFilters.setVisibility(View.INVISIBLE);
			Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.fadeout);
			anim.setDuration(300);
			layoutFilters.setAnimation(anim);
			break;
		case R.id.buttonFilterNone:
		case R.id.buttonFilterVintage:
		case R.id.buttonFilterVignette:
		case R.id.buttonFilterGrayscale:
		case R.id.buttonFilterSepiaTones:
			String strFilterGraph = "";
			StoryContent storyContent = contentArray.get(menuOpenIndex);
			if (v.getId() == R.id.buttonFilterNone) {
				storyContent.filter = 0;
			} else if (v.getId() == R.id.buttonFilterVintage) {
				storyContent.filter = 1;
				strFilterGraph = "curves=vintage";
			} else if (v.getId() == R.id.buttonFilterVignette) {
				storyContent.filter = 2;
				strFilterGraph = "vignette=PI/4";
			} else if (v.getId() == R.id.buttonFilterGrayscale) {
				storyContent.filter = 3;
				strFilterGraph = "colorchannelmixer=.3:.4:.3:0:.3:.4:.3:0:.3:.4:.3";
			} else if (v.getId() == R.id.buttonFilterSepiaTones) {
				storyContent.filter = 4;
				strFilterGraph = "colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131";
			}
			
			File thumbFile = new File(storyContent.thumbPath);
			String strThumbToDisplay = storyContent.thumbPath;
			if (!strFilterGraph.equals("")) {
				String thumbFileName = thumbFile.getName();
				File filteredThumbFile = new File(Constants.workDirectoryPath, thumbFileName + "_filtered.jpg");
				
				Videokit vk = new Videokit();
				String[] args = new String[] {
					"ffmpeg",
					"-y",
					"-i",
					storyContent.thumbPath,
					"-filter_complex",
					"[0:v]" + strFilterGraph + "[v]",
					"-map",
					"[v]",
					filteredThumbFile.getAbsolutePath()
				};
				int ret = vk.run(args);
				if (ret == 0)
					strThumbToDisplay = filteredThumbFile.getAbsolutePath();
			}
			Bitmap thumbBitmap = BitmapFactory.decodeFile(strThumbToDisplay);
			View itemView = contentsContainer.getChildAt(menuOpenIndex);
			ImageView imageViewContentItem = (ImageView)itemView.findViewById(R.id.imageViewContentItem);
			imageViewContentItem.setImageBitmap(thumbBitmap);
			
			layoutFilters.setVisibility(View.INVISIBLE);
			anim = AnimationUtils.loadAnimation(getActivity(), R.anim.fadeout);
			anim.setDuration(300);
			layoutFilters.setAnimation(anim);
			
			break;
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if (resultCode != Activity.RESULT_OK)
			return;
		
		Log.e("WriteStoryFragment", "onActivityResult " + requestCode);
		
		if (requestCode == PICK_CONTENT_REQ) {
			if (sourceMode == Constants.SOURCE_PHOTO) {
				Uri selectedImage = data.getData();
				String imagePath = "";
				Cursor cursor = null;
				try {
					String[] proj = {MediaStore.Images.Media.DATA};
					cursor = getActivity().getContentResolver().query(selectedImage, proj, null, null, null);
					int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
					cursor.moveToFirst();
					imagePath = cursor.getString(column_index);
				} finally {
					if (cursor != null)
						cursor.close();
				}
				if (!imagePath.equals(""))
					addPhoto(imagePath);
			} else {
				Uri selectedVideo = data.getData();
				String videoPath = "";
				Cursor cursor = null;
				try {
					String[] proj = {MediaStore.Video.Media.DATA};
					cursor = getActivity().getContentResolver().query(selectedVideo, proj, null, null, null);
					int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
					cursor.moveToFirst();
					videoPath = cursor.getString(column_index);
				} finally {
					if (cursor != null)
						cursor.close();
				}
				
				if (!videoPath.equals(""))
					addVideo(videoPath);
			}
		} else {
			Uri selectedAudio = data.getData();
			String audioPath = "";
			Cursor cursor = null;
			try {
				String[] proj = {MediaStore.Audio.Media.DATA};
				cursor = getActivity().getContentResolver().query(selectedAudio, proj, null, null, null);
				int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
				cursor.moveToFirst();
				audioPath = cursor.getString(column_index);
			} finally {
				if (cursor != null)
					cursor.close();
			}
			editTextAudioPath.setText(audioPath);
		}
	}
	
	public void onReplaceToOtherFragment() {
		File workDirectory = new File(Constants.workDirectoryPath);
		String[] myFiles;
		myFiles = workDirectory.list();  
		for (int i = 0; i < myFiles.length; i ++) {
			if (!myFiles[i].toLowerCase(Locale.US).endsWith(".jpg"))
				continue;
			File myFile = new File(workDirectory, myFiles[i]);   
			myFile.delete();  
		}
	}
	
	private void addContent() {
		int contentsCount = contentArray.size();
		if (contentsCount >= Constants.MAX_CONTENTS) {
			new AlertDialog.Builder(getActivity())
			.setMessage("You cannot add more than " + Constants.MAX_CONTENTS + " contents.")
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
		
		View anchorView = contentsContainer.getChildAt(contentsCount);
		PopupMenu popup = new PopupMenu(getActivity(), anchorView);
		if (sourceMode == Constants.SOURCE_PHOTO)
			popup.getMenuInflater().inflate(R.menu.pick_photo_content_menu, popup.getMenu());
		else
			popup.getMenuInflater().inflate(R.menu.pick_video_content_menu, popup.getMenu());
		popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if (item.getItemId() == R.id.take_photo) {
					if (sourceMode == Constants.SOURCE_PHOTO) {
						Intent takePhoto = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
						startActivityForResult(takePhoto, PICK_CONTENT_REQ);
					} else {
						Intent recordVideo = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
						startActivityForResult(recordVideo, PICK_CONTENT_REQ);
					}
				} else if (item.getItemId() == R.id.pick_gallary) {
					if (sourceMode == Constants.SOURCE_PHOTO) {
						Intent pickPhoto = new Intent(Intent.ACTION_PICK, Media.EXTERNAL_CONTENT_URI);
						startActivityForResult(pickPhoto, PICK_CONTENT_REQ);
					} else {
						Intent pickVideo = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
						startActivityForResult(pickVideo, PICK_CONTENT_REQ);
					}
				}
				return false;
			}
		});
		popup.show();
	}
	
	private void addPhoto(String imagePath) {
		Bitmap orgBitmap = null;
		try {
			orgBitmap = decodePath(imagePath, Constants.STORY_WIDTH, Constants.STORY_HEIGHT);
		} catch (Exception e) {
			e.printStackTrace();
			orgBitmap = null;
		}
		if (orgBitmap == null)
			return;
		
		int rotate = getBitmapRotation(imagePath);
		Log.e("WriteStoryFragment", "Rotation = " + rotate);
		
		Matrix matrix = new Matrix();
		matrix.postRotate(rotate);
		Bitmap selectedBitmap = Bitmap.createBitmap(orgBitmap, 0, 0,
				orgBitmap.getWidth(), orgBitmap.getHeight(), matrix, true);
		Log.e("WriteStoryFragment", "Width = " + selectedBitmap.getWidth() + ", Height = " + selectedBitmap.getHeight());
		
		Bitmap adjustedBitmap = adjustedBitmap(selectedBitmap, Constants.STORY_WIDTH, Constants.STORY_HEIGHT);
		selectedBitmap.recycle();
		
		int contentIndex = contentArray.size();
		int photoIndex = 0;
		while (true) {
			String desiredFileName = "photo" + photoIndex + ".jpg";
			boolean is_exist = false;
			for (int i = 0; i < contentArray.size(); i ++) {
				StoryContent storyContent = contentArray.get(i);
				File contentFile = new File(storyContent.contentPath);
				String contentFileName = contentFile.getName();
				if (desiredFileName.equalsIgnoreCase(contentFileName)) {
					is_exist = true;
					break;
				}
			}
			if (is_exist == false)
				break;
			photoIndex ++;
		}
		Log.i("WriteStoryFragment", "Content Index = " + photoIndex);
		File contentFile = new File(Constants.workDirectoryPath, "photo" + photoIndex + ".jpg");
		try {
			FileOutputStream os;
			os = new FileOutputStream(contentFile);
			adjustedBitmap.compress(CompressFormat.JPEG, 50, os);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		Bitmap thumbBitmap = adjustedBitmap(adjustedBitmap, Constants.THUMB_WIDTH, Constants.THUMB_HEIGHT);
		adjustedBitmap.recycle();
		
		File thumbFile = new File(Constants.workDirectoryPath, "photo_thumb" + photoIndex + ".jpg");
		try {
			FileOutputStream os;
			os = new FileOutputStream(thumbFile);
			thumbBitmap.compress(CompressFormat.JPEG, 50, os);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		RelativeLayout layoutContentItem = (RelativeLayout)getActivity().getLayoutInflater().inflate(R.layout.content_item, null);
		ImageView imageViewContentItem = (ImageView)layoutContentItem.findViewById(R.id.imageViewContentItem);
		imageViewContentItem.setImageBitmap(thumbBitmap);
		TextView textViewContentItem = (TextView)layoutContentItem.findViewById(R.id.textViewContentItem);
		textViewContentItem.setText("No Title");
		textViewContentItem.setTypeface(Constants.myFontTypeface);
		TextView textViewContentDuration = (TextView)layoutContentItem.findViewById(R.id.textViewContentDuration);
		textViewContentDuration.setVisibility(View.GONE);
		ImageButton buttonContentItem = (ImageButton)layoutContentItem.findViewById(R.id.buttonContentItem);
		buttonContentItem.setTag(Integer.valueOf(contentIndex));
		buttonContentItem.setOnClickListener(this);
		ImageButton buttonContentFilter = (ImageButton)layoutContentItem.findViewById(R.id.buttonContentFilter);
		buttonContentFilter.setTag(Integer.valueOf(contentIndex));
		buttonContentFilter.setOnClickListener(this);
		
		contentsContainer.addView(layoutContentItem, contentIndex);
		LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)layoutContentItem.getLayoutParams();
		lp.leftMargin = (int)getResources().getDimension(R.dimen.content_horizontal_spacing);
		lp.rightMargin = (int)getResources().getDimension(R.dimen.content_horizontal_spacing);
		layoutContentItem.setLayoutParams(lp);
		
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				scrollContents.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
			}
		}, 100L);
		
		StoryContent storyContent = new StoryContent();
		storyContent.contentType = Constants.SOURCE_PHOTO;
		storyContent.contentPath = contentFile.getAbsolutePath();
		storyContent.thumbPath = thumbFile.getAbsolutePath();
		storyContent.overlayString = "";
		contentArray.add(storyContent);
	}
	
	private void addVideo(String videoPath) {
		
		File videoFile = new File(videoPath);
		String videoFileName = videoFile.getName();
		
		Bitmap thumbBitmap = ThumbnailUtils.createVideoThumbnail(videoPath, Thumbnails.MINI_KIND);
		File thumbFile = new File(Constants.workDirectoryPath, videoFileName + "_thumb.jpg");
		try {
			FileOutputStream os;
			os = new FileOutputStream(thumbFile);
			thumbBitmap.compress(CompressFormat.JPEG, 50, os);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		
		MediaPlayer mp = MediaPlayer.create(getActivity(), Uri.parse(videoPath));
		float duration = (float)mp.getDuration() / 1000.f;
		mp.release();
		
		int contentIndex = contentArray.size();
		
		RelativeLayout layoutContentItem = (RelativeLayout)getActivity().getLayoutInflater().inflate(R.layout.content_item, null);
		ImageView imageViewContentItem = (ImageView)layoutContentItem.findViewById(R.id.imageViewContentItem);
		imageViewContentItem.setImageBitmap(thumbBitmap);
		TextView textViewContentItem = (TextView)layoutContentItem.findViewById(R.id.textViewContentItem);
		textViewContentItem.setText("No Title");
		textViewContentItem.setTypeface(Constants.myFontTypeface);
		TextView textViewContentDuration = (TextView)layoutContentItem.findViewById(R.id.textViewContentDuration);
		textViewContentDuration.setVisibility(View.VISIBLE);
		textViewContentDuration.setTypeface(Constants.myFontTypeface);
		textViewContentDuration.setText(String.format("%.1fs", duration));
		ImageButton buttonContentItem = (ImageButton)layoutContentItem.findViewById(R.id.buttonContentItem);
		buttonContentItem.setTag(Integer.valueOf(contentIndex));
		buttonContentItem.setOnClickListener(this);
		ImageButton buttonContentFilter = (ImageButton)layoutContentItem.findViewById(R.id.buttonContentFilter);
		buttonContentFilter.setTag(Integer.valueOf(contentIndex));
		buttonContentFilter.setOnClickListener(this);
		
		contentsContainer.addView(layoutContentItem, contentIndex);
		LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)layoutContentItem.getLayoutParams();
		lp.leftMargin = (int)getResources().getDimension(R.dimen.content_horizontal_spacing);
		lp.rightMargin = (int)getResources().getDimension(R.dimen.content_horizontal_spacing);
		layoutContentItem.setLayoutParams(lp);
		
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				scrollContents.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
			}
		}, 100L);
		
		StoryContent storyContent = new StoryContent();
		storyContent.contentType = Constants.SOURCE_VIDEO;
		storyContent.contentPath = videoPath;
		storyContent.thumbPath = thumbFile.getAbsolutePath();
		storyContent.overlayString = "";
		contentArray.add(storyContent);
	}
	
	private void openMenuForContent(int contentIndex) {
		menuOpenIndex = contentIndex;
		View anchorView = contentsContainer.getChildAt(contentIndex);
		PopupMenu popup = new PopupMenu(getActivity(), anchorView);
		popup.getMenuInflater().inflate(R.menu.content_item_menu, popup.getMenu());
		popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				
				if (menuOpenIndex < 0)
					return false;
				
				if (item.getItemId() == R.id.preview_content) {
					
					StoryContent storyContent = contentArray.get(menuOpenIndex);
					Intent openIntent = new Intent(Intent.ACTION_VIEW);
					if (storyContent.contentType == Constants.SOURCE_PHOTO)
						openIntent.setDataAndType(Uri.fromFile(new File(storyContent.contentPath)), "image/*");
					else
						openIntent.setDataAndType(Uri.fromFile(new File(storyContent.contentPath)), "video/*");
					startActivity(openIntent);
					
				} else if (item.getItemId() == R.id.set_title) {
					
					final EditText input = new EditText(getActivity());
					input.setMaxLines(1);
					input.setSingleLine();
					input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(Constants.OVERLAY_LINE_LENGTH * Constants.OVERLAY_MAX_LINES)});
					input.setTextColor(getResources().getColor(R.color.black));
					input.setTextSize(getResources().getDimension(R.dimen.common_font_size_2));
					input.setBackgroundResource(R.drawable.edittext_background);
					
					final StoryContent storyContent = contentArray.get(menuOpenIndex);
					input.setText(storyContent.overlayString);
					
					new AlertDialog.Builder(getActivity())
					.setTitle("Input Title")
					.setView(input)
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							storyContent.overlayString = input.getText().toString();
							contentArray.set(menuOpenIndex, storyContent);
							
							View itemView = contentsContainer.getChildAt(menuOpenIndex);
							TextView textViewContentItem = (TextView)itemView.findViewById(R.id.textViewContentItem);
							if (storyContent.overlayString.equals("")) {
								textViewContentItem.setText("No Title");
							} else {
								textViewContentItem.setText(storyContent.overlayString);
							}
						}
					})
					.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					})
					.create()
					.show();
					
				} else if (item.getItemId() == R.id.delete_content) {
					
					StoryContent storyContent = contentArray.get(menuOpenIndex);
					File contentFile = new File(storyContent.contentPath);
					File thumbFile = new File(storyContent.thumbPath);
					String thumbFileName = thumbFile.getName();
					File filteredThumbFile = new File(Constants.workDirectoryPath, thumbFileName + "_filtered.jpg");
					filteredThumbFile.delete();
//					thumbFile.delete();
					if (sourceMode == Constants.SOURCE_PHOTO)
						contentFile.delete();
					
					contentArray.remove(menuOpenIndex);
					contentsContainer.removeViewAt(menuOpenIndex);
					contentsContainer.requestLayout();
					for (int i = menuOpenIndex; i < contentArray.size(); i ++) {
						View itemView = contentsContainer.getChildAt(i);
						ImageButton buttonContentItem = (ImageButton)itemView.findViewById(R.id.buttonContentItem);
						buttonContentItem.setTag(Integer.valueOf(i));
						ImageButton buttonContentFilter = (ImageButton)itemView.findViewById(R.id.buttonContentFilter);
						buttonContentFilter.setTag(Integer.valueOf(i));
					}
					
				}
				return false;
			}
			
		});
		popup.show();
	}
	
	private void browseAudioFile() {
		Intent pickAudio = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(pickAudio, PICK_AUDIO_REQ);
	}
	
	private void openFilterScroll() {
		StoryContent storyContent = contentArray.get(menuOpenIndex);
		File thumbFile = new File(storyContent.thumbPath);
		String strThumbToDisplay = storyContent.thumbPath;
		String thumbFileName = thumbFile.getName();
		File filteredThumbFile = new File(Constants.workDirectoryPath, thumbFileName + "_filtered.jpg");
		
		Videokit vk = new Videokit();
		// vintage
		String[] args = new String[] {
			"ffmpeg",
			"-y",
			"-i",
			storyContent.thumbPath,
			"-filter_complex",
			"[0:v]curves=vintage[v]",
			"-map",
			"[v]",
			filteredThumbFile.getAbsolutePath()
		};
		
		int ret = vk.run(args);
		if (ret == 0)
			strThumbToDisplay = filteredThumbFile.getAbsolutePath();
		
		Bitmap thumbBitmap = BitmapFactory.decodeFile(strThumbToDisplay);
		ImageView imageViewFilter = (ImageView)layoutFilters.findViewById(R.id.imageViewFilterVintage);
		imageViewFilter.setImageBitmap(thumbBitmap);
		
		// vignette
		strThumbToDisplay = storyContent.thumbPath;
		args = new String[] {
			"ffmpeg",
			"-y",
			"-i",
			storyContent.thumbPath,
			"-filter_complex",
			"[0:v]vignette=PI/4[v]",
			"-map",
			"[v]",
			filteredThumbFile.getAbsolutePath()
		};
		
		ret = vk.run(args);
		if (ret == 0)
			strThumbToDisplay = filteredThumbFile.getAbsolutePath();
		
		thumbBitmap = BitmapFactory.decodeFile(strThumbToDisplay);
		imageViewFilter = (ImageView)layoutFilters.findViewById(R.id.imageViewFilterVignette);
		imageViewFilter.setImageBitmap(thumbBitmap);
		
		// grayscale
		strThumbToDisplay = storyContent.thumbPath;
		args = new String[] {
			"ffmpeg",
			"-y",
			"-i",
			storyContent.thumbPath,
			"-filter_complex",
			"[0:v]colorchannelmixer=.3:.4:.3:0:.3:.4:.3:0:.3:.4:.3[v]",
			"-map",
			"[v]",
			filteredThumbFile.getAbsolutePath()
		};
		
		ret = vk.run(args);
		if (ret == 0)
			strThumbToDisplay = filteredThumbFile.getAbsolutePath();
		
		thumbBitmap = BitmapFactory.decodeFile(strThumbToDisplay);
		imageViewFilter = (ImageView)layoutFilters.findViewById(R.id.imageViewFilterGrayscale);
		imageViewFilter.setImageBitmap(thumbBitmap);
		
		// sepia tones
		strThumbToDisplay = storyContent.thumbPath;
		args = new String[] {
			"ffmpeg",
			"-y",
			"-i",
			storyContent.thumbPath,
			"-filter_complex",
			"[0:v]colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131[v]",
			"-map",
			"[v]",
			filteredThumbFile.getAbsolutePath()
		};
		
		ret = vk.run(args);
		if (ret == 0)
			strThumbToDisplay = filteredThumbFile.getAbsolutePath();
		
		thumbBitmap = BitmapFactory.decodeFile(strThumbToDisplay);
		imageViewFilter = (ImageView)layoutFilters.findViewById(R.id.imageViewFilterSepiaTones);
		imageViewFilter.setImageBitmap(thumbBitmap);
		
		// none
		thumbBitmap = BitmapFactory.decodeFile(storyContent.thumbPath);
		imageViewFilter = (ImageView)layoutFilters.findViewById(R.id.imageViewFilterNone);
		imageViewFilter.setImageBitmap(thumbBitmap);
		
		layoutFilters.setVisibility(View.VISIBLE);
		Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.fadein);
		anim.setDuration(300);
		layoutFilters.setAnimation(anim);
	}
	
	private void onButtonDone() {
		int contentCount = contentArray.size();
		if (contentCount != 6 &&
			contentCount != 12 &&
			contentCount != 18 &&
			contentCount != 24)
		{
			new AlertDialog.Builder(getActivity())
			.setMessage("The content count should be 6, 12, 18, or 24.")
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
		
		String audioFilePath = editTextAudioPath.getText().toString();
		int transitionIndex = spinnerTransition.getSelectedItemPosition();
		
		if (sourceMode == Constants.SOURCE_VIDEO) {
			float[] durations = new float[contentArray.size()];
			float min_duration = 60.f;
			for (int i = 0; i < contentArray.size(); i ++) {
				StoryContent storyContent = contentArray.get(i);
				MediaPlayer mp = MediaPlayer.create(getActivity(), Uri.parse(storyContent.contentPath));
				durations[i] = (float)mp.getDuration() / 1000.f;
				mp.release();
				if (min_duration > durations[i])
					min_duration = durations[i];
			}
			float transduration;
			if (transitionIndex != 0) {
				transduration = Constants.TRANSITION_DURATION;
				if ((transduration * 3.f) > min_duration)
					transduration = min_duration / 3.f;
			} else {
				transduration = 0.f;
			}
			
			float duration_sum = 0.f;
			for (int i = 0; i < contentArray.size(); i ++) {
				if (contentArray.size() == 1) {
					duration_sum += durations[i];
				} else if (i == 0) {
					duration_sum += durations[i] - (transduration / 2.f);
				} else if (i == (contentArray.size() - 1)) {
					duration_sum += durations[i] - (transduration / 2.f);
				} else {
					duration_sum += durations[i] - transduration;
				}
			}
			if (duration_sum < Constants.STORY_DURATION) {
				new AlertDialog.Builder(getActivity())
				.setMessage("The final story duration must be at least " + Constants.STORY_DURATION + " seconds.")
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
		}
		
		writeStoryWithContents(contentArray, audioFilePath, transitionIndex);
	}
	
	private Bitmap decodePath(String imagePath, int desiredWidth, int desiredHeight) throws Exception {
		
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;
		FileInputStream is = new FileInputStream(imagePath);
		BitmapFactory.decodeStream(new FileInputStream(imagePath), null, o);
		is.close();
		
		int width_tmp = o.outWidth, height_tmp = o.outHeight;
		int scale = 1;
		while (true) {
			if (width_tmp / 2 < desiredWidth ||
				height_tmp / 2 < desiredHeight) {
				break;
			}
			width_tmp /= 2;
			height_tmp /= 2;
			scale *= 2;
		}
		
		BitmapFactory.Options o2 = new BitmapFactory.Options();
		o2.inSampleSize = scale;
		is = new FileInputStream(imagePath);
		Bitmap bitmap = BitmapFactory.decodeStream(is, null, o2);
		is.close();
		return bitmap;
	}
	
	private int getBitmapRotation(String imagePath) {
		ExifInterface exif;
		int orientation = 0;
		try {
			exif = new ExifInterface(imagePath);
			orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		int rotation = 0;
		switch (orientation) {
		case ExifInterface.ORIENTATION_ROTATE_180:
			rotation = 180;
			break;
		case ExifInterface.ORIENTATION_ROTATE_90:
			rotation = 90;
			break;
		case ExifInterface.ORIENTATION_ROTATE_270:
			rotation = 270;
			break;
		}
		  
		return rotation;
	}
	
	private Bitmap adjustedBitmap(Bitmap bitmap, int desiredWidth, int desiredHeight) {
		
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		float scaleX = (float)desiredWidth / (float)width;
		float scaleY = (float)desiredHeight / (float)height;
		float scale = scaleX < scaleY ? scaleX : scaleY;
		int scaled_width = (int)(scale * width);
		int scaled_height = (int)(scale * height);
		
		Bitmap newBitmap = Bitmap.createBitmap(desiredWidth, desiredHeight, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(newBitmap);
		canvas.drawColor(Color.BLACK, PorterDuff.Mode.SRC);
		int left = (desiredWidth - scaled_width) / 2;
		int top = (desiredHeight - scaled_height) / 2;
		canvas.drawBitmap(bitmap, null, new Rect(left, top, left + scaled_width, top + scaled_height), new Paint());
		
		return newBitmap;
	}
	
	private void writeStoryWithContents(ArrayList<StoryContent> contents, String audioFile, int transition) {
		
		storyBuilder = new StoryBuilder(getActivity(), sourceMode, contents, audioFile, transition);
		storyBuilder.setOnCompleteListener(new OnCompleteListener() {
			
			@Override
			public void onComplete(String outputFile) {
				progressDialog.dismiss();
				storyBuilder = null;
				if (outputFile.equals("")) {
					Toast.makeText(getActivity(), "Failed to write story.", Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(getActivity(), "Story written.", Toast.LENGTH_LONG).show();
					
					Intent galleryIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
					Uri outputFileUri = Uri.fromFile(new File(outputFile));
					galleryIntent.setData(outputFileUri);
					getActivity().sendBroadcast(galleryIntent);
					
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.fromFile(new File(outputFile)), "video/*");
					startActivity(intent);
				}
			}
		});
		progressDialog.show();
		storyBuilder.start();
		
	}

}
