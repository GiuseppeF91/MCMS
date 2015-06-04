package com.rjafri.mcms.fragments;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.videoeditor.AudioTrack;
import android.media.videoeditor.EffectColor;
import android.media.videoeditor.MediaItem;
import android.media.videoeditor.MediaProperties;
import android.media.videoeditor.MediaVideoItem;
import android.media.videoeditor.Transition;
import android.media.videoeditor.TransitionSliding;
import android.media.videoeditor.VideoEditor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
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
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.rjafri.mcms.R;
import com.rjafri.mcms.models.Constants;
import com.rjafri.mcms.veservice.ApiService;
import com.rjafri.mcms.veservice.ApiServiceListener;
import com.rjafri.mcms.veservice.MovieEffect;
import com.rjafri.mcms.veservice.MovieMediaItem;
import com.rjafri.mcms.veservice.MovieTransition;
import com.rjafri.mcms.veservice.VideoEditorProject;
import com.rjafri.mcms.veutil.FileUtils;
import com.rjafri.mcms.veutil.StringUtils;
import com.rjafri.mcms.views.MySurfaceView;
import com.videokit.ColorConverter;

@SuppressLint("InflateParams") 
public class WriteStoryNFragment extends Fragment implements OnClickListener, MySurfaceView.Callback {
	
	private static final int PICK_CONTENT_REQ = 1;
	private static final int PICK_AUDIO_REQ = 2;
	
	private int sourceMode = Constants.SOURCE_PHOTO;
	private int menuOpenIndex = -1;
	private ArrayList<String> mediaItemIds = new ArrayList<String>();
	
	Handler handler = new Handler();
	
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
	
	private String mProjectPath;
	private VideoEditorProject mProject;
	private String exportFilePath;
	private final ServiceListener mServiceListener = new ServiceListener();
	
	static {
		ColorConverter.setEncoderInputColorFormat(ColorConverter.COLOR_FormatYUV420SemiPlanar);
		ColorConverter.setDecoderOutputColorFormat(ColorConverter.COLOR_FormatYUV420SemiPlanar);
	}
	
	public WriteStoryNFragment() {
		this.sourceMode = Constants.SOURCE_PHOTO;
	}
	
	public WriteStoryNFragment(int sourceMode) {
		this.sourceMode = sourceMode;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_writestory_n, container, false);
		
		createNewProject();
		
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
				ApiService.cancelExportVideoEditor(
						WriteStoryNFragment.this.getActivity(), mProjectPath, exportFilePath);
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
		TextView textViewFilterGradient = (TextView)view.findViewById(R.id.textViewFilterGradient);
		textViewFilterGradient.setTypeface(Constants.myFontTypeface);
		TextView textViewFilterGrayscale = (TextView)view.findViewById(R.id.textViewFilterGrayscale);
		textViewFilterGrayscale.setTypeface(Constants.myFontTypeface);
		TextView textViewFilterSepiaTones = (TextView)view.findViewById(R.id.textViewFilterSepiaTones);
		textViewFilterSepiaTones.setTypeface(Constants.myFontTypeface);
		
