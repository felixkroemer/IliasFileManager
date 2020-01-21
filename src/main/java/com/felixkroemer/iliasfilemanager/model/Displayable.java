package com.felixkroemer.iliasfilemanager.model;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

public interface Displayable {
	public String getName();
	public SimpleStringProperty getStatus();
	public SimpleBooleanProperty getSynced();
}
