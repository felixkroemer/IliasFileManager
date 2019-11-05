package com.felixkroemer.iliasfilemanager.model;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.felixkroemer.iliasfilemanager.Settings;
import com.felixkroemer.iliasfilemanager.model.items.FileItem;
import com.felixkroemer.iliasfilemanager.model.items.Folder;
import com.felixkroemer.iliasfilemanager.model.items.Item;

public class Model {

	private static final Logger logger = LogManager.getLogger(Model.class);
	private Session session;
	private Settings settings;
	private Map<Folder, String> subscriptions;

	public Model(Settings s) {
		this.settings = s;
		this.session = new Session(settings.getUser(), settings.getPassword());
		this.subscriptions = this.initSubscriptions();
	}

	private Document parseConfig() {
		File f = new File(this.settings.getConfig());
		if (!f.exists()) {
			logger.fatal("Config file " + f.getAbsolutePath() + " does not exist");
			System.exit(0);
		}
		Document doc = null;
		try {
			doc = new SAXBuilder().build(this.settings.getConfig());
		} catch (JDOMException | IOException e) {
			logger.fatal("Config could not be parsed");
			logger.debug(e);
			System.exit(0);
		}
		return doc;
	}

	private Map<Folder, String> initSubscriptions() {
		logger.info("parsing config file");
		Map<Folder, String> subs = new HashMap<Folder, String>();
		Document config = this.parseConfig();
		for (Element child : config.getRootElement().getChildren()) {
			String title = child.getChildText("title");
			for (Folder course : this.session.getCourses()) {
				if (course.getName().equalsIgnoreCase(title)) {
					for (Element subfolder : child.getChildren("subfolder")) {
						logger.info(
								"Trying to find subfolder " + subfolder.getText() + " in Course " + course.getName());
						Folder f = course.findSubfolder(subfolder.getText());
						if (f != null) {
							subs.put(f, subfolder.getAttributeValue("path"));
							logger.info("Found match for subfolder " + subfolder.getText() + " --> \""
									+ subfolder.getAttributeValue("path") + "\"");
						} else {
							logger.info("Subfolder " + subfolder.getText() + " not found.");
						}
					}
				}
			}
		}
		return subs;
	}

	public void start() {
		for (Entry<Folder, String> e : this.subscriptions.entrySet()) {
			logger.info("Handling subscription: " + e.getKey().getName() + " (" + e.getValue() + ")");
			this.handleSubscription(e.getKey(), e.getValue());
		}
	}

	public void handleSubscription(Folder folder, String path) {
		this.handleFolder(folder, path);
		for (Folder subfolder : folder.getSubfolders()) {
			this.handleSubscription(subfolder, path + this.settings.getSeparator() + subfolder.getName());
		}
	}

	public void handleFolder(Folder folder, String path) {
		Set<Integer> existingFileRefs = this.getFilesPresent(path, folder.getChildren());
		if (existingFileRefs == null) {
			return;
		} else {
			Map<FileItem, File> downloadedItems = folder.downloadMissingItems(existingFileRefs, path);
			for (Entry<FileItem, File> d : downloadedItems.entrySet()) {
				FileItem downloadedFile = d.getKey();
				FileItem.addRefID(d.getValue(), "" + downloadedFile.getID());
			}
		}
	}

	public Set<Integer> getFilesPresent(String path, Set<Item> items) {
		Set<Integer> refs = new HashSet<Integer>();
		try {
			File directory = new File(path);
			if (!directory.canRead() || !directory.canWrite()) {
				throw new IOException(path + " is not accessible.");
			}
			if (directory.exists()) {
				if (!directory.isDirectory()) {
					throw new IOException(path + " is not a directory.");
				}
				for (File s : directory.listFiles()) {
					if (!s.isFile()) {
						continue;
					}
					if (!s.canRead()) {
						logger.error("File " + s.getName() + "could not be read");
						continue;
					}
					String ref;
					if ((ref = FileItem.getRefID(s)) != null) {
						refs.add(Integer.valueOf(ref));
					} else {
						for (Item i : items) {
							String fileWithoutExtension = FilenameUtils.removeExtension(s.getName());
							if (i.getName().equalsIgnoreCase(fileWithoutExtension)) {
								logger.info("Detected file " + s.getName());
								FileItem.addRefID(s, "" + i.getID());
								refs.add(Integer.valueOf(i.getID()));
							}
						}
					}
				}
			} else {
				directory.mkdir();
				logger.info("Created folder " + directory.getAbsolutePath());
				return null;
			}
		} catch (IOException e) {
			logger.error("Could not check existing files in directory " + path + "\n" + e.getMessage());
			logger.debug(e);
			return null;
		}
		return refs;
	}
}