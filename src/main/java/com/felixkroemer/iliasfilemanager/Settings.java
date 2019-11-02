package com.felixkroemer.iliasfilemanager;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Settings {

	private static final Logger logger = LogManager.getLogger(Settings.class);
	private String[] args;
	private static final String user = "-user";
	private static final String configFile = "-config";
	private Map<String, String> configs;

	public Settings(String[] args) {
		this.args = args;
		this.configs = new HashMap<String, String>();
		configs.put("configFile", "config.xml");
		this.parse();
	}

	public void parse(){

		for (int i = 0; i < this.args.length; i++) {

			if (args[i] == null) {
				continue;
			}

			if (args[i].toLowerCase().startsWith(user)) {
				configs.put("user", getParam(user, i));
			} else if (args[i].toLowerCase().startsWith(configFile)) {
				configs.put("configFile", getParam(configFile, i));
			} else {
				logger.fatal("parameter " + args[i] + " unknown.");
				System.exit(0);
			}
		}
	}

	private String getParam(String s, int i){
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

	public String getConfig() {
		return this.configs.get("configFile");
	}

	public String getSeparator() {
		return System.getProperty("file.separator");
	}

	public String getUser() {
		return this.configs.get("user");
	}

	public String getPassword() {
		return this.configs.get("password");
	}

	public void setUser(String user) {
		this.configs.put("user", user);
	}

	public void setPassword(String password) {
		this.configs.put("password", password);
	}
}
