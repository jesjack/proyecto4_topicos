package com.example.models;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ModelMaker {
    public List<String> getTables() {
        return Model.all("information_schema.tables")
                .stream()
                .map(model -> (String) model.get("TABLE_NAME"))
                .toList();
    }

    public List<String> getTables(String database) {
        return Model
                .find("information_schema.tables", "TABLE_SCHEMA", database)
                .stream()
                .map(model -> (String) model.get("TABLE_NAME"))
                .toList();
    }

    public String camelCase(String text) {
        // separate _
        List<String> list = Stream
                .of(text.split("_"))
        // separate white spaces
                .flatMap(word -> Stream.of(word.split(" ")))
        // if all letters on a word are uppercase, then lowercase
                .map(word -> {
                    boolean allUpperCase = true;
                    for (int i = 0; i < word.length(); i++) {
                        if (!Character.isUpperCase(word.charAt(i))) {
                            allUpperCase = false;
                            break;
                        }
                    }
                    if (allUpperCase) {
                        return word.toLowerCase();
                    }
                    return word;
                })
        // capitalize
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .toList();
        // join
        return String.join("", list);
    }

    public String pluralToSingular(String text) {
        if (text.endsWith("s")) {
            return text.substring(0, text.length() - 1);
        }
        if (text.endsWith("es")) {
            return text.substring(0, text.length() - 2);
        }
        return text;
    }

    public void generateTableModel(String tableName) throws IOException {
        File file = new File(Path.of("ModelTemplate.txt").toString());
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder result = new StringBuilder();
        String str;
        while ((str = reader.readLine()) != null) {
            result.append(str).append("\n");
        }
        String template = result.toString();

        String ClassName = camelCase(pluralToSingular(tableName));
        String database = tableName.split("\\.").length == 2 ? tableName.split("\\.")[0] : null;
        String TableName = tableName.split("\\.").length == 2 ? tableName.split("\\.")[1] : tableName;


        // now get table columns on to a list<string>
        List<Model> columns = database == null
                ? Model.find("information_schema.columns", "TABLE_NAME", TableName)
                : Model.find("information_schema.columns", Map.of(
                        "TABLE_SCHEMA", database,
                        "TABLE_NAME", TableName
                ));

        String ColumnNames = columns
                .stream()
                .map(column -> (String) column.get("COLUMN_NAME"))
                .reduce("", (a, b) -> a + ", \"" + b + "\"");
        ColumnNames = ColumnNames.substring(2);

        if (database == null) {
            database = (String) columns.get(0).get("TABLE_SCHEMA");
        }

        String fromModel = columns
                .stream()
                .map(column -> {
                    return String.format("m.set(\"%s\", model.get(\"%s\"));", column.get("COLUMN_NAME"), column.get("COLUMN_NAME"));
                })
                .reduce("", (a, b) -> a + "\n\t\t" + b);

        // now get Primary Key
        String PrimaryKey = columns
                .stream()
                .filter(column -> column.get("COLUMN_KEY").equals("PRI"))
                .map(column -> (String) column.get("COLUMN_NAME"))
                .findFirst().orElse(null);

        String PrimaryKeyType = Map.of(
                "int", "Integer",
                "varchar", "String",
                "date", "Date",
                "datetime", "Date",
                "timestamp", "Date",
                "tinyint", "Boolean",
                "double", "Double",
                "float", "Float",
                "null", "Object"
        ).get(columns
                .stream()
                .filter(column -> column.get("COLUMN_KEY").equals("PRI"))
                .map(column -> (String) column.get("DATA_TYPE"))
                .findFirst()
                .orElse("null"));

        // if primary key is null, then delete lines 20-22 from template
        if (PrimaryKey == null) {
            ArrayList<String> lines = new ArrayList<>(List.of(template.split("\n")));
            lines.remove(20);
            lines.remove(20);
            lines.remove(20);
            template = String.join("\n", lines);
        }

        if (PrimaryKeyType == null) {
            PrimaryKeyType = "Object";
        }

        ArrayList<String> usedNamesColumns = new ArrayList<>();
        usedNamesColumns.add("Data"); // getData is used by Model class

        String GettersSetters = columns
                .stream()
                .map(column -> {
                    String Column = camelCase((String) column.get("COLUMN_NAME"));
                    while (usedNamesColumns.contains(Column)) {
                        Column += "_";
                    }
                    usedNamesColumns.add(Column);
                    String Type = Map.of(
                            "int", "Integer",
                            "varchar", "String",
                            "date", "Date",
                            "datetime", "Date",
                            "timestamp", "Date",
                            "tinyint", "Boolean",
                            "double", "Double",
                            "float", "Float",
                            "null", "Object"
                    ).get(column.get("DATA_TYPE"));
                    if (Type == null) {
                        Type = "Object";
                    }
                    return String.format(
                            "\tpublic %s get%s() {\n" +
                            "\t\treturn (%s) super.get(\"%s\");\n" +
                            "\t}\n" +
                            "\tpublic void set%s(%s %s) {\n" +
                            "\t\tsuper.set(\"%s\", %s);\n" +
                            "\t}",
                            Type, Column, Type, column.get("COLUMN_NAME"),
                            Column, Type, Column, column.get("COLUMN_NAME"), Column
                    );
                })
                .reduce("", (a, b) -> a + "\n\n" + b);

        /**
         * Now in template replace all
         * %database% with database,
         * %ClassName% with ClassName,
         * %TableName% with TableName,
         * %ColumnNames% with ColumnNames,
         * %fromModel% with fromModel,
         * %PrimaryKey% with PrimaryKey,
         * %PrimaryKeyType% with PrimaryKeyType,
         * %GettersSetters% with GettersSetters
         */
        Map<String, String> map = new HashMap<>();
        map.put("%database%", database);
        map.put("%ClassName%", ClassName);
        map.put("%TableName%", TableName);
        map.put("%ColumnNames%", ColumnNames);
        map.put("%fromModel%", fromModel);
        if (PrimaryKey != null)
            map.put("%PrimaryKey%", PrimaryKey);
        if (PrimaryKeyType != null)
            map.put("%PrimaryKeyType%", PrimaryKeyType);
        map.put("%GettersSetters%", GettersSetters);

        for (Map.Entry<String, String> entry : map.entrySet()) {
            template = template.replace(entry.getKey(), entry.getValue());
        }

        // now write to file
        // create directory if not exists Path.of("src", "models", database
        File dir = new File(Path.of("src", "main", "java", "com", "example", "models", database).toString());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        FileWriter writer = new FileWriter(Path.of("src", "main", "java", "com", "example", "models", database, ClassName + ".java").toString());
        writer.write(template);
        writer.close();

        System.out.println("Done generating model for " + tableName + "");
    }

    public static void main(String[] args) {
        Model.setConnection(Connect.getConnection());
        var modelMaker = new ModelMaker();

        modelMaker.getTables("proyecto4_topicos").forEach(table -> {
            try {
                modelMaker.generateTableModel(table);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
