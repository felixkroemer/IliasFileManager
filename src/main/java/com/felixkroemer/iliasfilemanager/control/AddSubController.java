package com.felixkroemer.iliasfilemanager.control;

import com.felixkroemer.iliasfilemanager.model.Model;
import com.felixkroemer.iliasfilemanager.model.items.Folder;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.text.Text;

public class AddSubController {
	private Model model;
	private Folder selectedFolder;

	@FXML
	TreeView<Folder> treeView;

	@FXML
	Text statusText;

	@FXML
	Button addButton;

	public void injectModel(Model m) {
		this.model = m;
		this.selectedFolder = null;
		for (Folder f : this.model.getCourses()) {
			this.treeView.getRoot().getChildren().add(new LazyLoadingTreeItem(f));
		}
	}

	@FXML
	private void initialize() {
		this.treeView.setRoot(new TreeItem<Folder>());
		this.treeView.setShowRoot(false);

		this.addButton.setDisable(true);

		this.treeView.setCellFactory(treeview -> {
			return new TreeCell<Folder>() {
				@Override
				protected void updateItem(Folder f, boolean empty) {
					super.updateItem(f, empty);
					if (!empty) {
						setText(f.getName());
					} else {
						setText("");
						setGraphic(null);
					}
				}
			};
		});

		this.treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
			if (this.addButton.isDisable()) {
				this.addButton.setDisable(false);
			}
			this.selectedFolder = newValue.getValue();
		});
	}

	@FXML
	private void addSubscription(ActionEvent e) {
		// TODO
		System.out.println(this.selectedFolder.getName());
	}

	class LazyLoadingTreeItem extends TreeItem<Folder> {
		private boolean childrenLoaded;

		public LazyLoadingTreeItem(Folder f) {
			super(f);
			this.childrenLoaded = false;
		}

		@Override
		public boolean isLeaf() {
			if (childrenLoaded) {
				return getChildren().isEmpty();
			} else {
				return false;
			}
		}

		@Override
		public ObservableList<TreeItem<Folder>> getChildren() {
			if (childrenLoaded) {
				return super.getChildren();
			} else {
				for (Folder f : this.getValue().getSubfolders()) {
					super.getChildren().add(new LazyLoadingTreeItem(f));
				}
				this.childrenLoaded = true;
				return super.getChildren();
			}
		}
	}
}
