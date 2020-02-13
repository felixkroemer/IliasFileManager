package com.felixkroemer.iliasfilemanager.model;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.felixkroemer.iliasfilemanager.Constant;
import com.felixkroemer.iliasfilemanager.Settings;
import com.felixkroemer.iliasfilemanager.model.items.FileItem;
import com.felixkroemer.iliasfilemanager.model.items.Folder;
import com.felixkroemer.iliasfilemanager.model.items.Item;

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

	public void initSubscriptions() {
		Set<Subscription> subs = Settings.parseConfigFile();
		for (Subscription sub : subs) {
			this.subscriptions.add(sub);
		}
	}

	public void addSubscription(Subscription sub) {
		this.loading.set(true);
		sub.setStatus("Initializing");
		this.subscriptions.add(sub);
		this.detectSyncedFiles(sub);
		this.loading.set(false);
	}

	public boolean initSession() {
		if (this.session != null && this.session.isInitiated()) {
			return true;
		}
		this.session = new Session();
		if (!this.session.isInitiated()) {
			this.statusMessage.set(Constant.WRONG_CREDS);
		} else {
			this.statusMessage.set("Logged in successfully");
		}
		return this.session.isInitiated();
	}

	public boolean getSessionInitiated() {
		if (this.session != null) {
			return this.session.isInitiated();
		} else {
			return false;
		}
	}

	public void findSubscriptionFolders() {
		for (Subscription sub : this.subscriptions) {
			sub.setStatus("Initializing");
			boolean found = false;
			for (Folder course : this.session.getCourses()) {
				if (course.getName().equalsIgnoreCase(sub.getTitle())) {
					found = true;
					logger.info("Trying to find subfolder " + sub.getSubfolder() + " in Course " + course.getName());
					Folder folder = course.findSubfolder(sub.getSubfolder());
					if (folder != null) {
						sub.setFolder(folder);
						sub.validateSub();
						sub.setStatus("Folder found");
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

	public void detectAllSyncedFiles() {
		for (Subscription s : this.getValidSubscriptions()) {
			this.detectSyncedFiles(s);
		}
		this.readyToSync.set(true);
	}

	public void detectSyncedFiles(Subscription s) {
		s.setStatus("Discovering Files");
		this.detectSyncedFiles(s.getFolder(), s.getPath());
		if (testAllSynced(s.getFolder())) {
			s.setSynced(true);
			s.setStatus("Synced");
		} else {
			s.setStatus("Not Synced");
		}
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
			if (s.getSynced().get()) {
				continue;
			}
			logger.info("Handling subscription: " + s.getFolder().getName() + " (" + s.getPath() + ")");
			s.setStatus("Syncing");
			this.downloadMissingFiles(s.getFolder(), s.getPath());
			if (testAllSynced(s.getFolder())) {
				s.setSynced(true);
				s.setStatus("Synced");
			}
		}
	}

	public boolean testAllSynced(Folder f) {
		for (Item i : f.getChildren()) {
			switch (i.getType()) {
			case FILE:
				if (!i.getSynced().get()) {
					return false;
				}
				break;
			case FOLDER:
				if (testAllSynced((Folder) i)) {
					((Folder) i).setSynced(true);
				} else {
					return false;
				}
				break;
			}
		}
		return true;
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

	public Set<Folder> getCourses() {
		if (this.session.isInitiated()) {
			return this.session.getCourses();
		} else {
			return new HashSet<Folder>();
		}
	}
}