package com.rjafri.mcms.models;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.videokit.Videokit;

public class StoryBuilder {

	Context context;
	int contentType;
	ArrayList<StoryContent> clipsArray;
	String audioFilePath;
	int transition;
	
	FFMpegTask ffmpegTask = null;
	
	OnCompleteListener onCompleteListener = null;
	
	public StoryBuilder(Context context, int contentType, ArrayList<StoryContent> clipsArray, String audioFile, int transition) {
		this.context = context;
		this.contentType = contentType;
		this.clipsArray = clipsArray;
		this.audioFilePath = audioFile;
		this.transition = transition;
	}
	
	public void setOnCompleteListener(OnCompleteListener l) {
		this.onCompleteListener = l;
	}
	
	public void start() {
		// Check the validity of clips
		boolean isValid = true;
		for (int i = 0; i < clipsArray.size(); i ++) {
			StoryContent storyContent = clipsArray.get(i);
			if (storyContent.contentType != contentType) {
				isValid = false;
				break;
			}
		}
		if (isValid == false) {
			if (onCompleteListener != null)
				onCompleteListener.onComplete("");
			return;
		}
		
		ffmpegTask =  new FFMpegTask();
		ffmpegTask.execute(clipsArray, audioFilePath, Integer.valueOf(transition), Integer.valueOf(contentType));
	}
	
	public void stop() {
		Log.e("StoryBuilder", "stop()");
		if (ffmpegTask == null)
			return;
		ffmpegTask.stop();
		ffmpegTask = null;
	}
	
	public interface OnCompleteListener {
		public void onComplete(String outputFile);
	}
	
	private class FFMpegTask extends AsyncTask<Object, Void, String> {

		private String outputFilePath = "";
		private Videokit videokit = null;
		
