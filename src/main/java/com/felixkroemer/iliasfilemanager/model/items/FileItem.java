package com.felixkroemer.iliasfilemanager.model.items;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection.Response;

import com.felixkroemer.iliasfilemanager.Constant.ITEM_TYPE;

import org.jsoup.Jsoup;

public class FileItem extends Item {

	private static final Logger logger = LogManager.getLogger(FileItem.class);

	public FileItem(String name, String url, String phpsessid) {
		super(ITEM_TYPE.FILE, name, url, phpsessid);
	}

	public File downloadFile(String path) {
		try {
			Response resp = Jsoup.connect(this.getURL()).cookie("PHPSESSID", this.getSessionID())
					.cookie("ilClientId", "ILIAS").header("DNT", "1").header("Upgrade-Insecure-Requests", "1")
					.ignoreContentType(true).execute();

			String fileExtension = "";
			Matcher matcher = Pattern.compile("\\.\\w*").matcher(resp.header("Content-Disposition"));
			if (matcher.find()) {
				fileExtension = matcher.group(0);
			}

			File targetFile = new File(path + fileExtension);
			if (targetFile.exists()) {
				throw new IOException("File already exists.");
			}

			try (FileOutputStream fos = new FileOutputStream(targetFile)) {
				fos.write(resp.bodyAsBytes());
			}
			return targetFile;

		} catch (IOException e) {
			logger.error("File " + this.getName() + " with id " + this.getID() + " could not be downloaded.\n"
					+ e.getMessage());
			logger.debug(e);
			return null;
		}
	}
}
