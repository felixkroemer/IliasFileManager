package com.felixkroemer.iliasfilemanager;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class LoginCredentialsDialog {
	public static void requestCredentials() {
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		JPanel label = new JPanel(new GridLayout(0, 1, 2, 2));
		label.add(new JLabel("Username"));
		label.add(new JLabel("Password"));
		panel.add(label, BorderLayout.WEST);

		JPanel controls = new JPanel(new GridLayout(0, 1, 2, 2));
		JTextField usernameField = new JTextField();
		controls.add(usernameField);
		JPasswordField passwordField = new JPasswordField();
		controls.add(passwordField);
		panel.add(controls, BorderLayout.CENTER);
		
		String username = Settings.getConfig(Settings.Config.USERNAME);
		if(username!=null) {
			usernameField.setText(username);
		}

		JOptionPane.showMessageDialog(null, panel, "Login", JOptionPane.DEFAULT_OPTION);
		Settings.setConfig(Settings.Config.USERNAME, usernameField.getText());
		Settings.setConfig(Settings.Config.PASSWORD, new String(passwordField.getPassword()));
	}
}
