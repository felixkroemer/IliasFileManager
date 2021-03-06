package com.felixkroemer.iliasfilemanager.model.items;

import com.felixkroemer.iliasfilemanager.Constant;
import com.felixkroemer.iliasfilemanager.Constant.ITEM_TYPE;
import com.felixkroemer.iliasfilemanager.model.Displayable;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

public abstract class Item implements Displayable {

	private ITEM_TYPE type;
	private String name;
	private String url;
	private String phpsessid;
	private int id;
	private SimpleStringProperty status;
	private SimpleBooleanProperty synced;

	public Item(ITEM_TYPE type, String name, String url, String phpsessid) {
		this.type = type;
		this.url = url;
		this.name = name;
		this.phpsessid = phpsessid;
		this.id = Item.getIdFromURL(url);
		this.status = new SimpleStringProperty();
		this.status.set("");
		this.synced = new SimpleBooleanProperty();
		this.synced.set(false);
	}

	public String getName() {
		return this.name;
	}

	public ITEM_TYPE getType() {
		return this.type;
	}

	public int getID() {
		return this.id;
	}

	public String getSessionID() {
		return this.phpsessid;
	}

	public void setSessionID(String phpsessid) {
		this.phpsessid = phpsessid;
	}

	public String getURL() {
		return this.url;
	}

	public static int getIdFromURL(String url) {
		try {
			ITEM_TYPE type = Item.getType(url);
			if (type == null) {
				return -1;
			}
			if (type != ITEM_TYPE.FILE) {
				int start = url.indexOf("id=") + 3;
				return Integer.valueOf(url.substring(start, url.indexOf("&", start + 1)));
			} else {
				int start = url.indexOf("file_") + 5;
				int end = url.indexOf("_download");
				return Integer.valueOf(url.substring(start, end));
			}
		} catch (StringIndexOutOfBoundsException e) {
			return -1;
		}
	}

	public static ITEM_TYPE getType(String url) {
		if (url.toLowerCase().matches(Constant.FOLDER_LINK_PATTERN_1)
				|| url.toLowerCase().matches(Constant.FOLDER_LINK_PATTERN_2)) {
			return ITEM_TYPE.FOLDER;
		} else if (url.toLowerCase().matches(Constant.FILE_LINK_PATTERN)) {
			return ITEM_TYPE.FILE;
		} else {
			return null;
		}
	}

	public SimpleStringProperty getStatus() {
		return this.status;
	}

	public SimpleBooleanProperty getSynced() {
		return this.synced;
	}

	public void setSynced(boolean b) {
		this.synced.set(b);
	}
}
