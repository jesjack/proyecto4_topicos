module com.example.proyecto4_topicos {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.java;
    requires jmimemagic;
    requires tornadofx.controls;


    opens com.example.proyecto4_topicos to javafx.fxml;
    exports com.example.proyecto4_topicos;
}