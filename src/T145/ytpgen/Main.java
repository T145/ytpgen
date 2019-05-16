package T145.ytpgen;

import java.io.File;
import java.time.LocalTime;

import T145.ytpgen.core.Commands;
import T145.ytpgen.core.Tools;

public class Main {

	public static void main(String[] args) {
		LocalTime start = LocalTime.of(0, 3);
		LocalTime end = LocalTime.of(0, 5);
		File video = Tools.getFile("shave.mp4");

		//System.out.println(start);
		//System.out.println(end);
		System.out.println(Commands.getVideoLength(video));
		//System.out.println(Commands.createVideoSnippet(video, start, end));
		Commands.joinVideoSnippets("helloworld");
	}
}
