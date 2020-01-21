package com.felixkroemer.iliasfilemanager.model;

import com.felixkroemer.iliasfilemanager.model.items.Folder;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

public class Subscription implements Displayable {
	private String title;
	private String subfolder;
	private String path;
	private Folder folder;
	private SimpleStringProperty status;
	private SimpleBooleanProperty synced;
	private boolean isValid;

	public Subscription(String title, String subfolder, String path) {
		this.title = title;
		this.subfolder = subfolder;
		this.path = path;
		this.status = new SimpleStringProperty();
		this.status.set("");
		this.synced = new SimpleBooleanProperty();
		this.isValid = false;
	}

	public void setFolder(Folder folder) {
		this.folder = folder;
	}

	public Folder getFolder() {
		return this.folder;
	}

	public String getPath() {
		return this.path;
	}

	public String getTitle() {
		return this.title;
	}

	public String getSubfolder() {
		return this.subfolder;
	}

	public void setStatus(String s) {
		this.status.set(s);
	}

	public SimpleStringProperty getStatus() {
		return this.status;
	}

	public String getName() {
		return this.getTitle() + " - " + this.getSubfolder();
	}

	public SimpleBooleanProperty getSynced() {
		return this.synced;
	}

	public void validateSub() {
		this.isValid = true;
	}

	public boolean getSubValid() {
		return this.isValid;
	}
}
