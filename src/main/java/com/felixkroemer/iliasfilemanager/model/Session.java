package com.felixkroemer.iliasfilemanager.model;

import java.io.IOException;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import com.felixkroemer.iliasfilemanager.Constant;
import com.felixkroemer.iliasfilemanager.model.items.Folder;
import com.felixkroemer.iliasfilemanager.Settings;

public class Session {

	private static final Logger logger = LogManager.getLogger(Session.class);
	private String user;
	private String password;
	private String phpsessid;
	private Folder root;

	public Session() {
		this.user = Settings.getConfig(Settings.Config.USERNAME);
		this.password = Settings.getConfig(Settings.Config.PASSWORD);
		try {
			this.initSessionId(user, password);
			this.root = new Folder("Root", Constant.MAINPAGE, this.phpsessid);
		} catch (IOException e) {
			logger.fatal("Session could not be created: " + e.getMessage());
			logger.debug(e);
			System.exit(0);
		}
	}

	private void initSessionId(String user, String password) throws IOException {
		logger.info("Logging in");

		Response resp1 = Jsoup
				.connect("https://cas.uni-mannheim.de/cas/login?service=https%3A%2F%2Filias.uni-mannheim.de%2F")
				.method(Method.GET).execute();
		String jsessionid = resp1.cookie("JSESSIONID");

		String ident = "value=\"LT-";
		int start = resp1.body().indexOf("value=\"LT-") + ident.length() - 3;
		String lt = resp1.body().substring(start, resp1.body().indexOf('"', start + 1));

		Response resp2 = Jsoup
				.connect("https://cas.uni-mannheim.de/cas/login;jsessionid=" + jsessionid
						+ "?service=https%3A%2F%2Filias.uni-mannheim.de%2F")
				.cookies(resp1.cookies()).data("username", user)
				.header("Referer", "https://cas.uni-mannheim.de/cas/login").data("password", password)
				.data("_eventId", "submit").data("execution", "e1s1").header("DNT", "1").data("submit", "Anmelden")
				.data("lt", lt).followRedirects(false).userAgent(Constant.USER_AGENT).method(Method.POST).execute();

		if (!resp2.hasCookie("CASTGC")) {
			logger.fatal("Wrong login credentials");
			System.exit(0);
		}

		ident = "ticket=";
		start = resp2.header("Location").indexOf(ident) + ident.length();
		this.phpsessid = resp2.header("Location").substring(start);

		Jsoup.connect(resp2.header("Location")).cookies(resp2.cookies()).method(Method.GET).followRedirects(false)
				.execute();

		logger.info("Logged in succesfully");
	}

	public Set<Folder> getCourses() {
		return this.root.getSubfolders();
	}
}
