package com.felixkroemer.iliasfilemanager.model;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
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

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Model {

	private static final Logger logger = LogManager.getLogger(Model.class);
	private Session session;
	private ObservableList<Subscription> subscriptions;
	private SimpleBooleanProperty loading;
	private SimpleBooleanProperty readyToSync;
	private SimpleStringProperty statusMessage;

	public Model() {
		this.subscriptions = FXCollections.observableArrayList();
		this.loading = new SimpleBooleanProperty();
		this.readyToSync = new SimpleBooleanProperty();
		this.statusMessage = new SimpleStringProperty();
	}

	private Document parseConfig() {
		logger.info("parsing config file");
		File f = new File(Settings.getConfig(Settings.Config.CONFIG_FILE));
		if (!f.exists()) {
			logger.fatal("Config file " + f.getAbsolutePath() + " does not exist");
			System.exit(0);
		}
		Document doc = null;
		try {
			doc = new SAXBuilder().build(Settings.getConfig(Settings.Config.CONFIG_FILE));
		} catch (JDOMException | IOException e) {
			logger.fatal("Config could not be parsed");
			logger.debug(e);
			System.exit(0);
		}
		return doc;
	}

	public void initSubscriptions() {
		Document config = this.parseConfig();
		for (Element courseElement : config.getRootElement().getChildren()) {
			String title = courseElement.getChildText("title");
			for (Element subfolderElement : courseElement.getChildren("subfolder")) {
				this.subscriptions.add(new Subscription(title, subfolderElement.getText(),
						subfolderElement.getAttributeValue("path")));
			}
		}
	}

	public boolean initSession() {
		this.session = new Session();
		if (!this.session.isInitiated()) {
			this.statusMessage.set("Wrong login credentials");
		}
		return this.session.isInitiated();
	}

	public void findSubscriptionFolders() {
		for (Subscription sub : this.subscriptions) {
			boolean found = false;
			for (Folder course : this.session.getCourses()) {
				if (course.getName().equalsIgnoreCase(sub.getTitle())) {
					found = true;
					logger.info("Trying to find subfolder " + sub.getSubfolder() + " in Course " + course.getName());
					Folder folder = course.findSubfolder(sub.getSubfolder());
					if (folder != null) {
						sub.setFolder(folder);
						sub.validateSub();
						logger.info(
								"Found match for subfolder " + sub.getSubfolder() + " --> \"" + sub.getPath() + "\"");
					} else {
						logger.info("Subfolder " + sub.getPath() + " not found.");
						sub.setStatus("Folder not found");
					}
				}
			}
			if (!found) {
				sub.setStatus("Course not found");
			}
		}
	}

	public void detectAllSycedFiles() {
		for (Subscription s : this.getValidSubscriptions()) {
			this.detectSyncedFiles(s.getFolder(), s.getPath());
		}
		this.readyToSync.set(true);
	}

	public void detectSyncedFiles(Folder folder, String path) {
		try {
			File directory = new File(path);
			if (directory.exists()) {
				if (!directory.isDirectory()) {
					throw new IOException(path + " is not a directory.");
				}
				if (!directory.canRead() || !directory.canWrite()) {
					throw new IOException(path + " is not accessible.");
				}
				for (File file : directory.listFiles()) {
					if (!file.isFile()) {
						continue;
					}
					if (!file.canRead()) {
						logger.error("File " + file.getName() + "could not be read");
						continue;
					}
					String ref;
					if ((ref = FileItem.getRefID(file)) != null) {
						FileItem f;
						if ((f = folder.getChildByID(Integer.parseInt(ref))) != null) {
							f.setSynced(true);
						}
					} else {
						for (FileItem item : folder.getFiles()) {
							String fileWithoutExtension = FilenameUtils.removeExtension(file.getName());
							if (item.getName().equalsIgnoreCase(fileWithoutExtension)) {
								logger.info("Detected file " + file.getName());
								FileItem.addRefID(file, "" + item.getID());
								item.setSynced(true);
							}
						}
					}
				}
			} else {
				directory.mkdir();
				logger.info("Created folder " + directory.getAbsolutePath());
			}
		} catch (IOException e) {
			logger.error("Could not check existing files in directory " + path + "\n" + e.getMessage());
			logger.debug(e);
			e.printStackTrace();
		}
		for (Folder f : folder.getSubfolders()) {
			this.detectSyncedFiles(f, path + Settings.getSeparator() + f.getName());
		}
	}

	public void syncAll() {
		if (!this.readyToSync.get()) {
			return;
		}
		for (Subscription s : this.getValidSubscriptions()) {
			logger.info("Handling subscription: " + s.getFolder().getName() + " (" + s.getPath() + ")");
			this.downloadMissingFiles(s.getFolder(), s.getPath());
		}
	}

	public void downloadMissingFiles(Folder folder, String path) {
		Map<FileItem, File> downloadedItems = folder.downloadMissingItems(path);
		for (Map.Entry<FileItem, File> e : downloadedItems.entrySet()) {
			FileItem.addRefID(e.getValue(), "" + e.getKey().getID());
			e.getKey().setSynced(true);
		}
		for (Folder subfolder : folder.getSubfolders()) {
			this.downloadMissingFiles(subfolder, path + Settings.getSeparator() + subfolder.getName());
		}
	}

	public ObservableList<Subscription> getSubscriptions() {
		return this.subscriptions;
	}

	public SimpleBooleanProperty getLoadingProperty() {
		return this.loading;
	}

	public SimpleBooleanProperty getReadyToSyncProperty() {
		return this.readyToSync;
	}

	public SimpleStringProperty getStatusMessageProperty() {
		return this.statusMessage;
	}

	public Set<Subscription> getValidSubscriptions() {
		HashSet<Subscription> set = new HashSet<Subscription>();
		for (Subscription s : this.subscriptions) {
			if (s.getSubValid()) {
				set.add(s);
			}
		}
		return set;
	}
}