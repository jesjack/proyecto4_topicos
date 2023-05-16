package com.example.proyecto4_topicos;

import com.example.models.Model;
import com.example.proyecto4_topicos.tools.ModelTable;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class HelloController {
    @FXML
    public VBox box1;
    @FXML
    public VBox box2;

    public void initialize() {
        TableView<Model> doctors = new ModelTable("doctors");
        TableView<Model> patients = new ModelTable("patients");

        box1.getChildren().add(doctors);
        box2.getChildren().add(patients);
    }
}