		Button buttonCloseFilters = (Button)view.findViewById(R.id.buttonCloseFilters);
		Button buttonFilterNone = (Button)view.findViewById(R.id.buttonFilterNone);
		Button buttonFilterGradient = (Button)view.findViewById(R.id.buttonFilterGradient);
		Button buttonFilterGrayscale = (Button)view.findViewById(R.id.buttonFilterGrayscale);
		Button buttonFilterSepiaTones = (Button)view.findViewById(R.id.buttonFilterSepiaTones);
		buttonCloseFilters.setOnClickListener(this);
		buttonFilterNone.setOnClickListener(this);
		buttonFilterGradient.setOnClickListener(this);
		buttonFilterGrayscale.setOnClickListener(this);
		buttonFilterSepiaTones.setOnClickListener(this);
		
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		ApiService.registerListener(mServiceListener);
		// Check if we need to load the project
        if (mProjectPath != null &&
        	mProject == null) {
            ApiService.loadVideoEditor(getActivity(), mProjectPath);
        }
	}
	
	@Override
	public void onPause() {
		super.onPause();
		ApiService.unregisterListener(mServiceListener);
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
		case R.id.buttonFilterGradient:
		case R.id.buttonFilterGrayscale:
		case R.id.buttonFilterSepiaTones:
			// Remove all filter effects
			String mediaItemId = mediaItemIds.get(menuOpenIndex);
			MovieMediaItem mediaItem = mProject.getMediaItem(mediaItemId);
			MovieEffect effect = mediaItem.getEffect();
			if (effect != null) {
				ApiService.removeEffect(getActivity(), mProjectPath, mediaItemId, effect.getId());
			}
			
			if (R.id.buttonFilterNone != v.getId()) {
				int effectType = EffectColor.TYPE_COLOR;
				int effectColor = -1;
				if (v.getId() == R.id.buttonFilterGradient) {
					effectType = EffectColor.TYPE_GRADIENT;
					effectColor = EffectColor.GRAY;
				} else if (v.getId() == R.id.buttonFilterGrayscale) {
					effectType = EffectColor.TYPE_COLOR;
					effectColor = EffectColor.GRAY;
				} else if (v.getId() == R.id.buttonFilterSepiaTones) {
					effectType = EffectColor.TYPE_SEPIA;
					effectColor = -1;
				}
				ApiService.addEffectColor(getActivity(), mProjectPath, mediaItemId,
						ApiService.generateId(), 0, mediaItem.getDuration(), effectType, effectColor);
			}
			
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
				addPhoto(selectedImage);
			} else {
				Uri selectedVideo = data.getData();
				addVideo(selectedVideo);
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
	
	@Override
	public void surfaceChanged(MySurfaceView view, SurfaceHolder holder, int format, int width, int height) {
		Log.e("WriteStoryNFragment", "surfaceChanged");
		int contentIndex = ((Integer)view.getTag()).intValue();
		if (contentIndex < 0 ||
			contentIndex >= mediaItemIds.size())
			return;
		String mediaItemId = mediaItemIds.get(contentIndex);
		updateThumbnailOfMediaItem(mediaItemId);
	}

	@Override
	public void surfaceCreated(MySurfaceView view, SurfaceHolder holder) {
		Log.e("WriteStoryNFragment", "surfaceCreated");
		
	}

	@Override
	public void surfaceDestroyed(MySurfaceView view, SurfaceHolder holder) {
		Log.e("WriteStoryNFragment", "surfaceDestroyed");
	}
	
	public void onReplaceToOtherFragment() {
		ApiService.unregisterListener(mServiceListener);
		if (mProjectPath != null) {
            ApiService.releaseVideoEditor(getActivity(), mProjectPath);
            mProjectPath = null;
        }
		FileUtils.clearDirectory(Constants.workDirectoryPath);
	}
	
	private void createNewProject() {
		File projectDir = new File(Constants.workDirectoryPath, StringUtils.randomString(10));
		mProjectPath = projectDir.getAbsolutePath();
		String projectName = StringUtils.randomString(10);
		ApiService.createVideoEditor(getActivity(), mProjectPath,
				projectName, new String[0], new String[0], null);
	}
	
	private void addContent() {
		int contentsCount = mediaItemIds.size();
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
	
	private void addPhoto(Uri imageUri) {
		String prevMediaItemId = null;
		if (mediaItemIds.size() > 0)
			prevMediaItemId = mediaItemIds.get(mediaItemIds.size() - 1);
			
		String mediaItemId = ApiService.generateId();
		ApiService.addMediaItemImageUri(getActivity(), mProjectPath,
				mediaItemId, prevMediaItemId, imageUri,
				MediaItem.RENDERING_MODE_BLACK_BORDER,
				1000, mProject.getTheme());
	}
	
	private void addVideo(Uri videoUri) {
		String prevMediaItemId = null;
		if (mediaItemIds.size() > 0)
			prevMediaItemId = mediaItemIds.get(mediaItemIds.size() - 1);
		
		String mediaItemId = ApiService.generateId();
		ApiService.addMediaItemVideoUri(getActivity(), mProjectPath,
                mediaItemId, prevMediaItemId, videoUri,
                MediaItem.RENDERING_MODE_BLACK_BORDER,
                mProject.getTheme());
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
					
					String mediaItemId = mediaItemIds.get(menuOpenIndex);
					MovieMediaItem mediaItem = mProject.getMediaItem(mediaItemId);
					Intent openIntent = new Intent(Intent.ACTION_VIEW);
					if (mediaItem.isImage())
						openIntent.setDataAndType(Uri.fromFile(new File(mediaItem.getFilename())), "image/*");
					else
						openIntent.setDataAndType(Uri.fromFile(new File(mediaItem.getFilename())), "video/*");
					startActivity(openIntent);
					
				} else if (item.getItemId() == R.id.set_title) {
					
					final EditText input = new EditText(getActivity());
					input.setMaxLines(1);
					input.setSingleLine();
					input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(Constants.OVERLAY_LINE_LENGTH * Constants.OVERLAY_MAX_LINES)});
					input.setTextColor(getResources().getColor(R.color.black));
					input.setTextSize(getResources().getDimension(R.dimen.common_font_size_2));
					input.setBackgroundResource(R.drawable.edittext_background);
					
					new AlertDialog.Builder(getActivity())
					.setTitle("Input Title")
					.setView(input)
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
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
					
					String mediaItemId = mediaItemIds.get(menuOpenIndex);
					ApiService.removeMediaItem(WriteStoryNFragment.this.getActivity(),
							mProjectPath, mediaItemId, null);
					
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
		layoutFilters.setVisibility(View.VISIBLE);
		Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.fadein);
		anim.setDuration(300);
		layoutFilters.setAnimation(anim);
	}
	
	private void updateThumbnailOfMediaItem(String mediaItemId) {
		MovieMediaItem mediaItem = mProject.getMediaItem(mediaItemId);
		if (mediaItem == null)
			return;
		
		int contentIndex = -1;
		for (int i = 0; i < mediaItemIds.size(); i ++) {
			if (mediaItemIds.get(i).equals(mediaItemId)) {
				contentIndex = i;
				break;
			}
		}
		if (contentIndex == -1)
			return;
		
		View contentItemView = contentsContainer.getChildAt(contentIndex);
		if (contentItemView == null)
			return;
		SurfaceView surfaceViewContentItem = (SurfaceView)contentItemView.findViewById(R.id.surfaceViewContentItem);
		
		long timeMs = 0;
		for (int i = 0; i < contentIndex; i ++) {
			MovieMediaItem precedingItem = mProject.getMediaItem(mediaItemIds.get(i));
			timeMs += precedingItem.getAppBoundaryEndTime() - precedingItem.getAppBoundaryBeginTime();
		}
		timeMs += (mediaItem.getAppBoundaryEndTime() - mediaItem.getAppBoundaryBeginTime()) / 2;
		
		mProject.renderPreviewFrame(surfaceViewContentItem.getHolder(), timeMs, new VideoEditor.OverlayData());
	}
	
	private void onButtonDone() {
		int contentCount = mediaItemIds.size();
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
		
		writeStory();
	}
	
	private void writeStory() {
		
		ApiService.setAspectRatio(getActivity(), mProjectPath, MediaProperties.ASPECT_RATIO_4_3);
		
		// Remove all previous transitions
		for (int i = 0; i < mediaItemIds.size() - 1; i ++) {
			MovieMediaItem mediaItem = mProject.getMediaItem(mediaItemIds.get(i));
			if (mediaItem.getBeginTransition() != null)
				ApiService.removeTransition(getActivity(), mProjectPath, mediaItem.getBeginTransition().getId());
			if (mediaItem.getEndTransition() != null)
				ApiService.removeTransition(getActivity(), mProjectPath, mediaItem.getEndTransition().getId());
		}
		
		int transitionIndex = spinnerTransition.getSelectedItemPosition();
		
		if (sourceMode == Constants.SOURCE_PHOTO) {
			
			int clip_duration = Constants.STORY_DURATION_MS / mediaItemIds.size();
			int transduration;
			if (transitionIndex != 0) {
				transduration = Constants.TRANSITION_DURATION_MS;
				if ((transduration * 3) > clip_duration)
					transduration = clip_duration / 3;
			} else {
				transduration = 0;
			}
			
			for (int i = 0; i < mediaItemIds.size(); i ++) {
				int dur = clip_duration;
				if (i == 0 ||
					i == (mediaItemIds.size() - 1))
					dur += transduration / 2;
				else
					dur += transduration;
				ApiService.setMediaItemDuration(getActivity(), mProjectPath, mediaItemIds.get(i), dur);
			}
			
			if (transitionIndex != 0) {
				for (int i = 0; i < mediaItemIds.size() - 1; i ++) {
					if (transitionIndex == 1) {
						ApiService.insertCrossfadeTransition(getActivity(),
								mProjectPath, mediaItemIds.get(i), ApiService.generateId(),
								transduration, Transition.BEHAVIOR_LINEAR);
					} else if (transitionIndex == 2) {
						ApiService.insertSlidingTransition(getActivity(),
								mProjectPath, mediaItemIds.get(i), ApiService.generateId(),
								transduration, Transition.BEHAVIOR_SPEED_UP, TransitionSliding.DIRECTION_LEFT_OUT_RIGHT_IN);
					} else if (transitionIndex == 3) {
						ApiService.insertSlidingTransition(getActivity(),
								mProjectPath, mediaItemIds.get(i), ApiService.generateId(),
								transduration, Transition.BEHAVIOR_SPEED_DOWN, TransitionSliding.DIRECTION_TOP_OUT_BOTTOM_IN);
					}
				}
			}
			
		} else {
			
			int min_duration = Constants.STORY_DURATION_MS;
			int transduration = Constants.TRANSITION_DURATION_MS;
			int[] durations = new int[mediaItemIds.size()];
			for (int i = 0; i < mediaItemIds.size(); i ++) {
				MovieMediaItem mediaItem = mProject.getMediaItem(mediaItemIds.get(i));
				durations[i] = (int)mediaItem.getDuration();
				if (min_duration > durations[i])
					min_duration = durations[i];
			}
			
			if (transitionIndex != 0) {
				transduration = Constants.TRANSITION_DURATION_MS;
				if ((transduration * 3.f) > min_duration)
					transduration = min_duration / 3;
			} else {
				transduration = 0;
			}
			
			int duration_sum = 0;
			for (int i = 0; i < mediaItemIds.size(); i ++) {
				if (i == 0) {
					duration_sum += durations[i];
				} else {
					duration_sum += durations[i] - transduration;
				}
				if (duration_sum >= Constants.STORY_DURATION_MS) {
					int duration_to_cut = duration_sum - Constants.STORY_DURATION_MS;
					int lastclip_duration = durations[i] - duration_to_cut;
					ApiService.setMediaItemBoundaries(getActivity(), mProjectPath, mediaItemIds.get(i), 0, lastclip_duration);
					for (int j = i + 1; j < mediaItemIds.size(); j ++) {
						ApiService.setMediaItemBoundaries(getActivity(), mProjectPath, mediaItemIds.get(j), 0, 0);
					}
					break;
				} else {
					if (i != (mediaItemIds.size() - 1)) {
						if (transitionIndex == 1) {
							ApiService.insertCrossfadeTransition(getActivity(),
									mProjectPath, mediaItemIds.get(i), ApiService.generateId(),
									transduration, Transition.BEHAVIOR_LINEAR);
						} else if (transitionIndex == 2) {
							ApiService.insertSlidingTransition(getActivity(),
									mProjectPath, mediaItemIds.get(i), ApiService.generateId(),
									transduration, Transition.BEHAVIOR_SPEED_UP, TransitionSliding.DIRECTION_LEFT_OUT_RIGHT_IN);
						} else if (transitionIndex == 3) {
							ApiService.insertSlidingTransition(getActivity(),
									mProjectPath, mediaItemIds.get(i), ApiService.generateId(),
									transduration, Transition.BEHAVIOR_SPEED_DOWN, TransitionSliding.DIRECTION_TOP_OUT_BOTTOM_IN);
						}
					}
				}
			}
			if (duration_sum < Constants.STORY_DURATION_MS) {
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
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
		String fileName = dateFormat.format(new Date());
		String fileNameWithExt = fileName + ".mp4";
		File outputFile = new File(Constants.storiesDirectoryPath, fileNameWithExt);
		exportFilePath = outputFile.getAbsolutePath();
		
		progressDialog.show();
		ApiService.exportVideoEditor(getActivity(), mProjectPath,
				exportFilePath, Constants.STORY_HEIGHT, MediaProperties.BITRATE_2M);
	}
	
	
	/**
     * The service listener
     */
    private class ServiceListener extends ApiServiceListener {
    	
    	@Override
        public void onVideoEditorCreated(String projectPath, VideoEditorProject project,
                List<MediaItem> mediaItems, List<AudioTrack> audioTracks, Exception exception) {
    		// Check if the VideoEditor is the one we are expecting
    		if (!projectPath.equals(mProjectPath)) {
                return;
            }
    		
    		mProject = project;
    	}
    	
    	@Override
        public void onVideoEditorLoaded(String projectPath, VideoEditorProject project,
                List<MediaItem> mediaItems, List<AudioTrack> audioTracks, Exception exception) {
            if (!projectPath.equals(mProjectPath)) {
                return;
            }
            
            mProject = project;
    	}
    	
    	@Override
        public void onMediaItemAdded(String projectPath, String mediaItemId,
                MovieMediaItem mediaItem, String afterMediaItemId, Class<?> mediaItemClass,
                Integer newAspectRatio, Exception exception) {
            // Check if the VideoEditor is the one we are expecting.
            if (!projectPath.equals(mProjectPath) || mProject == null) {
                return;
            }

            if (exception != null) {
                if (mediaItemClass.getCanonicalName().equals(
                        MediaVideoItem.class.getCanonicalName())) {
                    Toast.makeText(WriteStoryNFragment.this.getActivity(),
                            "Video clip is corrupted.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(WriteStoryNFragment.this.getActivity(),
                    		"Image clip is corrupted.", Toast.LENGTH_LONG).show();
                }
            } else {
            	final String itemId = mediaItemId;
            	handler.post(new Runnable() {
					@Override
					public void run() {
						
						int contentIndex = WriteStoryNFragment.this.mediaItemIds.size();
						mediaItemIds.add(itemId);
						
						RelativeLayout layoutContentItem = (RelativeLayout)getActivity().getLayoutInflater().inflate(R.layout.content_item_n, null);
						MySurfaceView surfaceViewContentItem = (MySurfaceView)layoutContentItem.findViewById(R.id.surfaceViewContentItem);
						surfaceViewContentItem.setCallback(WriteStoryNFragment.this);
						surfaceViewContentItem.setTag(Integer.valueOf(contentIndex));
		        		TextView textViewContentItem = (TextView)layoutContentItem.findViewById(R.id.textViewContentItem);
		        		textViewContentItem.setText("No Title");
		        		textViewContentItem.setTypeface(Constants.myFontTypeface);
		        		TextView textViewContentDuration = (TextView)layoutContentItem.findViewById(R.id.textViewContentDuration);
		        		textViewContentDuration.setVisibility(View.GONE);
		        		ImageButton buttonContentItem = (ImageButton)layoutContentItem.findViewById(R.id.buttonContentItem);
		        		buttonContentItem.setTag(Integer.valueOf(contentIndex));
		        		buttonContentItem.setOnClickListener(WriteStoryNFragment.this);
		        		ImageButton buttonContentFilter = (ImageButton)layoutContentItem.findViewById(R.id.buttonContentFilter);
		        		buttonContentFilter.setTag(Integer.valueOf(contentIndex));
		        		buttonContentFilter.setOnClickListener(WriteStoryNFragment.this);
		        		
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
						
					}
            	});
            }
        }
    	
    	@Override
        public void onMediaItemRemoved(String projectPath, String mediaItemId,
                MovieTransition transition, Exception exception) {
            // Check if the VideoEditor is the one we are expecting
            if (!projectPath.equals(mProjectPath)) {
                return;
            }

            if (mProject == null) {
                return;
            }

            int removedIndex = -1;
            for (int i = 0; i < mediaItemIds.size(); i ++) {
            	if (mediaItemId.equals(mediaItemIds.get(i))) {
            		removedIndex = i;
            	}
            }
            if (removedIndex != -1) {
	            mediaItemIds.remove(removedIndex);
				contentsContainer.removeViewAt(removedIndex);
				contentsContainer.requestLayout();
				for (int i = removedIndex; i < mediaItemIds.size(); i ++) {
					View itemView = contentsContainer.getChildAt(i);
					MySurfaceView surfaceViewContentItem = (MySurfaceView)itemView.findViewById(R.id.surfaceViewContentItem);
					surfaceViewContentItem.setTag(Integer.valueOf(i));
					ImageButton buttonContentItem = (ImageButton)itemView.findViewById(R.id.buttonContentItem);
					buttonContentItem.setTag(Integer.valueOf(i));
					ImageButton buttonContentFilter = (ImageButton)itemView.findViewById(R.id.buttonContentFilter);
					buttonContentFilter.setTag(Integer.valueOf(i));
				}
            }
        }
    	
    	@Override
        public void onVideoEditorExportComplete(String projectPath, String filename,
                Exception exception, boolean cancelled) {
            // Check if the VideoEditor is the one we are expecting
            if (!projectPath.equals(mProjectPath)) {
                return;
            }
            
            WriteStoryNFragment.this.progressDialog.dismiss();
            
            if (exception != null) {
            	Toast.makeText(getActivity(), "Error occured.", Toast.LENGTH_LONG).show();
            	new File(WriteStoryNFragment.this.exportFilePath).delete();
            	return;
            }
            
            if (cancelled == false) {
            	// Cancel all previous trim actions
        		if (sourceMode == Constants.SOURCE_VIDEO) {
        			for (int i = 0; i < mediaItemIds.size(); i ++) {
        				MovieMediaItem mediaItem = mProject.getMediaItem(mediaItemIds.get(i));
        				ApiService.setMediaItemBoundaries(
        						getActivity(), mProjectPath, mediaItemIds.get(i), 0, mediaItem.getDuration());
        			}
        		}
            	
	            Toast.makeText(getActivity(), "Story written.", Toast.LENGTH_LONG).show();
				
				Intent galleryIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
				Uri outputFileUri = Uri.fromFile(new File(filename));
				galleryIntent.setData(outputFileUri);
				getActivity().sendBroadcast(galleryIntent);
				
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.fromFile(new File(filename)), "video/*");
				startActivity(intent);
            }
        }
    	
    	@Override
        public void onEffectAdded(String projectPath, MovieEffect effect, String mediaItemId,
                Exception exception) {
            // Check if the VideoEditor is the one we are expecting
            if (!projectPath.equals(mProjectPath)) {
                return;
            }

            if (mProject == null) {
                return;
            }

            final String itemId = mediaItemId;
            handler.post(new Runnable() {
				@Override
				public void run() {
					WriteStoryNFragment.this.updateThumbnailOfMediaItem(itemId);
				}
            });
        }

        @Override
        public void onEffectRemoved(String projectPath, String effectId, String mediaItemId,
                Exception exception) {
            // Check if the VideoEditor is the one we are expecting
            if (!projectPath.equals(mProjectPath)) {
                return;
            }

            if (mProject == null) {
                return;
            }

            final String itemId = mediaItemId;
            handler.post(new Runnable() {
				@Override
				public void run() {
					WriteStoryNFragment.this.updateThumbnailOfMediaItem(itemId);
				}
            });
        }
    	
    }

}