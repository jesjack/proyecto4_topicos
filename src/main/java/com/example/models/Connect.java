package com.example.models;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Connect {
    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // Driver de MySQL mysql-connector-j-8.0.33.jar
            return DriverManager.getConnection("jdbc:mysql://localhost:3306/proyecto4_topicos", "user", "");
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
