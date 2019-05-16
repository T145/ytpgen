package T145.ytpgen.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

public class Commands {

	public static final String MPEG = "ffmpeg";
	public static final String PROBE = "ffprobe";
	public static final String MAGICK = "magick";

	public static final String KEY_PATH_TRANSITIONS = "path_transitions";
	public static final String KEY_PATH_SOUNDS = "path_sounds";
	public static final String KEY_PATH_MUSIC = "path_music";
	public static final String KEY_PATH_TEMP = "path_temp";
	public static final String KEY_VIDEO_GEN_ARGS = "video_gen_args";

	private static String path_transitions = StringUtils.EMPTY;
	private static String path_sounds = StringUtils.EMPTY;
	private static String path_music = StringUtils.EMPTY;
	private static String path_temp = StringUtils.EMPTY;
	private static String video_gen_args = StringUtils.EMPTY;

	private static final Runtime env = Runtime.getRuntime();
	private static final DefaultExecutor executor = new DefaultExecutor();

	private Commands() {}

	public static String getTransitionsPath() {
		return path_transitions;
	}

	public static String getSoundsPath() {
		return path_sounds;
	}

	public static String getMusicPath() {
		return path_music;
	}

	public static String getTempPath() {
		return path_temp;
	}

	static {
		File cfg = Tools.getFile("config.json");
		JSONObject json = new JSONObject(Tools.readFile(cfg));

		if (path_transitions.isEmpty()) {
			path_transitions = json.getString(KEY_PATH_TRANSITIONS);
		}

		if (path_sounds.isEmpty()) {
			path_sounds = json.getString(KEY_PATH_SOUNDS);
		}

		if (path_music.isEmpty()) {
			path_music = json.getString(KEY_PATH_MUSIC);
		}

		if (path_temp.isEmpty()) {
			path_temp = String.format("%s/job_%s", json.getString(KEY_PATH_TEMP), System.currentTimeMillis());
			new File(path_temp).mkdirs();
		}

		if (video_gen_args.isEmpty()) {
			video_gen_args = json.getString(KEY_VIDEO_GEN_ARGS);
		}
	}

	static BufferedReader getStreamReader(InputStream stream) {
		return new BufferedReader(new InputStreamReader(stream));
	}

	/**
	 * Return the length of a video (in seconds)
	 *
	 * @param video input video filename to work with
	 * @return Video length as a string (output from ffprobe)
	 */
	public static double getVideoLength(File video) {
		String cmd = String.format("%s -i %s -show_entries format=duration -v quiet", PROBE, video.getAbsolutePath());

		try {
			Process exec = env.exec(cmd);
			BufferedReader stdErr = getStreamReader(exec.getErrorStream());

			if (!Tools.isNullOrBlank(stdErr.readLine())) {
				throw new IOException("Caught unhandled IOException!");
			}

			BufferedReader stdIn = getStreamReader(exec.getInputStream());
			stdIn.readLine(); // = [FORMAT]
			return Double.parseDouble(stdIn.readLine().replace("duration=", StringUtils.EMPTY));
		} catch (IOException err) {
			err.printStackTrace();
		}
		return 0.0D;
	}

	static String getFormattedLocalTime(LocalTime time) {
		return String.format("%s:%s:%s", time.getHour(), time.getMinute(), time.getSecond());
	}

	static String getTempVideoPath() {
		return String.format("%s/snippet_%s", path_temp, System.currentTimeMillis());
	}

	private static int editVideo(File video, String cmd) {
		try {
			return executor.execute(CommandLine.parse(cmd));
		} catch (IOException err) {
			err.printStackTrace();
		}
		return 1;
	}

	public static int createVideoSnippet(File video, LocalTime start, LocalTime end) {
		String cmd = String.format("%s -i %s -ss %s -to %s %s -y %s.mp4", MPEG, video.getAbsolutePath(), getFormattedLocalTime(start), getFormattedLocalTime(end), video_gen_args, getTempVideoPath());
		return editVideo(video, cmd);
	}

	public static int copyVideo(File video) {
		String cmd = String.format("%s -i %s %s -y %s.mp4", MPEG, video.getAbsolutePath(), video_gen_args, getTempVideoPath());
		return editVideo(video, cmd);
	}

	public static void joinVideoSnippets(String outputPath) {
		StringBuilder cmd = new StringBuilder();

		try {
			List<Path> videoPaths = Files.list(new File(path_temp).toPath()).filter(Files::isRegularFile).collect(Collectors.toList());

			for (short i = 0; i < videoPaths.size(); ++i) {
				Path videoPath = videoPaths.get(i);

				if (videoPath.endsWith(".mp4")) {
					cmd.append(String.format("-i %s", videoPath.getFileName()));
				} else {
					// avoids ConcurrentModificationException
					// added benefit of O(1) removal
					videoPaths.remove(i);
				}
			}

			cmd.append(" -filter-complex \"");

			for (Path _ : videoPaths) {
				cmd.append(String.format("[%s:v:0][%s:a:0]", videoPaths.size()));
			}

			cmd.append(String.format("concat=n=%s:v=1:a=1[outv][outa]\" -map \"[outv]\" -map \"[outa]\" -y ", videoPaths.size()));
			System.out.println(cmd.toString());
			
			// then just run it, but for now we're testing
		} catch (IOException err) {
			err.printStackTrace();
		}
	}
}
