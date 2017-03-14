package com.zimmerbell.photo_utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class Main {
	private final static String OPT_HELP = "help";
	public final static String OPT_SOURCE = "src";
	public final static String OPT_DESTINATION = "dest";

	public final static String OPT_OVERWRITE = "overwrite";
	public final static String OPT_DELETE = "delete";

	public final static String OPT_RENAME = "rename";
	public final static String OPT_STAMP = "stamp";
	public final static String OPT_RESIZE = "resize";
	public final static List<String> CHANGE_OPTIONS = Collections.unmodifiableList(Arrays.asList(new String[] { OPT_STAMP, OPT_RESIZE }));

	public static void main(String[] args) throws Throwable {
		Options options = new Options();

		Option optionSrc, optionDest;

		options.addOption(Option.builder("h").longOpt(OPT_HELP).build());
		options.addOption(optionSrc = Option.builder("s").longOpt(OPT_SOURCE).argName("PATH").hasArg().build());
		options.addOption(optionDest = Option.builder("d").longOpt(OPT_DESTINATION).argName("PATH").hasArg().build());

		options.addOption(Option.builder("o").longOpt(OPT_OVERWRITE).desc("overwrite existing files").build());
		options.addOption(Option.builder().longOpt(OPT_DELETE).desc("delete extraneous files from dest dirs").build());

		options.addOption(Option.builder().longOpt(OPT_RENAME).desc("rename files according to their capture date").build());
		options.addOption(Option.builder().longOpt(OPT_STAMP).desc("write capture date in image").build());
		options.addOption(Option.builder().longOpt(OPT_RESIZE).argName("SIZE").hasArg().optionalArg(true).desc("resize image (default: " + PhotoProcessor.RESIZE_DEFAULT + ")").build());

		CommandLine cl = new DefaultParser().parse(options, args);
		if (cl.hasOption(OPT_HELP) || args.length == 0) {
			String footer = "\nEXAMPLES\n" //
					+ "copy all photos from dir1 to dir2 and stamp them with their capture date\n" //
					+ "    photo-utils -s dir1 -d dir2 --stamp\n";
			new HelpFormatter().printHelp("photo-utils --" + optionSrc.getLongOpt() + " " + optionSrc.getArgName() + " --" + optionDest.getLongOpt() + " " + optionDest.getArgName() + " [OPTIONS]", "", options, footer);
			return;
		}

		new PhotoProcessor(cl);
	}
}