		@Override
		protected String doInBackground(Object... params) {
			
			int paramCount = params.length;
			if (paramCount != 4)
				return outputFilePath;
			@SuppressWarnings("unchecked")
			ArrayList<StoryContent> clipsArray = (ArrayList<StoryContent>)params[0];
			String audioFilePath = (String)params[1];
			int transition = ((Integer)params[2]).intValue();
			int contentType = ((Integer)params[3]).intValue();
			
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
			String fileName = dateFormat.format(new Date());
			String fileNameWithExt = fileName + ".mp4";
			File outputFile = new File(Constants.storiesDirectoryPath, fileNameWithExt);
			outputFilePath = outputFile.getAbsolutePath();
			
			ArrayList<String> argArray = new ArrayList<String>();
			argArray.add("ffmpeg");
			argArray.add("-y");
			for (int i = 0; i < clipsArray.size(); i ++) {
				StoryContent storyContent = clipsArray.get(i);
				if (storyContent.contentType == Constants.SOURCE_PHOTO) {
					argArray.add("-loop");
					argArray.add("1");
					argArray.add("-i");
					argArray.add(storyContent.contentPath);
				} else {
					argArray.add("-i");
					argArray.add(storyContent.contentPath);
				}
			}
			int milli_duration = 0;
			if (!audioFilePath.equals("")) {
				MediaPlayer mp = MediaPlayer.create(context, Uri.parse(audioFilePath));
				milli_duration = mp.getDuration();
				mp.release();
				Log.i("StoryBuilder", "Audio duration : " + milli_duration);
			}
			if (milli_duration > 0) {
				argArray.add("-i");
				argArray.add(audioFilePath);
			}
			float audio_duration = (float)milli_duration / 1000.f;
			String complexFilterGraph = getFilterGraphString(clipsArray, transition, contentType);
			if (milli_duration > 0) {
				if (audio_duration < Constants.STORY_DURATION) {
					int repeat_count = (int)Math.ceil(Constants.STORY_DURATION / audio_duration);
					Log.i("StoryBuilder", "Repeat count : " + repeat_count);
					String audioFilterGraph = "";
					for (int i = 0; i < repeat_count; i ++)
						audioFilterGraph += "[" + clipsArray.size() + ":a]";
					audioFilterGraph += "concat=n=" + repeat_count + ":v=0:a=1[aout]";
					Log.i("StoryBuilder", audioFilterGraph);
					complexFilterGraph += ";" + audioFilterGraph;
				}
			} else if (contentType == Constants.SOURCE_VIDEO && milli_duration <= 0) {
				String audioFilterGraph = "";
				if (transition == 0) {
					// No transition
					for (int i = 0; i < clipsArray.size(); i ++)
						audioFilterGraph += "[" + i + ":a]";
					audioFilterGraph += "concat=n=" + clipsArray.size() + ":v=0:a=1[aout]";
				} else {
					// Transition
					float[] durations = new float[clipsArray.size()];
					float min_duration = 60.f;
					for (int i = 0; i < clipsArray.size(); i ++) {
						StoryContent storyContent = clipsArray.get(i);
						MediaPlayer mp = MediaPlayer.create(StoryBuilder.this.context, Uri.parse(storyContent.contentPath));
						durations[i] = (float)mp.getDuration() / 1000.f;
						mp.release();
						if (min_duration > durations[i])
							min_duration = durations[i];
					}
					float transduration = Constants.TRANSITION_DURATION;
					if ((transduration * 3.f) > min_duration)
						transduration = min_duration / 3.f;
					
					for (int i = 0; i < clipsArray.size(); i ++) {
						if (clipsArray.size() == 1) {
							audioFilterGraph += "[" + i + ":a]atrim=duration=" + durations[i] + "[a" + i + "];";
						} else if (i == 0) {
							audioFilterGraph += "[" + i + ":a]atrim=duration=" +
									String.valueOf(durations[i] - (transduration / 2.f)) + "[a" + i + "];";
						} else if (i == (clipsArray.size() - 1)) {
							audioFilterGraph += "[" + i + ":a]atrim=duration=" +
									String.valueOf(durations[i] - (transduration / 2.f)) +
									":start=" + String.valueOf(transduration / 2.f) + "[a" + i + "];";
						} else {
							audioFilterGraph += "[" + i + ":a]atrim=duration=" +
									String.valueOf(durations[i] - transduration) +
									":start=" + String.valueOf(transduration / 2.f) + "[a" + i + "];";
						}
					}
					
					for (int i = 0; i < clipsArray.size(); i ++)
						audioFilterGraph += "[a" + i + "]";
					audioFilterGraph += "concat=n=" + clipsArray.size() + ":v=0:a=1[aout]";
				}
				Log.i("StoryBuilder", audioFilterGraph);
				complexFilterGraph += ";" + audioFilterGraph;
			}
			
			argArray.add("-filter_complex");
			argArray.add(complexFilterGraph);
			argArray.add("-map");
			argArray.add("[out]");
			argArray.add("-c:v");
			argArray.add("mpeg4");
			argArray.add("-qscale:v");
			argArray.add("1");
//			argArray.add("-g");
//			argArray.add("0");
//			argArray.add("libx264");
//			argArray.add("-preset");
//			argArray.add("ultrafast");
//			argArray.add("-profile:v");
//			argArray.add("baseline");
//			argArray.add("-level");
//			argArray.add("3.0");
			if (milli_duration > 0) {
				if (audio_duration >= Constants.STORY_DURATION) {
					argArray.add("-map");
					argArray.add(clipsArray.size() + ":a");
				} else {
					argArray.add("-map");
					argArray.add("[aout]");
				}
				argArray.add("-c:a");
				argArray.add("mp2");
			} else if (contentType == Constants.SOURCE_VIDEO) {
				argArray.add("-map");
				argArray.add("[aout]");
				argArray.add("-c:a");
				argArray.add("mp2");
			}
			argArray.add("-t");
			argArray.add(String.valueOf(Constants.STORY_DURATION));
			argArray.add(outputFilePath);
			
			videokit = new Videokit();
			int ret = videokit.run(argArray.toArray(new String[0]));
			if (ret != 0) {
				outputFile.delete();
				outputFilePath = "";
			}
			
			return outputFilePath;
		}
		
		@Override
		protected void onPostExecute(String result) {
			if (StoryBuilder.this.onCompleteListener != null)
				StoryBuilder.this.onCompleteListener.onComplete(result);
			videokit = null;
			StoryBuilder.this.ffmpegTask = null;
		}
		
