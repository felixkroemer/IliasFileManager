package com.felixkroemer.iliasfilemanager.control;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.felixkroemer.iliasfilemanager.Constant;
import com.felixkroemer.iliasfilemanager.LoginCredentialsDialog;
import com.felixkroemer.iliasfilemanager.model.Displayable;
import com.felixkroemer.iliasfilemanager.model.Model;
import com.felixkroemer.iliasfilemanager.model.Subscription;
import com.felixkroemer.iliasfilemanager.model.items.Folder;
import com.felixkroemer.iliasfilemanager.model.items.Item;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainController {
	private Model model;
	private static final Logger logger = LogManager.getLogger(MainController.class);
	private Map<Displayable, TreeItem<Displayable>> subscriptionMap;

	@FXML
	TreeTableView<Displayable> subscriptionTTV;

	@FXML
	TreeTableColumn<Displayable, String> nameColumn;

	@FXML
	TreeTableColumn<Displayable, Boolean> syncedColumn;

	@FXML
	TreeTableColumn<Displayable, String> statusColumn;

	@FXML
	ProgressIndicator progressIndicator;

	@FXML
	MenuItem syncAllButton;

	@FXML
	TextFlow statusText;

	@FXML
	Hyperlink hyperlink;

	@FXML
	MenuItem addSubButton;

	public void injectModel(Model m) {
		this.model = m;
		this.subscriptionMap = new HashMap<Displayable, TreeItem<Displayable>>();

		model.getLoadingProperty().addListener((obs, oldValue, newValue) -> {
			progressIndicator.setVisible(newValue);
		});

		model.getSubscriptions().addListener((ListChangeListener<Subscription>) change -> {
			while (change.next()) {
				if (change.wasAdded()) {
					for (Subscription s : change.getAddedSubList()) {
						TreeItem<Displayable> t = new TreeItem<Displayable>(s);
						subscriptionTTV.getRoot().getChildren().add(t);
						this.subscriptionMap.put(s, t);
						if (s.getSubValid()) {
							this.recursiveTTVInsert(t, s.getFolder().getChildren());
						}
					}
				}
			}
		});

		model.getReadyToSyncProperty().addListener((obs, oldValue, newValue) -> {
			if (newValue) {
				this.syncAllButton.setDisable(false);
			} else {
				this.syncAllButton.setDisable(true);
			}
		});

		model.getStatusMessageProperty().addListener((obs, oldValue, newValue) -> {
			if (newValue.equals(Constant.WRONG_CREDS)) {
				hyperlink = new Hyperlink(" (Log In)");
				hyperlink.setOnAction(e -> {
					new Thread(() -> {
						LoginCredentialsDialog.requestCredentials();
						this.initModel();
					}).start();
				});
			} else {
				hyperlink = null;
			}
			Platform.runLater(() -> {
				this.statusText.getChildren().clear();
				this.statusText.getChildren().add(new Text(newValue));
				if (hyperlink != null) {
					this.statusText.getChildren().add(hyperlink);
				}
			});
		});

		new Thread(new Runnable() {
			@Override
			public void run() {
				model.initSubscriptions();
				initModel();
			}
		}).start();
	}

	private void initModel() {
		model.getLoadingProperty().set(true);
		if (this.model.initSession() && !this.model.getReadyToSyncProperty().get()) {
			this.model.findSubscriptionFolders();
			this.model.detectAllSyncedFiles();
			for (Subscription s : this.model.getValidSubscriptions()) {
				recursiveTTVInsert(subscriptionMap.get(s), s.getFolder().getChildren());
			}
		}
		model.getLoadingProperty().set(false);
	}

	private void recursiveTTVInsert(TreeItem<Displayable> parent, Set<Item> children) {
		for (Item child : children) {
			TreeItem<Displayable> treeItem = new TreeItem<Displayable>(child);
			parent.getChildren().add(treeItem);
			if (child.getType() == Constant.ITEM_TYPE.FOLDER) {
				recursiveTTVInsert(treeItem, ((Folder) child).getChildren());
			}
		}
	}

	@FXML
	private void initialize() {
		TreeItem<Displayable> root = new TreeItem<Displayable>();
		subscriptionTTV.setRoot(root);
		subscriptionTTV.setShowRoot(false);

		this.nameColumn.setCellValueFactory(cdf -> {
			return new ReadOnlyObjectWrapper<String>(cdf.getValue().getValue().getName());
		});
		this.nameColumn.prefWidthProperty().bind(subscriptionTTV.widthProperty().multiply(0.7));

		this.statusColumn.setCellValueFactory(cdf -> {
			return cdf.getValue().getValue().getStatus();
		});
		this.statusColumn.setStyle("-fx-alignment: CENTER;");
		this.statusColumn.prefWidthProperty().bind(subscriptionTTV.widthProperty().multiply(0.2));

		this.syncedColumn.setCellValueFactory(cdf -> {
			return cdf.getValue().getValue().getSynced();
		});
		this.syncedColumn.setStyle("-fx-alignment: CENTER;");
		this.syncedColumn.prefWidthProperty().bind(subscriptionTTV.widthProperty().multiply(0.1).subtract(4));

		this.syncedColumn.setCellFactory(ttc -> new TreeTableCell<Displayable, Boolean>() {
			@Override
			protected void updateItem(Boolean b, boolean empty) {
				super.updateItem(b, empty);
				if (!empty) {
					setText(b ? "\u2713" : "\u274C");
				} else {
					setText("");
				}
			}
		});

		this.progressIndicator.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
		this.progressIndicator.setVisible(false);

		this.syncAllButton.setDisable(true);

		this.statusText.prefWidthProperty().bind(subscriptionTTV.widthProperty());
	}

	@FXML
	private void syncAll(ActionEvent e) {
		this.model.syncAll();
	}

	@FXML
	private void addSubscription(ActionEvent e) {
		try {
			Stage stage = new Stage();
			stage.initModality(Modality.WINDOW_MODAL);
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/AddSub.fxml"));
			stage.setScene(new Scene(fxmlLoader.load()));
			stage.initOwner(subscriptionTTV.getScene().getWindow());
			stage.show();
			((AddSubController) fxmlLoader.getController()).injectModel(this.model);
		} catch (IOException exception) {
			logger.debug(exception);
		}
	}
}
