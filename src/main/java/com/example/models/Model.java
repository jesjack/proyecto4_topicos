package com.example.models;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Model {
    private static Connection connection;
    public String modelName;
    private final Map<String, Object> data = new HashMap<>();
    private final Map<String, Integer> keysetOrder = new HashMap<>();

    public Model(String modelName) {
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public void set(String key, Object value) {
        this.data.put(key, value);
        if (!this.keysetOrder.containsKey(key)) {
            this.keysetOrder.put(key, this.keysetOrder.size());
        }
    }

    public void set(ModelColumn keyColumn, Object value) {
        String key = (String) keyColumn.getData().get("COLUMN_NAME");
        this.set(key, value);
    }

    public Object get(String key) {
        return data.get(key);
    }
    public Object get(ModelColumn modelColumn) {
        return data.get(modelColumn.getData().get("COLUMN_NAME"));
    }

    public List<ModelColumn> getColumns() {
        return getColumns(this.modelName);
    }

    public static List<ModelColumn> getColumns(String modelName) {
        // if modelname have schema, then split it
        String[] modelNameSplit = modelName.split("\\.");
        String schemaName = modelNameSplit.length > 1 ? modelNameSplit[0] : null;
        String tableName = modelNameSplit.length > 1 ? modelNameSplit[1] : modelNameSplit[0];
        // get columns from information_schema.columns
        ArrayList<ModelColumn> modelColumns = new ArrayList<>();
        try {
            ResultSet rs = connection.getMetaData().getColumns(null, schemaName, tableName, null);
            while (rs.next()) {
                ModelColumn modelColumn = new ModelColumn(tableName, rs.getString("COLUMN_NAME"));
                modelColumns.add(modelColumn);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        // order columns by ordinal_position
        modelColumns.sort((o1, o2) -> {
            Long o1OrdinalPosition = (Long) o1.getData().get("ORDINAL_POSITION");
            Long o2OrdinalPosition = (Long) o2.getData().get("ORDINAL_POSITION");
            return o1OrdinalPosition.compareTo(o2OrdinalPosition);
        });
        return modelColumns;
    }

    public static void setConnection(Connection con) {
        connection = con;
    }

    public static List<Model> all(String modelName) {
        try {
            ResultSet rs = connection.getMetaData().getTables(null, null, modelName, null);
            if (!rs.next()) {
                throw new RuntimeException("Table " + modelName + " does not exist.");
            }
            ArrayList<Model> models = new ArrayList<>();
            rs = connection.createStatement().executeQuery("SELECT * FROM " + modelName);
            while (rs.next()) {
                Model model = new Model(modelName);
                ResultSetMetaData rsmd = rs.getMetaData();
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    model.set(rsmd.getColumnName(i), rs.getObject(i));
                }
                models.add(model);
            }
            return models;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Model> find(String modelName, String key, Object value) {
        try {
            ResultSet rs = connection.getMetaData().getTables(null, null, modelName, null);
            if (!rs.next()) {
                throw new RuntimeException("Table " + modelName + " does not exist.");
            }
            ArrayList<Model> models = new ArrayList<>();
            String sql = "SELECT * FROM " + modelName + " WHERE " + key + " = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setObject(1, value);
            rs = ps.executeQuery();
            while (rs.next()) {
                Model model = new Model(modelName);
                ResultSetMetaData rsmd = rs.getMetaData();
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    model.set(rsmd.getColumnName(i), rs.getObject(i));
                }
                models.add(model);
            }
            return models;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Model findFirst(String modelName, String key, Object value) {
        try {
            ResultSet rs = connection.getMetaData().getTables(null, null, modelName, null);
            if (!rs.next()) {
                throw new RuntimeException("Table " + modelName + " does not exist.");
            }
            String sql = "SELECT * FROM " + modelName + " WHERE " + key + " = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setObject(1, value);
            rs = ps.executeQuery();
            if (rs.next()) {
                Model model = new Model(modelName);
                ResultSetMetaData rsmd = rs.getMetaData();
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    model.set(rsmd.getColumnName(i), rs.getObject(i));
                }
                return model;
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * SELECT * FROM modelName WHERE key1 = value1 AND key2 = value2 AND ... [map]
     */
    public static List<Model> find(String modelName, Map<String, Object> where) {
        try {
            ResultSet rs = connection.getMetaData().getTables(null, null, modelName, null);
            if (!rs.next()) {
                throw new RuntimeException("Table " + modelName + " does not exist.");
            }
            ArrayList<Model> models = new ArrayList<>();
            StringBuilder sql = new StringBuilder("SELECT * FROM " + modelName + " WHERE ");
            ArrayList<String> keys = new ArrayList<>(where.keySet());
            for (int i = 0; i < keys.size(); i++) {
                sql.append(keys.get(i)).append(" = ?");
                if (i < keys.size() - 1) {
                    sql.append(" AND ");
                }
            }
            PreparedStatement ps = connection.prepareStatement(sql.toString());
            for (int i = 0; i < keys.size(); i++) {
                ps.setObject(i + 1, where.get(keys.get(i)));
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                Model model = new Model(modelName);
                ResultSetMetaData rsmd = rs.getMetaData();
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    model.set(rsmd.getColumnName(i), rs.getObject(i));
                }
                models.add(model);
            }
            return models;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * SELECT modelName.* FROM modelName JOIN otherModelName ON modelName.modelKey = otherModelName.otherModelKey WHERE key1 = value1 AND key2 = value2 AND ... [map]
     */
    public static List<Model> find(String modelName, String otherModelName, String modelKey, String otherModelKey, Map<String, Object> where) {
        try {
            ResultSet rs = connection.getMetaData().getTables(null, null, modelName, null);
            if (!rs.next()) {
                throw new RuntimeException("Table " + modelName + " does not exist.");
            }
            rs = connection.getMetaData().getTables(null, null, otherModelName, null);
            if (!rs.next()) {
                throw new RuntimeException("Table " + otherModelName + " does not exist.");
            }
            ArrayList<Model> models = new ArrayList<>();
            StringBuilder sql = new StringBuilder("SELECT " + modelName + ".* FROM " + modelName + " JOIN " + otherModelName + " ON " + modelName + "." + modelKey + " = " + otherModelName + "." + otherModelKey + " WHERE ");
            ArrayList<String> keys = new ArrayList<>(where.keySet());
            for (int i = 0; i < keys.size(); i++) {
                sql.append(keys.get(i)).append(" = ?");
                if (i < keys.size() - 1) {
                    sql.append(" AND ");
                }
            }
            PreparedStatement ps = connection.prepareStatement(sql.toString());
            for (int i = 0; i < keys.size(); i++) {
                ps.setObject(i + 1, where.get(keys.get(i)));
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                Model model = new Model(modelName);
                ResultSetMetaData rsmd = rs.getMetaData();
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    model.set(rsmd.getColumnName(i), rs.getObject(i));
                }
                models.add(model);
            }
            return models;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void save() {
        try {
            String modelName;
            String schema;
            if (this.getModelName().contains(".")) {
                String[] parts = this.getModelName().split("\\.");
                schema = parts[0];
                modelName = parts[1];
            } else {
                schema = null;
                modelName = this.getModelName();
            }
            // first check if the modelName exists as table in the database
            ResultSet rs = connection.getMetaData().getTables(null, null, this.getModelName(), null);
            if (!rs.next()) {
                throw new RuntimeException("Table " + this.getModelName() + " does not exist.");
            }

            // check if the primary key exists
            rs = connection.getMetaData().getPrimaryKeys(null, schema, modelName);
            if (!rs.next()) {
                throw new RuntimeException("Table " + this.getModelName() + " does not have a primary key.");
            }

            // get the primary key
            String primaryKey = rs.getString("COLUMN_NAME");

            // check if model doesn't not have other columns than the table
            ArrayList<String> keys = new ArrayList<>(this.data.keySet());
            for (String key : keys) {
                rs = connection.getMetaData().getColumns(null, schema, modelName, key);
                if (!rs.next()) {
                    throw new RuntimeException("Column " + key + " does not exist in table " + this.getModelName());
                }
            }

            // check if the table doesn't have other not null columns than the model, except the primary key and default valued columns
            rs = connection.getMetaData().getColumns(null, schema, modelName, null);
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                if (!keys.contains(columnName) && !columnName.equals(primaryKey)) {
                    if (rs.getInt("NULLABLE") == 0) {
                        if (rs.getString("COLUMN_DEF") == null) {
                            throw new RuntimeException("Column " + columnName + " is not nullable.");
                        }
                    }
                }
            }

            // if the model doesn't have the primary key, insert
            if (!keys.contains(primaryKey)) {
                this.insert();
            } else {
                // check if the model's primary key, have a value in the database
                String sql = "SELECT * FROM " + this.getModelName() + " WHERE " + primaryKey + " = ?";
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setObject(1, this.data.get(primaryKey));
                rs = ps.executeQuery();
                if (!rs.next()) {
                    this.insert();
                } else {
                    this.update();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    private void insert() {
        // insert
        try {
            String primaryKey = getPrimaryKey();
            ResultSet rs;


            StringBuilder sql = new StringBuilder("INSERT INTO " + this.getModelName() + " (");
            ArrayList<String> keys = new ArrayList<>(this.data.keySet());
            for (String key : keys) {
                sql.append(key);
                if (keys.indexOf(key) < keys.size() - 1) {
                    sql.append(", ");
                }
            }
            sql.append(") VALUES (");
            for (int i = 0; i < keys.size(); i++) {
                sql.append("?");
                if (i < keys.size() - 1) {
                    sql.append(", ");
                }
            }
            sql.append(")");

            PreparedStatement ps = connection.prepareStatement(sql.toString());
            for (int i = 1; i <= keys.size(); i++) {
                ps.setObject(i, this.data.get(keys.get(i - 1)));
            }
            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new RuntimeException("Insert failed, no rows affected.");
            }

            ps = connection.prepareStatement("SELECT * FROM " + this.getModelName() + " WHERE " + primaryKey + " = LAST_INSERT_ID()");
            rs = ps.executeQuery();

            // now parse data from the database to the model
            if (rs.next()) {
                ResultSetMetaData rsmd = rs.getMetaData();
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    this.set(rsmd.getColumnName(i), rs.getObject(i));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void update() {
        // update
        try {
            String primaryKey = getPrimaryKey();
            ResultSet rs;

            StringBuilder sql = new StringBuilder("UPDATE " + this.getModelName() + " SET ");
            ArrayList<String> keys = new ArrayList<>(this.data.keySet());
            for (String key : keys) {
                sql.append(modelName).append(".").append(key).append(" = ?");
                if (keys.indexOf(key) < keys.size() - 1) {
                    sql.append(", ");
                }
            }
            sql.append(" WHERE ").append(primaryKey).append(" = ?");

            PreparedStatement ps = connection.prepareStatement(sql.toString());
            for (int i = 1; i <= keys.size(); i++) {
                ps.setObject(i, this.data.get(keys.get(i - 1)));
            }
            ps.setObject(keys.size() + 1, this.data.get(primaryKey));
//            System.out.println(ps.toString());
            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new RuntimeException("Update failed, no rows affected.");
            }

            // now parse data from the database to the model, to posible auto changed values
            ps = connection.prepareStatement("SELECT * FROM " + this.getModelName() + " WHERE " + primaryKey + " = ?");
            ps.setObject(1, this.data.get(primaryKey));
            rs = ps.executeQuery();

            if (rs.next()) {
                ResultSetMetaData rsmd = rs.getMetaData();
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    this.set(rsmd.getColumnName(i), rs.getObject(i));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String getPrimaryKey() throws SQLException {
        String primaryKey;
        String schema;
        String modelName;
        if (this.getModelName().contains(".")) {
            String[] parts = this.getModelName().split("\\.");
            schema = parts[0];
            modelName = parts[1];
        } else {
            schema = null;
            modelName = this.getModelName();
        }
        // get the primary key
        ResultSet rs = connection.getMetaData().getPrimaryKeys(null, schema, modelName);
        if (!rs.next()) {
            throw new RuntimeException("Table " + this.getModelName() + " does not have a primary key.");
        }
        primaryKey = rs.getString("COLUMN_NAME");
        return primaryKey;
    }

    public void delete() {
        // delete
        try {
            String primaryKey = getPrimaryKey();

            PreparedStatement ps = connection.prepareStatement("DELETE FROM " + this.getModelName() + " WHERE " + primaryKey + " = ?");
            ps.setObject(1, this.data.get(primaryKey));
            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new RuntimeException("Delete failed, no rows affected.");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> getData() {
        return this.data;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(this.getModelName() + " {");
        ArrayList<String> keys = new ArrayList<>(this.data.keySet());
        keys.sort((a, b) -> this.keysetOrder.get(a) - this.keysetOrder.get(b));
        for (String key : keys) {
            str.append(key).append(": ").append(this.get(key));
            if (keys.indexOf(key) < keys.size() - 1) {
                str.append(", ");
            }
        }
        str.append("}");
        return str.toString();
    }

    public static void main(String[] args) {

        Model.setConnection(Connect.getConnection());

        Model.find("vehiculos", "empleados_vehiculos", "id", "id_vehiculo", Map.of(
                "empleados_vehiculos.id_empleado", 1
        )).forEach(System.out::println);

    }
}
