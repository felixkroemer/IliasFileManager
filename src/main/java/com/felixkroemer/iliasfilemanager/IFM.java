package com.felixkroemer.iliasfilemanager;

import com.felixkroemer.iliasfilemanager.model.Model;

public class IFM {
	public static void main(String[] args){
		Settings.init(args);
		LoginCredentialsDialog.requestCredentials();
		Model m = new Model();
		m.start();
	}
}
