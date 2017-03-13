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

	public final static String OPT_RENAME = "rename";
	public final static String OPT_STAMP = "stamp";
	public final static String OPT_RESIZE = "resize";
	public final static List<String> CHANGE_OPTIONS = Collections.unmodifiableList(Arrays.asList(new String[] { OPT_STAMP, OPT_RESIZE }));

	public static void main(String[] args) throws Throwable {
		Options options = new Options();

		options.addOption(Option.builder("h").longOpt(OPT_HELP).build());
		options.addOption(Option.builder("s").longOpt(OPT_SOURCE).argName("path").hasArg().build());
		options.addOption(Option.builder("d").longOpt(OPT_DESTINATION).argName("path").hasArg().build());

		options.addOption(Option.builder("o").longOpt(OPT_OVERWRITE).build());

		options.addOption(Option.builder().longOpt(OPT_RENAME).desc("rename files according to their capture date").build());
		options.addOption(Option.builder().longOpt(OPT_STAMP).desc("print capture date in image").build());
		options.addOption(Option.builder().longOpt(OPT_RESIZE).desc("resize image").build());

		CommandLine cl = new DefaultParser().parse(options, args);
		if (cl.hasOption(OPT_HELP)) {
			new HelpFormatter().printHelp("photo-utils", options);
			return;
		}

		new PhotoProcessor(cl);
	}
}
