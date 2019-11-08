package com.felixkroemer.iliasfilemanager.model.items;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.felixkroemer.iliasfilemanager.Constant;
import com.felixkroemer.iliasfilemanager.Constant.ITEM_TYPE;

public class Folder extends Item {

	private static final Logger logger = LogManager.getLogger(Folder.class);
	private Set<Item> children;

	public Folder(String name, String url, String phpsessid) {
		super(ITEM_TYPE.FOLDER, name, url, phpsessid);
	}

	public Set<Item> getChildren() {
		try {
			if (this.children == null) {
				this.init();
			}
		} catch (IOException e) {
			logger.error("Item " + this.getName() + " could not be initialized");
			logger.debug(e);
		}
		return this.children;
	}

	public Set<Folder> getSubfolders() {
		Set<Folder> folders = new HashSet<Folder>();
		for (Item i : this.getChildren()) {
			if (i.getType() == ITEM_TYPE.FOLDER) {
				folders.add((Folder) i);
			}
		}
		return folders;
	}

	public Folder findSubfolder(String s) {
		for (Folder f : this.getSubfolders()) {
			if (f.getName().equalsIgnoreCase(s)) {
				return f;
			}
		}
		for (Folder f : this.getSubfolders()) {
			Folder res = f.findSubfolder(s);
			if (res != null) {
				return res;
			}
		}
		return null;
	}

	public Map<FileItem, File> downloadMissingItems(Set<Integer> existingFiles, String folder) {
		HashMap<FileItem, File> downloadedFiles = new HashMap<FileItem, File>();
		for (FileItem item : this.getFiles()) {
			if (!existingFiles.contains(item.getID())) {
				File targetFile;
				if ((targetFile = item.downloadFile(folder)) != null) {
					downloadedFiles.put(item, targetFile);
				}
			}
		}
		if (!downloadedFiles.isEmpty()) {
			String msg = "Downloaded missing files in folder " + this.getName() + ":\n";
			for (Item i : downloadedFiles.keySet()) {
				msg += "- " + i.getName() + " (" + i.getID() + ")\n";
			}
			msg = msg.substring(0, msg.length() - 1);
			logger.info(msg);
		}
		return downloadedFiles;
	}

	public void init() throws IOException {
		this.children = new HashSet<Item>();

		Response resp = Jsoup.connect(this.getURL()).cookie("PHPSESSID", this.getSessionID())
				.cookie("ilClientId", "ILIAS").header("DNT", "1").header("Upgrade-Insecure-Requests", "1")
				.userAgent(Constant.USER_AGENT).execute();

		logger.info("Downloaded: " + this.getName() + " (" + this.getID() + ")");

		Document doc = Jsoup.parse(resp.body());
		Elements all = doc.getElementsByClass("il_ContainerItemTitle");
		for (Element e : all) {
			if (e.tagName().equals("a")) {
				Item i = null;
				String url = e.attr("href");
				if (!url.toLowerCase().startsWith(Constant.LINK_PREFIX)) {
					url = Constant.LINK_PREFIX + url;
				}

				ITEM_TYPE type = Item.getType(url);
				if (type == null) {
					continue;
				}
				switch (type) {
				case FOLDER:
					i = new Folder(e.text(), url, this.getSessionID());
					break;
				case FILE:
					i = new FileItem(this.replaceUmlaute(e.text()), url, this.getSessionID());
					break;
				}
				if (i != null) {
					this.children.add(i);
				}
			}
		}
		String msg = "Folder " + this.getName() + " has children:\n";
		for (Item i : this.getChildren()) {
			msg += "- " + i.getName() + " (" + i.getType() + ")\n";
		}
		msg = msg.substring(0, msg.length() - 1);
		logger.info(msg);
	}

	public Set<FileItem> getFiles() {
		Set<FileItem> files = new HashSet<FileItem>();
		for (Item i : this.getChildren()) {
			if (i.getType() == ITEM_TYPE.FILE) {
				files.add((FileItem) i);
			}
		}
		return files;
	}

	public String replaceUmlaute(String s) {
		return s.replace("\u00E4", "ae").replace("\u00F6", "oe").replace("\u00FC", "ue").replace("\u00C4", "Ae")
				.replace("\u00D6", "Oe").replace("\u00DC", "Ue").replace("\u00DF", "ss");
	}

	@Override
	public void setSessionID(String phpsessid) {
		super.setSessionID(phpsessid);
		for (Item i : this.children) {
			i.setSessionID(phpsessid);
		}
	}
}
