package com.felixkroemer.iliasfilemanager.model.items;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import com.felixkroemer.iliasfilemanager.Constant.ITEM_TYPE;

public class FileItem extends Item {

	private static final Logger logger = LogManager.getLogger(FileItem.class);

	public FileItem(String name, String url, String phpsessid) {
		super(ITEM_TYPE.FILE, name, url, phpsessid);
	}

	public File downloadFile(String folder) {
		try {
			Response resp = Jsoup.connect(this.getURL()).cookie("PHPSESSID", this.getSessionID())
					.cookie("ilClientId", "ILIAS").header("DNT", "1").header("Upgrade-Insecure-Requests", "1")
					.maxBodySize(0).ignoreContentType(true).execute();

			String fileExtension = "";
			Matcher matcher = Pattern.compile("\\.\\w*").matcher(resp.header("Content-Disposition"));
			if (matcher.find()) {
				fileExtension = matcher.group(0);
			}
			Path p = Paths.get(folder, this.getName() + fileExtension);
			File targetFile = new File(p.toString());
			if (targetFile.exists()) {
				HashSet<String> existingFileNames = new HashSet<String>();
				for(File f : new File(folder).listFiles()) {
					existingFileNames.add(f.getName());
				}
				int version = 2;
				while(existingFileNames.contains(this.getName() + "_V" + version + fileExtension)) {
					version++;
				}
				p = Paths.get(folder, this.getName() + "_V" + version + fileExtension);
				targetFile = new File(p.toString());
				logger.info("Found updated version of file " + this.getName());
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
	
	public static void addRefID(File f, String id) {
		try {
			if (f.getPath().endsWith(".pdf")) {
				PDDocument pd = PDDocument.load(f);
				PDDocumentInformation info = new PDDocumentInformation();
				info.setCustomMetadataValue("refid", id);
				pd.setDocumentInformation(info);
				pd.save(f);
				pd.close();
			} else {
				Path p = Paths.get(f.getAbsolutePath());
				Files.setAttribute(p, "user:refid", id.getBytes());
			}
			logger.info("Attached ID " + id + " to File " + f.getName());
		} catch (IOException e) {
			logger.error("Could not attach refid " + id + " to File " + f.getName());
			logger.debug(e);
		}
	}

	public static String getRefID(File f) {
		try {
			if (f.getPath().endsWith(".pdf")) {
				PDDocument pd = PDDocument.load(f);
				PDDocumentInformation info = pd.getDocumentInformation();
				pd.close();
				return(info.getCustomMetadataValue("refid"));
			} else {
				Path p = Paths.get(f.getAbsolutePath());
				byte[] b = (byte[]) Files.getAttribute(p, "user:refid");
				return new String(b);
			}
		} catch (IOException e) {
			logger.debug(e);
		}
		return null;
	}
}
