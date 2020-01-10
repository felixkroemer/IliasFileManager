package com.felixkroemer.iliasfilemanager;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Settings {

	private static final Logger logger = LogManager.getLogger(Settings.class);
	private static String[] args;
	private static final String userFlag = "-user";
	private static final String configFileFlag = "-config";
	private static Map<Config, String> configs;
	public static enum Config {USERNAME, PASSWORD, CONFIG_FILE};

	public static void init(String[] arguments) {
		args = arguments;
		configs = new HashMap<Config, String>();
		configs.put(Config.CONFIG_FILE, "config.xml");
		parse();
	}

	public static void parse() {

		for (int i = 0; i < args.length; i++) {

			if (args[i] == null) {
				continue;
			}

			if (args[i].toLowerCase().startsWith(userFlag)) {
				configs.put(Config.USERNAME, getParam(userFlag, i));
			} else if (args[i].toLowerCase().startsWith(configFileFlag)) {
				configs.put(Config.CONFIG_FILE, getParam(configFileFlag, i));
			} else {
				logger.fatal("parameter " + args[i] + " unknown.");
				System.exit(0);
			}
		}
	}

	private static String getParam(String s, int i) {
		String param;
		if (args[i].startsWith(s + "=")) {
			param = args[i].substring(args[i].indexOf("=") + 1);
			args[i] = null;
			return param;
		} else {
			try {
				param = args[i + 1];
				args[i] = null;
				args[i + 1] = null;
				return param;
			} catch (ArrayIndexOutOfBoundsException e) {
				logger.fatal("No value for parameter " + args[i] + " given.");
				System.exit(0);
			}
		}
		return "";
	}

	public static void setConfig(Config c, String s) {
		configs.put(c, s);
	}

	public static String getConfig(Config c) {
		return configs.get(c);
	}

	public static String getSeparator() {
		return System.getProperty("file.separator");
	}
}
