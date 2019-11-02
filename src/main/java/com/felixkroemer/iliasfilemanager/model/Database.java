package com.felixkroemer.iliasfilemanager.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
	
	protected Connection c = null;

	public Database(String dbName) {
		try {
			Class.forName("org.sqlite.JDBC");
			this.c = DriverManager.getConnection("jdbc:sqlite:" + dbName);
		} catch (Exception e) {
			System.err.println("Can't open Database " + dbName);
			System.exit(0);
			e.printStackTrace();
		}
	}

	protected void disconnect() {
		try {
			this.c.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
