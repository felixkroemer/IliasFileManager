package com.felixkroemer.iliasfilemanager.control;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.felixkroemer.iliasfilemanager.Constant;
import com.felixkroemer.iliasfilemanager.model.Displayable;
import com.felixkroemer.iliasfilemanager.model.Model;
import com.felixkroemer.iliasfilemanager.model.Subscription;
import com.felixkroemer.iliasfilemanager.model.items.Folder;
import com.felixkroemer.iliasfilemanager.model.items.Item;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.text.Text;

public class MainController {
	private Model model;
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
	Text statusText;

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
			this.statusText.setText(newValue);
		});

		new Thread(new Runnable() {
			@Override
			public void run() {
				model.getLoadingProperty().set(true);
				model.initSubscriptions();
				if (model.initSession()) {
					model.findSubscriptionFolders();
					model.detectAllSycedFiles();
					for (Subscription s : model.getValidSubscriptions()) {
						recursiveTTVInsert(subscriptionMap.get(s), s.getFolder().getChildren());
					}
				}
				model.getLoadingProperty().set(false);
			}
		}).start();
	}

	public void recursiveTTVInsert(TreeItem<Displayable> parent, Set<Item> children) {
		for (Item child : children) {
			TreeItem<Displayable> treeItem = new TreeItem<Displayable>(child);
			parent.getChildren().add(treeItem);
			if (child.getType() == Constant.ITEM_TYPE.FOLDER) {
				recursiveTTVInsert(treeItem, ((Folder) child).getChildren());
			}
		}
	}

	public void initialize() {
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

		this.statusText.wrappingWidthProperty().bind(subscriptionTTV.widthProperty());
	}

	@FXML
	private void syncAll(ActionEvent e) {
		this.model.syncAll();
	}
}
