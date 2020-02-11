package com.felixkroemer.iliasfilemanager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.felixkroemer.iliasfilemanager.model.Subscription;

public final class Settings {

	private static final Logger logger = LogManager.getLogger(Settings.class);
	private static String[] args;
	private static final String userFlag = "-user";
	private static final String configFileFlag = "-config";
	private static Map<Config, String> configs;

	public static enum Config {
		USERNAME, PASSWORD, CONFIG_FILE
	};

	public static void init(String[] arguments) {
		args = arguments;
		configs = new HashMap<Config, String>();
		configs.put(Config.CONFIG_FILE, "config.xml");
		parseArgs();
	}

	public static Set<Subscription> parseConfigFile() {
		logger.info("parsing config file");
		Document doc = null;
		HashSet<Subscription> subs = new HashSet<Subscription>();
		try {
			String conf = Settings.getConfig(Settings.Config.CONFIG_FILE);
			File f = new File(conf);
			if (!f.exists() || !f.canRead()) {
				return subs;
			}
			doc = new SAXBuilder().build(Settings.getConfig(Settings.Config.CONFIG_FILE));
		} catch (JDOMException | IOException e) {
			logger.fatal("Config could not be parsed");
			logger.debug(e);
			System.exit(0);
		}
		for (Element courseElement : doc.getRootElement().getChildren()) {
			String title = courseElement.getChildText("title");
			for (Element subfolderElement : courseElement.getChildren("subfolder")) {
				subs.add(new Subscription(title, subfolderElement.getText(),
						subfolderElement.getAttributeValue("path")));
			}
		}
		return subs;
	}

	private static void parseArgs() {
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
