package com.felixkroemer.iliasfilemanager;

import com.felixkroemer.iliasfilemanager.control.MainController;
import com.felixkroemer.iliasfilemanager.model.Model;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class IFM extends Application {
	public static void main(String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		Settings.init(getParameters().getRaw().toArray(new String[0]));
		LoginCredentialsDialog.requestCredentials();
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/Main.fxml"));
		stage.setScene(new Scene(fxmlLoader.load()));
		stage.show();
		((MainController) fxmlLoader.getController()).injectModel(new Model());
	}
}
