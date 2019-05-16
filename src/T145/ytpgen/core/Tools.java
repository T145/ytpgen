package T145.ytpgen.core;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

public class Tools {

	private Tools() {}

	public static boolean isNullOrEmpty(String s) {
		return s == null || StringUtils.isEmpty(s);
	}

	public static boolean isNullOrBlank(String s) {
		return s == null || StringUtils.isBlank(s);
	}

	// using multiple variables for debug purposes
	public static File getFile(String filePath) {
		ClassLoader loader = Tools.class.getClassLoader();
		URL url = loader.getResource(filePath);
		String path = url.getFile();
		return new File(path);
	}

	public static String readFile(File file) {
		StringBuilder builder = new StringBuilder();

		try (Stream<String> stream = Files.lines(file.toPath())) {
			for (String line : stream.collect(Collectors.toList())) {
				builder.append(line.trim());
			}
		} catch (IOException err) {
			err.printStackTrace();
		}

		// if something goes wrong an empty string will return
		return builder.toString();
	}
}
