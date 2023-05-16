package com.example.proyecto4_topicos.tools;

import com.example.models.Model;
import com.example.models.ModelColumn;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicException;
import net.sf.jmimemagic.MagicMatchNotFoundException;
import net.sf.jmimemagic.MagicParseException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ModelTable extends TableView<Model> {
    private final String modelName;
    private ModelTable(String modelName, Map<String, Object> limits) {
        super();
        this.modelName = modelName;
        setContextMenu(new ContextMenu());
        getContextMenu().getItems().add(new AddItem());
        getContextMenu().getItems().add(new DeleteItem());
        if (limits == null) getItems().addAll(Model.all(modelName));
        else                getItems().addAll(Model.find(modelName, limits));
        Model.getColumns(modelName).forEach(this::addColumn);

        setEditable(true);
    }
    public ModelTable(String modelName) {
        this(modelName, null);
    }
    private String otherModelName;
    private String modelKey;
    private String otherModelKey;
    private Map<String, Object> limits;
    private ModelTable(String modelName, String otherModelName, String modelKey, String otherModelKey, Map<String, Object> limits) {
        super();
        this.modelName = modelName;
        this.otherModelName = otherModelName;
        this.modelKey = modelKey;
        this.otherModelKey = otherModelKey;
        this.limits = limits;
        System.out.println("{modelName: " + modelName + ", otherModelName: " + otherModelName + ", modelKey: " + modelKey + ", otherModelKey: " + otherModelKey + ", limits: " + limits + "}");
        setContextMenu(new ContextMenu());
        getContextMenu().getItems().add(new AddItem());
        getContextMenu().getItems().add(new DeleteItem());
        getItems().addAll(Model.find(modelName, otherModelName, modelKey, otherModelKey, limits));
        Model.getColumns(modelName).forEach(this::addColumn);

        setEditable(true);
    }

    private void addColumn(ModelColumn modelColumn) {
        for (ModelColumn relation : modelColumn.getRelations()) {
            String mainSchemaRelationated = relation.getTableSchema();
            String mainTableRelationated = relation.getTableName();
            String modelName = mainSchemaRelationated + "." + mainTableRelationated;
            getContextMenu().getItems().add(new ShowOtherTable(modelName, modelColumn, relation));
        }

        if (modelColumn.isForeignKey()) {
            TableColumn<Model, String> tableColumn = new TableColumn<>(modelColumn.getName());
            tableColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(modelColumn) != null ? param.getValue().get(modelColumn).toString() : ""));
            tableColumn.setCellFactory(modelStringTableColumn -> selectForeignKeyCellFactory(modelColumn));
            getColumns().add(tableColumn);
            return;
        }

        if (modelColumn.getCompleteType().equals("tinyint(1)") || modelColumn.getCompleteType().equals("bit(1)") || modelColumn.getType().equals("boolean")) {
            TableColumn<Model, Boolean> tableColumn = new TableColumn<>(modelColumn.getName());
            tableColumn.setCellValueFactory(param -> new SimpleBooleanProperty(param.getValue().get(modelColumn) != null ? (Boolean) param.getValue().get(modelColumn) : false));
            tableColumn.setCellFactory(this::checkBoxCellFactory);
            getColumns().add(tableColumn);
            return;
        }

        if (modelColumn.getType().equals("date")) {
            // date picker
            TableColumn<Model, Date> tableColumn = new TableColumn<>(modelColumn.getName());
            tableColumn.setCellValueFactory(param -> new SimpleObjectProperty<>((Date) param.getValue().get(modelColumn)));
            tableColumn.setCellFactory(this::dateCellFactory);
            getColumns().add(tableColumn);
            return;
        }

        if (modelColumn.getType().equals("datetime")) {
            // datetime picker
            TableColumn<Model, LocalDateTime> tableColumn = new TableColumn<>(modelColumn.getName());
            tableColumn.setCellValueFactory(param -> new SimpleObjectProperty<>((LocalDateTime) param.getValue().get(modelColumn)));
            tableColumn.setCellFactory(this::dateTimeCellFactory);
            getColumns().add(tableColumn);
            return;
        }

        if (modelColumn.getType().contains("blob")) {
            TableColumn<Model, String> tableColumn = new TableColumn<>(modelColumn.getName());
            tableColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(modelColumn) == null ? "" : "Blob"));
            tableColumn.setCellFactory(modelStringTableColumn -> blobCellFactory(modelColumn));
            getColumns().add(tableColumn);
            return;
        }

        TableColumn<Model, String> tableColumn = new TableColumn<>(modelColumn.getName());
        tableColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(modelColumn) != null ? param.getValue().get(modelColumn).toString() : ""));
        if(!modelColumn.getKey().equals("PRI")) tableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        tableColumn.setOnEditCommit(this::editStringCol);
        getColumns().add(tableColumn);
    }

    private TableCell<Model, String> blobCellFactory(ModelColumn modelColumn) {
        // label, on double click, open file chooser
        return new TableCell<Model, String>() {
            private final Button change = new Button("Change") {{
                setOnAction(event -> {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Open Resource File");
                    File file = fileChooser.showOpenDialog(new Stage());
                    if (file == null) return;
                    try {
                        byte[] bytes = Files.readAllBytes(file.toPath());
                        Model modelToSave = getItems().get(getIndex());
                        modelToSave.set(modelColumn, bytes);
                        tryToSaveModel(modelToSave);
                        refresh();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }};
            private final Button open = new Button("Open") {{
                setOnAction(event -> {
                    byte[] bytes = (byte[]) getItems().get(getIndex()).get(modelColumn);
                    if (bytes == null) return;
                    try {
                        // open in explorer
                        String suffix = "." + Magic.getMagicMatch(bytes).getExtension();
                        File file = File.createTempFile("temp", suffix);
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(bytes);
                        fos.close();
                        // exec command to open folder contains file
                        Runtime.getRuntime().exec("explorer.exe /select," + file.getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (MagicMatchNotFoundException | MagicException | MagicParseException e) {
                        throw new RuntimeException(e);
                    }
                });
            }};
            private final HBox hBox = new HBox(change, open);
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                // set graphic
                setGraphic(empty ? null : hBox);
            }
        };
    }

    private TableCell<Model, String> selectForeignKeyCellFactory(ModelColumn modelColumn) {
        return new TableCell<Model, String>() {
            private final Label label = new Label() {{
                // on double click
                setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        Model forgeinModel = editForeignKey(modelColumn);
                        if (forgeinModel == null) return;
                        ModelColumn primaryKeyColumn = forgeinModel.getColumns().stream().filter(ModelColumn::isPrimaryKey).findFirst().orElseThrow();
                        Object primaryKey = forgeinModel.get(primaryKeyColumn);
                        Model model = getTableView().getItems().get(getIndex());
                        model.set(modelColumn, primaryKey);
                        tryToSaveModel(model);
                        refresh();
                    }
                });
            }};
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                label.setText(item);
                // set 100% width and height
                label.setMaxWidth(Double.MAX_VALUE);
                label.setMaxHeight(Double.MAX_VALUE);
                setGraphic(empty ? null : label);
            }
        };
    }

    private Model editForeignKey(ModelColumn modelColumn) {
        TableView<Model> tableView = new ModelTable(modelColumn.getRelation().getTableSchema() + "." + modelColumn.getRelation().getTableName());
        Button button = new Button("Asignar");
        VBox vBox = new VBox();
        vBox.getChildren().add(tableView);
        vBox.getChildren().add(button);
        StackPane secondaryLayout = new StackPane();
        secondaryLayout.getChildren().add(vBox);

        Stage newWindow = new Stage();
        newWindow.setTitle("Asignar " + modelColumn.getTableName());
        newWindow.setScene(new Scene(secondaryLayout, 230, 100));
        newWindow.initOwner(getScene().getWindow());
        newWindow.initModality(Modality.WINDOW_MODAL);

        AtomicReference<Model> selectedModel = new AtomicReference<>(null);
        button.setOnAction(actionEvent -> {
            selectedModel.set(tableView.getSelectionModel().getSelectedItem());
            newWindow.close();
        });
        // on focus row, change button text
        button.setDisable(true);
        tableView.getSelectionModel().selectedItemProperty().addListener((observableValue, model, t1) -> {
            if (t1 == null) {
                button.setText("Asignar");
                button.setDisable(true);
                return;
            }
            ModelColumn primaryKeyColumn = t1.getColumns().stream().filter(ModelColumn::isPrimaryKey).findFirst().orElseThrow();
            button.setText("Asignar " + t1.get(primaryKeyColumn));
            button.setDisable(false);
        });

        newWindow.showAndWait();

        return selectedModel.get();
    }

    private TableCell<Model, Date> dateCellFactory(TableColumn<Model, Date> modelDateTableColumn) {
        return new TableCell<>() {
            private final DatePicker datePicker = new DatePicker();
            {
                datePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
                    if (getIndex() < 0) {
                        return;
                    }
                    Model model = getTableView().getItems().get(getIndex());
                    String columnName = modelDateTableColumn.getText();
                    model.set(columnName, newValue);
                    tryToSaveModel(model);
                });
            }

            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null)
                    datePicker.setValue(item.toLocalDate());
                setGraphic(empty ? null : datePicker);
            }
        };
    }

    private TableCell<Model, LocalDateTime> dateTimeCellFactory(TableColumn<Model, LocalDateTime> modelLocalDateTimeTableColumn) {
        // only date picker and convert it to local date time
        return new TableCell<>() {
            private final DatePicker datePicker = new DatePicker();
            {
                datePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
                    if (getIndex() < 0) {
                        return;
                    }
                    Model model = getTableView().getItems().get(getIndex());
                    String columnName = modelLocalDateTimeTableColumn.getText();
                    LocalDateTime localDateTime = newValue.atStartOfDay();
                    model.set(columnName, localDateTime);
                    tryToSaveModel(model);
                });
            }

            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null)
                    datePicker.setValue(item.toLocalDate());
                setGraphic(empty ? null : datePicker);
            }
        };
    }

    private void tryToSaveModel(Model model) {
        try {
            HashSet<String> values1 = getHashSetFromModel(model);
            model.save();
            HashSet<String> values2 = getHashSetFromModel(model);
            if (!values1.containsAll(values2) || !values2.containsAll(values1)) {
                refresh();
            }

            if (otherModelName != null) {
                Map<String, Object> limits2 = Map.of(otherModelKey, model.get(modelKey));
                // concat limpits with limits
                HashMap<String, Object> concat = new HashMap<>();
                concat.putAll(limits);
                concat.putAll(limits2);
                Model otherModel = Model.find(otherModelName, concat).stream().findFirst().orElse(null);
                if (otherModel == null) {
                    otherModel = new Model(otherModelName);
                    otherModel.set(otherModelKey, model.get(modelKey));
                    Model finalOtherModel = otherModel;
                    limits.forEach((key, value) -> {
                        String tableName = key.split("\\.")[0];
                        String columnName = key.split("\\.")[1];
                        if (tableName.equals(otherModelName)) {
                            finalOtherModel.set(columnName, value);
                        }
                    });
                }
                otherModel.save();
                System.out.println(otherModel);
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().trim().startsWith("column")) {
                System.out.println(e.getMessage());
                return;
            }
            e.printStackTrace();
        }
    }

    private TableCell<Model, Boolean> checkBoxCellFactory(TableColumn<Model, Boolean> modelBooleanTableColumn) {
        return new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                    if (getIndex() < 0) {
                        return;
                    }
                    Model model = getTableView().getItems().get(getIndex());
                    String columnName = modelBooleanTableColumn.getText();
                    model.set(columnName, newValue);
                    tryToSaveModel(model);
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                checkBox.setSelected(item != null && item);
                setGraphic(empty ? null : checkBox);
            }
        };
    }
    private void editStringCol(TableColumn.CellEditEvent<Model, String> event) {
        Model model = event.getRowValue();
        String columnName = event.getTableColumn().getText();
        model.set(columnName, event.getNewValue());
        tryToSaveModel(model);
    }

    private static HashSet<String> getHashSetFromModel(Model model) {
        return new HashSet<>(model.getColumns().stream().map(model::get).map(o -> o == null ? "" : o.toString()).toList());
    }

    private class AddItem extends MenuItem {
        public AddItem() {
            super("Añadir nuevo item");
            setOnAction(event -> getItems().add(new Model(modelName)));
        }
    }
    private class DeleteItem extends MenuItem {
        public DeleteItem() {
            super("Borrar item");
            setOnAction(event -> {
                Model model = getSelectionModel().getSelectedItem();
                if (model != null) {
                    getItems().remove(model);
                    model.delete();
                }
            });
        }
    }
    private class ShowOtherTable extends MenuItem {
        public ShowOtherTable(String modelName, ModelColumn mainModelColumn, ModelColumn relationModelColumn) {
            super("Mostrar " + modelName);
            setOnAction(event -> {
                Model model = getSelectionModel().getSelectedItem();
                if (model != null) {
                    AtomicBoolean allColumnsAreForgeinKeyOrPrimaryKey = new AtomicBoolean(true);
                    AtomicReference<ModelColumn> forgeinColumn = new AtomicReference<>();
                    Model.getColumns(modelName).forEach(modelColumn -> {
                        if (!modelColumn.isForeignKey() && !modelColumn.isPrimaryKey()) {
                            allColumnsAreForgeinKeyOrPrimaryKey.set(false);
                        } else {
                            if (modelColumn.isForeignKey() && !(modelColumn.getRelation().getTableName() + "." + modelColumn.getRelation().getName()).equals(mainModelColumn.getTableName() + "." + mainModelColumn.getName())) {
                                forgeinColumn.set(modelColumn);
                            }
                        }
                    });

                    ModelTable table;
                    Map<String, Object> limits;
                    if (allColumnsAreForgeinKeyOrPrimaryKey.get()) {
                        String cond1 = relationModelColumn.getTableName() + "." + relationModelColumn.getName();
                        Object cond2 = model.get(mainModelColumn);
                        limits = Map.of(cond1, cond2);
                        /*
                          SELECT modelName.* FROM modelName JOIN otherModelName ON modelName.modelKey = otherModelName.otherModelKey WHERE key1 = value1 AND key2 = value2 AND ... [map]
                          public static List<Model> find(String modelName, String otherModelName, String modelKey, String otherModelKey, Map<String, Object> where) {
                         */
                        String __modelName = forgeinColumn.get().getRelation().getTableName();
                        String __otherModelName = relationModelColumn.getTableName();
                        String __modelKey = forgeinColumn.get().getRelation().getName();
                        String __otherModelKey = forgeinColumn.get().getName();
                        table = new ModelTable(__modelName, __otherModelName, __modelKey, __otherModelKey, limits);
                    } else {
                        limits = Map.of(relationModelColumn.getName(), model.get(mainModelColumn));
                        table = new ModelTable(modelName, limits);
                    }

                    StackPane secondaryLayout = new StackPane();
                    secondaryLayout.getChildren().add(table);

                    Scene secondScene = new Scene(secondaryLayout, 230, 100);
                    Stage newWindow = new Stage();
                    newWindow.setTitle(modelName);
                    newWindow.setScene(secondScene);

                    newWindow.show();
                }
            });
        }
    }
}
