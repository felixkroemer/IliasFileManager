package com.felixkroemer.iliasfilemanager;

import com.felixkroemer.iliasfilemanager.model.Model;

public class IFM {
	public static void main(String[] args){
		Settings settings = new Settings(args);
		String[] credentials = LoginCredentialsDialog.getCredentials(settings.getUser());
		settings.setUser(credentials[0]);
		settings.setPassword(credentials[1]);
		Model m = new Model(settings);
		m.start();
	}
}