		public void stop() {
			if (videokit != null)
				videokit.stop();
			cancel(true);
			videokit = null;
			if (!outputFilePath.equals("")) {
				File outputFile = new File(outputFilePath);
				outputFile.delete();
				outputFilePath = "";
			}
		}
		
		private String getFilterGraphString(ArrayList<StoryContent> clips, int transition, int contentType) {
			String strFilterGraph = "";
			
			if (contentType == Constants.SOURCE_PHOTO) {
				// Write photo story
				if (transition == 0) {
					// No Transition
					float duration = Constants.STORY_DURATION / (float)clips.size();
					String strConcatSrcs = "";
					int concatNum = 0;
					for (int i = 0; i < clips.size(); i ++) {
						
						StoryContent storyContent = clips.get(i);
						
						String strFilterEffectFilter = "";
						if (storyContent.filter == 1) {
							strFilterEffectFilter = ",curves=vintage";
						} else if (storyContent.filter == 2) {
							strFilterEffectFilter = ",vignette=PI/4";
						} else if (storyContent.filter == 3) {
							strFilterEffectFilter = ",colorchannelmixer=.3:.4:.3:0:.3:.4:.3:0:.3:.4:.3";
						} else if (storyContent.filter == 4) {
							strFilterEffectFilter = ",colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131";
						}
						
						String strTextOverlayFilter = getOverlayTextFilter(storyContent.overlayString);
						strFilterGraph += "[" + i + ":v]trim=duration=" + duration +
								strFilterEffectFilter + strTextOverlayFilter + ",setsar=sar=1/1[v" + i + "];";
						
						strConcatSrcs += "[v" + i + "]";
						concatNum ++;
					}
					strFilterGraph += strConcatSrcs + "concat=n=" + concatNum + ":v=1:a=0,format=yuv420p[out]";
					
				} else if (transition == 1) {
					// Transition is Fade
					float base_duration = Constants.STORY_DURATION / (float)clips.size();
					float transduration = Constants.TRANSITION_DURATION;
					if ((transduration * 3.f) > base_duration)
						transduration = base_duration / 3.f;
					
					for (int i = 0; i < clips.size(); i ++) {
						StoryContent storyContent = clips.get(i);
						
						float duration = base_duration;
						if (i == 0 && clips.size() == 1) {
							// If only one clip, no transition
						} else if (i == 0) {
							// For the first clip, only out transition
							duration += transduration / 2.f;
						} else if (i == (clips.size() - 1)) {
							// For the last clip, only in transition
							duration += transduration / 2.f;
						} else {
							// both in and out transitions
							duration += transduration;
						}
						
						String strFilterEffectFilter = "";
						if (storyContent.filter == 1) {
							strFilterEffectFilter = ",curves=vintage";
						} else if (storyContent.filter == 2) {
							strFilterEffectFilter = ",vignette=PI/4";
						} else if (storyContent.filter == 3) {
							strFilterEffectFilter = ",colorchannelmixer=.3:.4:.3:0:.3:.4:.3:0:.3:.4:.3";
						} else if (storyContent.filter == 4) {
							strFilterEffectFilter = ",colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131";
						}
						
						String strTextOverlayFilter = getOverlayTextFilter(storyContent.overlayString);
						
						if (clips.size() == 1) {
							strFilterGraph += "[" + i + ":v]trim=duration=" + duration + strFilterEffectFilter + strTextOverlayFilter +
									"[v" + i + "0];color=c=white:s=" + Constants.STORY_WIDTH + "x" + Constants.STORY_HEIGHT +
									"[alpha" + i + "];[v" + i + "0][alpha" + i + "]alphamerge[v" + i + "];";
						} else {
							String strFadeFilter = "";
							if (i == 0) {
								strFadeFilter = "fade=t=out:st=" + String.valueOf(duration - transduration) +
										":d=" + transduration + ":alpha=1";
							} else if (i == (clips.size() - 1)) {
								strFadeFilter = "fade=t=in:st=0:d=" + transduration + ":alpha=1";
							} else {
								strFadeFilter = "fade=t=in:st=0:d=" + transduration + ":alpha=1,fade=t=out:st=" +
										String.valueOf(duration - transduration) + ":d=" + transduration + ":alpha=1";
							}
							float startpts = 0.f;
							if (i > 0)
								startpts = (base_duration * i) - (transduration / 2.f);
							strFilterGraph += "[" + i + ":v]trim=duration=" + duration + strFilterEffectFilter + strTextOverlayFilter +
									"[v" + i + "0];color=c=white:s=" + Constants.STORY_WIDTH + "x" + Constants.STORY_HEIGHT +
									"[alpha" + i + "];[v" + i + "0][alpha" + i + "]alphamerge[v" + i + "1];" +
									"[v" + i + "1]" + strFadeFilter + ",setpts=PTS-STARTPTS+" + startpts + "/TB[v" + i + "];";
						}
					}
					
					strFilterGraph += "color=c=black:s=" + Constants.STORY_WIDTH + "x" + Constants.STORY_HEIGHT +
							",trim=duration=" + Constants.STORY_DURATION + "[over];";
					String strBackgroundStream = "[over]";
					for (int i = 0; i < clips.size(); i ++) {
						if (i == (clips.size() - 1)) {
							strFilterGraph += strBackgroundStream + "[v" + i + "]overlay=format=yuv420[out]";
						} else {
							String strOverlayResult = "[over" + i + "]";
							strFilterGraph += strBackgroundStream + "[v" + i + "]overlay" + strOverlayResult + ";";
							strBackgroundStream = strOverlayResult;
						}
					}
					
				} else {
					// Transition is Wipe/Push
					float base_duration = Constants.STORY_DURATION / (float)clips.size();
					float transduration = Constants.TRANSITION_DURATION;
					if ((transduration * 3.f) > base_duration)
						transduration = base_duration / 3.f;
					
					for (int i = 0; i < clips.size(); i ++) {
						StoryContent storyContent = clips.get(i);
						
						float duration = base_duration;
						if (i == 0 && clips.size() == 1) {
							// If only one clip, no transition
						} else if (i == 0) {
							// For the first clip, only out transition
							duration += transduration / 2.f;
						} else if (i == (clips.size() - 1)) {
							// For the last clip, only in transition
							duration += transduration / 2.f;
						} else {
							// both in and out transitions
							duration += transduration;
						}
						
						String strFilterEffectFilter = "";
						if (storyContent.filter == 1) {
							strFilterEffectFilter = ",curves=vintage";
						} else if (storyContent.filter == 2) {
							strFilterEffectFilter = ",vignette=PI/4";
						} else if (storyContent.filter == 3) {
							strFilterEffectFilter = ",colorchannelmixer=.3:.4:.3:0:.3:.4:.3:0:.3:.4:.3";
						} else if (storyContent.filter == 4) {
							strFilterEffectFilter = ",colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131";
						}
						
						String strTextOverlayFilter = getOverlayTextFilter(storyContent.overlayString);
						
						float startpts = 0.f;
						if (i > 0)
							startpts = (base_duration * i) - (transduration / 2.f);
						
						strFilterGraph += "[" + i + ":v]trim=duration=" + duration + strFilterEffectFilter + strTextOverlayFilter +
								"[v" + i + "0];[v" + i + "0]setpts=PTS-STARTPTS+" + startpts + "/TB[v" + i + "];";
					}
					
					strFilterGraph += "color=c=black:s=" + Constants.STORY_WIDTH + "x" + Constants.STORY_HEIGHT +
							",trim=duration=" + Constants.STORY_DURATION + "[over];";
					
					String strBackgroundStream = "[over]";
					for (int i = 0; i < clips.size(); i ++) {
						String strOverlayFilter = "";
						float base_timestamp = (base_duration * i) + (transduration / 2.f);
						float base_timestamp1 = base_timestamp + base_duration - transduration;
						if (transition == 2) {
							// Transition is Wipe
							if (i > 0) {
								strOverlayFilter = "=y=0:x='if(gte(t," + base_timestamp + "),0,((" + base_timestamp + "-t)/" + transduration + ")*W)'";
							}
						} else {
							// Transition is Push
							if (i == 0) {
								strOverlayFilter = "=x=0:y='if(gte(t," + base_timestamp1 + "),(0-((t-" + base_timestamp1 + ")/" + transduration + "))*H,0)'";
							} else if (i == (clips.size() - 1)) {
								strOverlayFilter = "=x=0:y='if(gte(t," + base_timestamp + "),0,((" + base_timestamp + "-t)/" + transduration + ")*H)'";
							} else {
								strOverlayFilter = "=x=0:y='if(gte(t," + base_timestamp1 + "),(0-((t-" + base_timestamp1 + ")/" + transduration + "))*H," +
													"if(gte(t," + base_timestamp + "),0,((" + base_timestamp + "-t)/" + transduration + ")*H))'";
							}
						}
						
						String strOverlayResult = "[over" + i + "]";
						if (i == 0 && i == (clips.size() - 1)) {
							strFilterGraph += strBackgroundStream + "[v" + i + "]overlay=format=yuv420[out]";
						} else if (i == (clips.size() - 1)) {
							strFilterGraph += strBackgroundStream + "[v" + i + "]overlay" + strOverlayFilter + ":format=yuv420[out]";
						} else {
							strFilterGraph += strBackgroundStream + "[v" + i + "]overlay" + strOverlayFilter + strOverlayResult + ";";
						}
						strBackgroundStream = strOverlayResult;
					}
					
				}
				
			} else {
				// Write video story
				if (transition == 0) {
					// No Transition
					String strConcatSrcs = "";
					int concatNum = 0;
					for (int i = 0; i < clips.size(); i ++) {
						
						StoryContent storyContent = clips.get(i);
						
						MediaPlayer mp = MediaPlayer.create(StoryBuilder.this.context, Uri.parse(storyContent.contentPath));
						int width = mp.getVideoWidth();
						int height = mp.getVideoHeight();
						float clip_duration = (float)mp.getDuration() / 1000.f;
						mp.release();
						
						float scaleX = (float)Constants.STORY_WIDTH / (float)width;
						float scaleY = (float)Constants.STORY_HEIGHT / (float)height;
						float scale = scaleX < scaleY ? scaleX : scaleY;
						int scaled_width = (int)(scale * width);
						int scaled_height = (int)(scale * height);
						int left = (Constants.STORY_WIDTH - scaled_width) / 2;
						int top = (Constants.STORY_HEIGHT - scaled_height) / 2;
						
						Videokit vk = new Videokit();
						String strRotation = vk.getMetaData(storyContent.contentPath, "rotate");
						if (strRotation == null)
							strRotation = "0";
						int rotateCount = 0;
						if (strRotation.equals("90"))
							rotateCount = 1;
						else if (strRotation.equals("180"))
							rotateCount = 2;
						else if (strRotation.equals("270"))
							rotateCount = 3;
						String strRotationFilter = "";
						for (int j = 0; j < rotateCount; j ++) {
							strRotationFilter += "transpose=1,";
						}
						
						String strFilterEffectFilter = "";
						if (storyContent.filter == 1) {
							strFilterEffectFilter = ",curves=vintage";
						} else if (storyContent.filter == 2) {
							strFilterEffectFilter = ",vignette=PI/4";
						} else if (storyContent.filter == 3) {
							strFilterEffectFilter = ",colorchannelmixer=.3:.4:.3:0:.3:.4:.3:0:.3:.4:.3";
						} else if (storyContent.filter == 4) {
							strFilterEffectFilter = ",colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131";
						}
						
						String strTextOverlayFilter = getOverlayTextFilter(storyContent.overlayString);
						strFilterGraph += "color=c=black:s=" + Constants.STORY_WIDTH + "x" +
										Constants.STORY_HEIGHT + ",trim=duration=" + clip_duration + "[bg" + i + "];" +
										"[" + i + ":v]" + strRotationFilter + "scale=" + scaled_width + "x" +
										scaled_height + "[s" + i + "];" + "[bg" + i + "][s" + i + "]overlay=x=" +
										left + ":y=" + top + "[o" + i + "];" + "[o" + i + "]trim=duration=" +
										clip_duration + strFilterEffectFilter + strTextOverlayFilter + "[v" + i + "];";
						
						strConcatSrcs += "[v" + i + "]";
						concatNum ++;
					}
					strFilterGraph += strConcatSrcs + "concat=n=" + concatNum + ":v=1:a=0,format=yuv420p[out]";
					
				} else if (transition == 1) {
					// Transition is Fade
					float[] durations = new float[clips.size()];
					int[] widths = new int[clips.size()];
					int[] heights = new int[clips.size()];
					float min_duration = 60.f;
					for (int i = 0; i < clips.size(); i ++) {
						StoryContent storyContent = clips.get(i);
						MediaPlayer mp = MediaPlayer.create(StoryBuilder.this.context, Uri.parse(storyContent.contentPath));
						durations[i] = (float)mp.getDuration() / 1000.f;
						widths[i] = mp.getVideoWidth();
						heights[i] = mp.getVideoHeight();
						mp.release();
						if (min_duration > durations[i])
							min_duration = durations[i];
					}
					float transduration = Constants.TRANSITION_DURATION;
					if ((transduration * 3.f) > min_duration)
						transduration = min_duration / 3.f;
					
					float start_ts = 0.f;
					for (int i = 0; i < clips.size(); i ++) {
						StoryContent storyContent = clips.get(i);
						
						float duration = durations[i];
						
						float scaleX = (float)Constants.STORY_WIDTH / (float)widths[i];
						float scaleY = (float)Constants.STORY_HEIGHT / (float)heights[i];
						float scale = scaleX < scaleY ? scaleX : scaleY;
						int scaled_width = (int)(scale * widths[i]);
						int scaled_height = (int)(scale * heights[i]);
						int left = (Constants.STORY_WIDTH - scaled_width) / 2;
						int top = (Constants.STORY_HEIGHT - scaled_height) / 2;
						
						Videokit vk = new Videokit();
						String strRotation = vk.getMetaData(storyContent.contentPath, "rotate");
						if (strRotation == null)
							strRotation = "0";
						int rotateCount = 0;
						if (strRotation.equals("90"))
							rotateCount = 1;
						else if (strRotation.equals("180"))
							rotateCount = 2;
						else if (strRotation.equals("270"))
							rotateCount = 3;
						String strRotationFilter = "";
						for (int j = 0; j < rotateCount; j ++) {
							strRotationFilter += "transpose=1,";
						}
						
						String strFilterEffectFilter = "";
						if (storyContent.filter == 1) {
							strFilterEffectFilter = ",curves=vintage";
						} else if (storyContent.filter == 2) {
							strFilterEffectFilter = ",vignette=PI/4";
						} else if (storyContent.filter == 3) {
							strFilterEffectFilter = ",colorchannelmixer=.3:.4:.3:0:.3:.4:.3:0:.3:.4:.3";
						} else if (storyContent.filter == 4) {
							strFilterEffectFilter = ",colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131";
						}
						
						String strTextOverlayFilter = getOverlayTextFilter(storyContent.overlayString);
						
						if (clips.size() == 1) {
							strFilterGraph += "color=c=black:s=" + Constants.STORY_WIDTH + "x" +
									Constants.STORY_HEIGHT + ",trim=duration=" + duration + "[bg" + i + "];" +
									"[" + i + ":v]" + strRotationFilter + "scale=" + scaled_width + "x" +
									scaled_height + "[s" + i + "];" + "[bg" + i + "][s" + i + "]overlay=x=" +
									left + ":y=" + top + "[o" + i + "];" + "[o" + i + "]trim=duration=" +
									duration + strFilterEffectFilter + strTextOverlayFilter + "[v" + i + "];";
						} else {
							String strFadeFilter = "";
							if (i == 0) {
								strFadeFilter = "fade=t=out:st=" + String.valueOf(duration - transduration) +
										":d=" + transduration + ":alpha=1";
							} else if (i == (clips.size() - 1)) {
								strFadeFilter = "fade=t=in:st=0:d=" + transduration + ":alpha=1";
							} else {
								strFadeFilter = "fade=t=in:st=0:d=" + transduration + ":alpha=1,fade=t=out:st=" +
										String.valueOf(duration - transduration) + ":d=" + transduration + ":alpha=1";
							}
							strFilterGraph += "color=c=black:s=" + Constants.STORY_WIDTH + "x" +
									Constants.STORY_HEIGHT + ",trim=duration=" + duration + "[bg" + i + "];" +
									"[" + i + ":v]" + strRotationFilter + "scale=" + scaled_width + "x" +
									scaled_height + "[s" + i + "];" + "[bg" + i + "][s" + i + "]overlay=x=" +
									left + ":y=" + top + "[o" + i + "];" + "[o" + i + "]trim=duration=" +
									duration + strFilterEffectFilter + strTextOverlayFilter + "[v" + i + "0];" +
									"color=c=white:s=" + Constants.STORY_WIDTH + "x" + Constants.STORY_HEIGHT +
									"[alpha" + i + "];[v" + i + "0][alpha" + i + "]alphamerge[v" + i + "1];" +
									"[v" + i + "1]" + strFadeFilter + ",setpts=PTS-STARTPTS+" + start_ts + "/TB[v" + i + "];";
						}
						
						start_ts += duration - transduration;
					}
					
					strFilterGraph += "color=c=black:s=" + Constants.STORY_WIDTH + "x" + Constants.STORY_HEIGHT +
							",trim=duration=" + Constants.STORY_DURATION + "[over];";
					String strBackgroundStream = "[over]";
					for (int i = 0; i < clips.size(); i ++) {
						if (i == (clips.size() - 1)) {
							strFilterGraph += strBackgroundStream + "[v" + i + "]overlay=format=yuv420[out]";
						} else {
							String strOverlayResult = "[over" + i + "]";
							strFilterGraph += strBackgroundStream + "[v" + i + "]overlay" + strOverlayResult + ";";
							strBackgroundStream = strOverlayResult;
						}
					}
					
				} else {
					// Transition is Wipe/Push
					float[] durations = new float[clips.size()];
					int[] widths = new int[clips.size()];
					int[] heights = new int[clips.size()];
					float min_duration = 60.f;
					for (int i = 0; i < clips.size(); i ++) {
						StoryContent storyContent = clips.get(i);
						MediaPlayer mp = MediaPlayer.create(StoryBuilder.this.context, Uri.parse(storyContent.contentPath));
						durations[i] = (float)mp.getDuration() / 1000.f;
						widths[i] = mp.getVideoWidth();
						heights[i] = mp.getVideoHeight();
						mp.release();
						if (min_duration > durations[i])
							min_duration = durations[i];
					}
					float transduration = Constants.TRANSITION_DURATION;
					if ((transduration * 3.f) > min_duration)
						transduration = min_duration / 3.f;
					
					float start_ts = 0.f;
					for (int i = 0; i < clips.size(); i ++) {
						StoryContent storyContent = clips.get(i);
						
						float duration = durations[i];
						
						float scaleX = (float)Constants.STORY_WIDTH / (float)widths[i];
						float scaleY = (float)Constants.STORY_HEIGHT / (float)heights[i];
						float scale = scaleX < scaleY ? scaleX : scaleY;
						int scaled_width = (int)(scale * widths[i]);
						int scaled_height = (int)(scale * heights[i]);
						int left = (Constants.STORY_WIDTH - scaled_width) / 2;
						int top = (Constants.STORY_HEIGHT - scaled_height) / 2;
						
						Videokit vk = new Videokit();
						String strRotation = vk.getMetaData(storyContent.contentPath, "rotate");
						if (strRotation == null)
							strRotation = "0";
						int rotateCount = 0;
						if (strRotation.equals("90"))
							rotateCount = 1;
						else if (strRotation.equals("180"))
							rotateCount = 2;
						else if (strRotation.equals("270"))
							rotateCount = 3;
						String strRotationFilter = "";
						for (int j = 0; j < rotateCount; j ++) {
							strRotationFilter += "transpose=1,";
						}
						
						String strFilterEffectFilter = "";
						if (storyContent.filter == 1) {
							strFilterEffectFilter = ",curves=vintage";
						} else if (storyContent.filter == 2) {
							strFilterEffectFilter = ",vignette=PI/4";
						} else if (storyContent.filter == 3) {
							strFilterEffectFilter = ",colorchannelmixer=.3:.4:.3:0:.3:.4:.3:0:.3:.4:.3";
						} else if (storyContent.filter == 4) {
							strFilterEffectFilter = ",colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131";
						}
						
						String strTextOverlayFilter = getOverlayTextFilter(storyContent.overlayString);
						
						strFilterGraph += "color=c=black:s=" + Constants.STORY_WIDTH + "x" +
								Constants.STORY_HEIGHT + ",trim=duration=" + duration + "[bg" + i + "];" +
								"[" + i + ":v]" + strRotationFilter + "scale=" + scaled_width + "x" +
								scaled_height + "[s" + i + "];" + "[bg" + i + "][s" + i + "]overlay=x=" +
								left + ":y=" + top + "[o" + i + "];" + "[o" + i + "]trim=duration=" +
								duration + strFilterEffectFilter + strTextOverlayFilter + "[v" + i + "0];" +
								"[v" + i + "0]setpts=PTS-STARTPTS+" + start_ts + "/TB[v" + i + "];";
						
						start_ts += duration - transduration;
					}
					
					strFilterGraph += "color=c=black:s=" + Constants.STORY_WIDTH + "x" + Constants.STORY_HEIGHT +
							",trim=duration=" + Constants.STORY_DURATION + "[over];";
					
					start_ts = 0.f;
					String strBackgroundStream = "[over]";
					for (int i = 0; i < clips.size(); i ++) {
						String strOverlayFilter = "";
						float base_timestamp = start_ts + transduration;
						float base_timestamp1 = start_ts + durations[i] - transduration;
						if (transition == 2) {
							// Transition is Wipe
							if (i > 0) {
								strOverlayFilter = "=y=0:x='if(gte(t," + base_timestamp + "),0,((" + base_timestamp + "-t)/" + transduration + ")*W)'";
							}
						} else {
							// Transition is Push
							if (i == 0) {
								strOverlayFilter = "=x=0:y='if(gte(t," + base_timestamp1 + "),(0-((t-" + base_timestamp1 + ")/" + transduration + "))*H,0)'";
							} else if (i == (clips.size() - 1)) {
								strOverlayFilter = "=x=0:y='if(gte(t," + base_timestamp + "),0,((" + base_timestamp + "-t)/" + transduration + ")*H)'";
							} else {
								strOverlayFilter = "=x=0:y='if(gte(t," + base_timestamp1 + "),(0-((t-" + base_timestamp1 + ")/" + transduration + "))*H," +
													"if(gte(t," + base_timestamp + "),0,((" + base_timestamp + "-t)/" + transduration + ")*H))'";
							}
						}
						
						String strOverlayResult = "[over" + i + "]";
						if (i == 0 && i == (clips.size() - 1)) {
							strFilterGraph += strBackgroundStream + "[v" + i + "]overlay=format=yuv420[out]";
						} else if (i == (clips.size() - 1)) {
							strFilterGraph += strBackgroundStream + "[v" + i + "]overlay" + strOverlayFilter + ":format=yuv420[out]";
						} else {
							strFilterGraph += strBackgroundStream + "[v" + i + "]overlay" + strOverlayFilter + strOverlayResult + ";";
						}
						strBackgroundStream = strOverlayResult;
						
						start_ts += durations[i] - transduration;
					}
					
				}
				
			}
			
			Log.d("StoryBuilder", strFilterGraph);
			return strFilterGraph;
		}
		
