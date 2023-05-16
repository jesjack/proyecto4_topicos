package com.example.models;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

public class ModelColumn extends Model {
    private static final String COLUMNS_GENERAL_NAME = "information_schema.columns";
    private static final String RELATIONS_GENERAL_NAME = "information_schema.key_column_usage";
    private final String table_schema;
    private final String table_name;
    private final String column_name;
    private final Long ordinal_position;
    private final String column_default;
    private final String is_nullable;
    private final String data_type;
    private final String column_type;
    private final String column_key;
    private final String column_comment;

    public ModelColumn(String tableName, String columnName) {
        super(COLUMNS_GENERAL_NAME);
        this.table_name = tableName;
        this.column_name = columnName;
        Model
                .find(COLUMNS_GENERAL_NAME, Map.of(
                        "TABLE_NAME", tableName,
                        "COLUMN_NAME", columnName))
                .stream()
                .findFirst()
                .orElseThrow()
                .getData()
                .forEach((key, value) ->
                        this.getData().put(key, value));

        this.table_schema = (String) this.getData().get("TABLE_SCHEMA");
        this.ordinal_position = (Long) this.getData().get("ORDINAL_POSITION");
        this.column_default = (String) this.getData().get("COLUMN_DEFAULT");
        this.is_nullable = (String) this.getData().get("IS_NULLABLE");
        this.data_type = (String) this.getData().get("DATA_TYPE");
        this.column_type = (String) this.getData().get("COLUMN_TYPE");
        this.column_key = (String) this.getData().get("COLUMN_KEY");
        this.column_comment = (String) this.getData().get("COLUMN_COMMENT");
    }

    @Override
    public String toString() {
        return "[" + table_schema + "." + table_name + "." + column_name + "](" + column_type + "){"
                + "ordinal_position=" + ordinal_position
                + ", column_default=" + column_default
                + ", is_nullable=" + is_nullable
                + ", data_type=" + data_type
                + ", column_key=" + column_key
                + "}" + (!column_comment.equals("") ? "// " + column_comment : "");
    }

    public static ModelColumn fromModel(Model model) {
        return new ModelColumn(
                (String) model.getData().get("TABLE_NAME"),
                (String) model.getData().get("COLUMN_NAME"));
    }

    public static void main(String[] args) {
        Model.setConnection(Connect.getConnection());
        ModelColumn modelColumn = new ModelColumn("vehiculos", "id");
        System.out.println(modelColumn);
        modelColumn.getRelations().forEach(System.out::println);
    }

    public String getName() {
        return column_name;
    }

    public String getType() {
        return data_type;
    }

    public String getCompleteType() {
        return column_type;
    }

    public String getKey() {
        return column_key;
    }

    public List<ModelColumn> getRelations() { // get foreign keys
        return Model
                .find(RELATIONS_GENERAL_NAME, Map.of(
                        "TABLE_SCHEMA", table_schema,
                        "REFERENCED_TABLE_NAME", table_name,
                        "REFERENCED_COLUMN_NAME", column_name))
                .stream()
                .map(ModelColumn::fromModel)
                .toList();
    }
    public ModelColumn getRelation() { // get referenced primary key
        String sql = "SELECT *\n" +
                "FROM information_schema.columns\n" +
                "WHERE TABLE_NAME = (\n" +
                "    SELECT REFERENCED_TABLE_NAME\n" +
                "    FROM information_schema.key_column_usage\n" +
                "    WHERE TABLE_NAME = '"+getTableName()+"' AND COLUMN_NAME = '"+getName()+"' AND REFERENCED_TABLE_NAME IS NOT NULL\n" +
                ") AND COLUMN_NAME = (\n" +
                "    SELECT REFERENCED_COLUMN_NAME\n" +
                "    FROM information_schema.key_column_usage\n" +
                "    WHERE TABLE_NAME = '"+getTableName()+"' AND COLUMN_NAME = '"+getName()+"' AND REFERENCED_COLUMN_NAME IS NOT NULL\n" +
                ")";
        String modelName = "information_schema.columns";
        Model generated = new Model(modelName);
        Connection connection = Connect.getConnection();
        // hacer consulta nativa
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            if (!resultSet.next()) {
                return null;
            }

            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                generated.set(metaData.getColumnName(i), resultSet.getObject(i));
            }
            return ModelColumn.fromModel(generated);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getTableName() {
        return table_name;
    }

    public String getTableSchema() {
        return table_schema;
    }

    public Boolean isPrimaryKey() {
        return column_key.equals("PRI");
    }
    public Boolean isUniqueKey() {
        return column_key.equals("UNI");
    }
    public Boolean isForeignKey() {
        return getRelation() != null;
    }
}
