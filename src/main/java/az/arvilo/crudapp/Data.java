package az.arvilo.crudapp;

import java.util.HashMap;
import java.util.List;

public class Data {


    /**
     * A static field that stores all database tables in memory.
     * - The key represents the table name.
     * - The value is a list of rows, where:
     *   - The first row contains column names (must be unique within the table).
     *   - Subsequent rows contain table data.
     * - Table names and column names must not be empty ("").
     * - Null values are not allowed anywhere.
     * - Duplicate table names are not allowed.
     */
    public static final HashMap<String,List<List<String>>> TABLES = new HashMap<>();
}