		private String getOverlayTextFilter(String overlayText) {
			String strAdjustedText = "";
			String strTemp = overlayText;
			for (int i = 0; i < Constants.OVERLAY_MAX_LINES; i ++) {
				if (strTemp.length() == 0) {
					break;
				} else if (strTemp.length() > Constants.OVERLAY_LINE_LENGTH) {
					if (i == 0)
						strAdjustedText += "\\ " + strTemp.substring(0, Constants.OVERLAY_LINE_LENGTH) + "\\ ";
					else
						strAdjustedText += "\n\\ " + strTemp.substring(0, Constants.OVERLAY_LINE_LENGTH) + "\\ ";
					strTemp = strTemp.substring(Constants.OVERLAY_LINE_LENGTH);
				} else {
					if (i == 0)
						strAdjustedText += "\\ " + strTemp + "\\ ";
					else
						strAdjustedText += "\n\\ " + strTemp + "\\ ";
					strTemp = "";
				}
			}
			
			String strResult = ",drawtext=fontfile=" + Constants.defaultFontPath +
					":x=W/2-tw/2:y=H-th-20:fontsize=" + Constants.OVERLAY_FONT_SIZE +
					":fontcolor=white:text='" + strAdjustedText + "':box=1:boxcolor=black@0.5";
			
			return strResult;
		}

	}

}
