/* JerryFX - A Chess Graphical User Interface
 * Copyright (C) 2020 Dominik Klein
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.asdfjkl.jerryfx.gui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import jfxtras.styles.jmetro.JMetro;
import org.asdfjkl.jerryfx.engine.UciEngineProcess;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.asdfjkl.jerryfx.gui.EngineOption.*;

public class DialogEngines {

    final FileChooser fileChooser = new FileChooser();

    Stage stage;
    boolean accepted = false;

    ObservableList<Engine> engineList;
    ListView<Engine> engineListView;

    final Button btnAdd = new Button("Add...");
    final Button btnRemove = new Button("Remove...");
    final Button btnEditParameters = new Button("Edit Parameters");
    final Button btnResetParameters = new Button("Reset Parameters");

    Button btnOk;
    Button btnCancel;

    int selectedIndex = 0;

    public boolean show(ArrayList<Engine> engines, int idxSelectedEngine) {

        engineList = FXCollections.observableArrayList(engines);

        engineListView = new ListView<Engine>();
        engineListView.setItems(engineList);

        engineListView.setCellFactory(param -> new ListCell<Engine>() {
            @Override
            protected void updateItem(Engine item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null || item.getName() == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        engineListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Engine>() {
            @Override
            public void changed(ObservableValue<? extends Engine> observable, Engine oldValue, Engine newValue) {

                selectedIndex = engineList.indexOf(newValue);
                if(selectedIndex == 0) {
                    btnEditParameters.setDisable(true);
                    btnResetParameters.setDisable(true);
                    btnRemove.setDisable(true);
                } else {
                    btnEditParameters.setDisable(false);
                    btnResetParameters.setDisable(false);
                    btnRemove.setDisable(false);
                }
            }
        });

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        btnOk = new Button();
        btnOk.setText("OK");

        btnCancel = new Button();
        btnCancel.setText("Cancel");
        Region spacer = new Region();

        HBox hbButtons = new HBox();
        hbButtons.getChildren().addAll(spacer, btnOk, btnCancel);
        hbButtons.setHgrow(spacer, Priority.ALWAYS);
        hbButtons.setSpacing(10);

        VBox vbButtonsRight = new VBox();
        vbButtonsRight.setPrefWidth(120);
        Region spacer1 = new Region();
        btnAdd.setMinWidth(vbButtonsRight.getPrefWidth());
        btnRemove.setMinWidth(vbButtonsRight.getPrefWidth());
        btnEditParameters.setMinWidth(vbButtonsRight.getPrefWidth());
        btnResetParameters.setMinWidth(vbButtonsRight.getPrefWidth());

        vbButtonsRight.getChildren().addAll(btnAdd, btnRemove,
                spacer1,
                btnEditParameters, btnResetParameters);
        vbButtonsRight.setVgrow(spacer1, Priority.ALWAYS);
        vbButtonsRight.setSpacing(10);
        vbButtonsRight.setPadding( new Insets(0,0,0,10));

        HBox hbMain = new HBox();
        hbMain.getChildren().addAll(engineListView, vbButtonsRight);
        hbMain.setHgrow(engineListView, Priority.ALWAYS);

        VBox vbMain = new VBox();
        vbMain.getChildren().addAll(hbMain, hbButtons);
        vbMain.setVgrow(hbMain, Priority.ALWAYS);
        vbMain.setSpacing(10);
        vbMain.setPadding( new Insets(10));

        btnOk.setOnAction(e -> {
            btnOkClicked();
        });

        btnCancel.setOnAction(e -> {
            btnCancelClicked();
        });

        btnAdd.setOnAction(e -> {
            btnAddEngineClicked();
        });

        btnRemove.setOnAction(e -> {
            btnRemoveEngineClicked();
        });

        btnEditParameters.setOnAction(e -> {
            btnEditParametersClicked();
        });

        btnResetParameters.setOnAction(e -> {
            btnResetParametersClicked();
        });

        engineListView.getSelectionModel().select(idxSelectedEngine);

        Scene scene = new Scene(vbMain);

        JMetro jMetro = new JMetro();
        jMetro.setScene(scene);
        stage.setScene(scene);
        stage.getIcons().add(new Image("icons/app_icon.png"));

        stage.showAndWait();

        return accepted;
    }

    private void btnOkClicked() {
        accepted = true;
        stage.close();
    }

    private void btnCancelClicked() {
        accepted = false;
        stage.close();
    }

    private void btnRemoveEngineClicked() {
        Engine selectedEngine = engineListView.getSelectionModel().getSelectedItem();
        engineList.remove(selectedEngine);
        if(engineList.size() > 9) {
            btnAdd.setDisable(true);
        } else {
            btnAdd.setDisable(false);
        }
    }

    private void btnResetParametersClicked() {

        Engine selectedEngine = engineListView.getSelectionModel().getSelectedItem();
        for(EngineOption enOpt : selectedEngine.options) {
            enOpt.resetToDefault();
        }
    }

    private void btnEditParametersClicked() {
        Engine selectedEngine = engineListView.getSelectionModel().getSelectedItem();
        DialogEngineOptions dlg = new DialogEngineOptions();
        boolean accepted = dlg.show(selectedEngine.options);
        if(accepted) {
            // collect all entries from dialog
            for(EngineOption enOpt : selectedEngine.options) {

                String optName = enOpt.name;
                if(enOpt.type == EN_OPT_TYPE_CHECK) {
                    CheckBox widget = dlg.checkboxWidgets.get(optName);
                    if(widget != null) {
                        enOpt.checkStatusValue = widget.isSelected();
                    }
                }
                if(enOpt.type == EN_OPT_TYPE_COMBO) {
                    ComboBox<String> widget = dlg.comboboxWidgets.get(optName);
                    if(widget != null) {
                        enOpt.comboValue = widget.getSelectionModel().getSelectedItem();
                    }
                }
                if(enOpt.type == EN_OPT_TYPE_SPIN) {
                    Spinner<Integer> widget = dlg.spinnerWidgets.get(optName);
                    if(widget != null) {
                        enOpt.spinValue = widget.getValue();
                    }
                }
                if(enOpt.type == EN_OPT_TYPE_STRING) {
                    TextField widget = dlg.textfieldWidgets.get(optName);
                    if(widget != null) {
                        enOpt.stringValue = widget.getText();
                    }
                }
            }
        }
    }

    private void btnAddEngineClicked() {

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            Engine engine = new Engine();
            engine.setPath(file.getAbsolutePath());

            try (UciEngineProcess engineProcess = new UciEngineProcess(file)) {

                List<String> reply = engineProcess.sendSynchronous("uci");
                for(String line: reply) {
                    if (line.startsWith("id name")) {
                        engine.setName(line.substring(7).trim());
                    }
                    else {
                        EngineOption engineOption = new EngineOption();
                        boolean parsed = engineOption.parseUciOptionString(line);
                        if (parsed) {
                            engine.options.add(engineOption);
                        }
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            if(engine.getName() != null && !engine.getName().isEmpty()) {
                engineList.add(engine);
                int idx = engineList.indexOf(engine);
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        engineListView.scrollTo(idx);
                        engineListView.getSelectionModel().select(idx);
                    }
                });
            }
        }

        if(engineList.size() > 9) {
            btnAdd.setDisable(true);
        } else {
            btnAdd.setDisable(false);
        }
    }

}
