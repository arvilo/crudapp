package az.arvilo.crudapp.service;

import az.arvilo.crudapp.Data;
import az.arvilo.crudapp.exception.DataBaseCorruptException;
import az.arvilo.crudapp.exception.InvalidInputException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceTest {

    Service service;

    public ServiceTest() {
        service = new Service();
    }

    @BeforeEach
    void clearTables() {
        Data.TABLES.clear();
    }

    @Test
    void testGetTableNames() {
        //Any table does not exist.
        assertTrue(service.getTableNames().isEmpty());
        //Tables exist.
        Data.TABLES.put("Users", List.of(List.of("ID", "Name")));
        Data.TABLES.put("Orders", List.of(List.of("OrderID", "Amount")));
        assertEquals(2, service.getTableNames().size());
        assertTrue(service.getTableNames().contains("Users"));
        assertTrue(service.getTableNames().contains("Orders"));
    }

    @Test
    void testCreateTable() {
        // The table does not exist.
        String tableName = "Users";
        assertDoesNotThrow(() -> service.createTable(tableName));
        assertTrue(Data.TABLES.containsKey(tableName));
        clearTables();
        // With a null or invalid argument.
        assertThrows(NullPointerException.class, () -> service.createTable(null));
        assertThrows(InvalidInputException.class, () -> service.createTable(""));
        assertThrows(InvalidInputException.class, () -> service.createTable("   "));
        assertThrows(InvalidInputException.class, () -> service.createTable("Table    Name"));
        // The table already exists.
        try {
            service.createTable(tableName);
        } catch (InvalidInputException e) {
            throw new RuntimeException(e);
        }
        assertThrows(InvalidInputException.class, () -> service.createTable(tableName));
    }

    @Test
    void testDropTable() {
        // The table exists.
        String tableName = "Students";
        try {
            service.createTable(tableName);
        } catch (InvalidInputException e) {
            throw new RuntimeException(e);
        }
        assertDoesNotThrow(() -> service.dropTable(tableName));
        assertFalse(Data.TABLES.containsKey(tableName));
        // With a null argument.
        assertThrows(NullPointerException.class, () -> service.dropTable(null));
        //  The table does not exist.
        assertThrows(InvalidInputException.class, () -> service.dropTable("Persons"));
    }

    @Test
    void testRenderTable() {
        // The table is empty.
        String tableName = "Users";
        List<List<String>> table = new ArrayList<>();
        table.add(new ArrayList<>());
        Data.TABLES.put(tableName, table);
        try {
            assertEquals("Empty table.", service.renderTable(tableName, false));
            assertEquals("Empty table.", service.renderTable(tableName, true));
        } catch (DataBaseCorruptException | InvalidInputException e) {
            throw new RuntimeException(e);
        }
        // There is data inside the table.
        tableName = "Students";
        table = new ArrayList<>();
        table.add(new ArrayList<>());
        table.getFirst().add("ID");
        table.getFirst().add("Name");
        table.getFirst().add("Surname");
        table.add(new ArrayList<>());
        table.getLast().add("1");
        table.getLast().add("Name1");
        table.getLast().add("Surname1");
        table.add(new ArrayList<>());
        table.getLast().add("2");
        table.getLast().add("Name     2");
        table.getLast().add("Surname 2");
        Data.TABLES.put(tableName, table);
        String exceptedValueWithoutRuler = """
                =========================
                |ID|   Name   | Surname |
                =========================
                |1 |Name1     |Surname1 |
                |--+----------+---------|
                |2 |Name     2|Surname 2|
                =========================""";
        String exceptedValueWithRuler = """
                     =========================
                     |ID|   Name   | Surname |
                     =========================
                1 -> |1 |Name1     |Surname1 |
                     |--+----------+---------|
                2 -> |2 |Name     2|Surname 2|
                     =========================""";
        try {
            assertEquals(exceptedValueWithoutRuler, service.renderTable(tableName, false));
            assertEquals(exceptedValueWithRuler, service.renderTable(tableName, true));
        } catch (DataBaseCorruptException | InvalidInputException e) {
            throw new RuntimeException(e);
        }
        // The table is corrupted.
        String tableName2 = "Employees";
        Data.TABLES.put(tableName2, new ArrayList<>());
        table = Data.TABLES.get(tableName2);
        assertThrows(DataBaseCorruptException.class, () -> service.renderTable(tableName2, false));
        table.add(new ArrayList<>());
        table.add(new ArrayList<>());
        assertThrows(DataBaseCorruptException.class, () -> service.renderTable(tableName2, false));
        table.getFirst().add("Column1");
        table.getFirst().add("Column2");
        table.getLast().add("Value11");
        table.getLast().add("Value12");
        table.add(null);
        assertThrows(DataBaseCorruptException.class, () -> service.renderTable(tableName2, false));
        table.set(2, new ArrayList<>());
        table.getLast().add(null);
        table.getLast().add("Value22");
        assertThrows(DataBaseCorruptException.class, () -> service.renderTable(tableName2, false));
        table.get(2).set(0, "Value21");
        table.get(1).add("Value13");
        assertThrows(DataBaseCorruptException.class, () -> service.renderTable(tableName2, false));
        // The table does not exist.
        String tableName3 = "Cars";
        assertThrows(InvalidInputException.class, () -> service.renderTable(tableName3, false));
    }

    @Test
    void testAddNewRow() {
        // The table does not exist.
        String tableName = "Users";
        assertThrows(InvalidInputException.class, () -> service.addNewRow(tableName));
        // Structure of the table is not correct.
        Data.TABLES.put(tableName, new ArrayList<>());
        List<List<String>> table = Data.TABLES.get(tableName);
        assertThrows(DataBaseCorruptException.class, () -> service.addNewRow(tableName));
        table.add(new ArrayList<>());
        table.add(new ArrayList<>());
        assertThrows(DataBaseCorruptException.class, () -> service.addNewRow(tableName));
        table.getFirst().add("Column1");
        table.getFirst().add("Column2");
        table.getLast().add("Value11");
        table.getLast().add("Value12");
        table.add(null);
        assertThrows(DataBaseCorruptException.class, () -> service.addNewRow(tableName));
        table.set(2, new ArrayList<>());
        table.getLast().add(null);
        table.getLast().add("Value22");
        assertThrows(DataBaseCorruptException.class, () -> service.addNewRow(tableName));
        table.get(2).set(0, "Value21");
        table.get(1).add("Value13");
        assertThrows(DataBaseCorruptException.class, () -> service.addNewRow(tableName));
        // The table has no columns.
        clearTables();
        table = new ArrayList<>();
        table.add(new ArrayList<>());
        Data.TABLES.put(tableName, table);
        assertThrows(InvalidInputException.class, () -> service.addNewRow(tableName));
        // Structure of the table is correct.
        table = new ArrayList<>();
        table.add(new ArrayList<>());
        table.getFirst().add("ID");
        table.getFirst().add("Name");
        table.getFirst().add("Surname");
        table.add(new ArrayList<>());
        table.getLast().add("1");
        table.getLast().add("Name1");
        table.getLast().add("Surname1");
        table.add(new ArrayList<>());
        table.getLast().add("2");
        table.getLast().add("Name     2");
        table.getLast().add("Surname 2");
        Data.TABLES.put(tableName, table);
        assertDoesNotThrow(() -> service.addNewRow(tableName));
    }

    @Test
    void testRenderRow() {
        // The table is not exist.
        assertThrows(
                InvalidInputException.class,
                () -> service.renderRow("Users", "1")
        );
        // The is corrupted.
        List<String> header = new ArrayList<>();
        header.add("header1");
        List<String> row1 = new ArrayList<>();
        row1.add("value1");
        List<String> row2 = new ArrayList<>();
        row2.add(null);
        List<List<String>> table = new ArrayList<>();
        table.add(header);
        table.add(row1);
        table.add(row2);
        Data.TABLES.put("Users", table);
        assertThrows(DataBaseCorruptException.class,
                () -> service.renderRow("Users", "1")
        );
        // The row is not exist.
        Data.TABLES.get("Users").get(2).set(0, "value2");
        assertThrows(
                InvalidInputException.class,
                () -> service.renderRow("Users", "5")
        );
        assertThrows(
                InvalidInputException.class,
                () -> service.renderRow("Users", "0")
        );
        assertThrows(
                InvalidInputException.class,
                () -> service.renderRow("Users", "-5")
        );
        // Structure of table is correct. The row exists.
        String exceptedValue = """
                     =========
                     |header1|
                     |-------|
                2 -> |value2 |
                     =========""";
        try {
            assertEquals(exceptedValue, service.renderRow("Users", "2"));
        } catch (InvalidInputException | DataBaseCorruptException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testUpdateCell() {
        // The table does not exist.
        String tableName = "Users";
        assertThrows(InvalidInputException.class, () -> service.updateCell(tableName, "1", "Name", "NewName"));
        // Structure of the table is not correct.
        Data.TABLES.put(tableName, new ArrayList<>());
        assertThrows(DataBaseCorruptException.class, () -> service.updateCell(tableName, "1", "Name", "NewName"));
        // Create valid structure for the table.
        List<List<String>> table = Data.TABLES.get(tableName);
        table.add(new ArrayList<>());
        table.getFirst().add("ID");
        table.getFirst().add("Name");
        table.add(new ArrayList<>());
        table.get(1).add("1");
        table.get(1).add("OldName");
        // The row does not exist.
        assertThrows(InvalidInputException.class, () -> service.updateCell(tableName, "2", "Name", "NewName"));
        // The column does not exist.
        assertThrows(InvalidInputException.class, () -> service.updateCell(tableName, "1", "NonExistentColumn", "NewName"));
        // Successfully update a cell.
        assertDoesNotThrow(() -> service.updateCell(tableName, "1", "Name", "NewName"));
        assertEquals("NewName", table.get(1).get(1));
        // Ensure the original value is replaced.
        assertNotEquals("OldName", table.get(1).get(1));
    }

    @Test
    void testDeleteRow() {
        String tableName = "Users";
        // The table does not exist.
        assertThrows(InvalidInputException.class, () -> service.deleteRow(tableName, "1"));
        // The table is corrupted.
        Data.TABLES.put(tableName, new ArrayList<>());
        assertThrows(DataBaseCorruptException.class, () -> service.deleteRow(tableName, "1"));
        // Create valid structure for the table.
        List<List<String>> table = new ArrayList<>();
        table.add(new ArrayList<>());
        table.getFirst().add("ID");
        table.getFirst().add("Name");
        table.add(new ArrayList<>());
        table.get(1).add("1");
        table.get(1).add("Alice");
        table.add(new ArrayList<>());
        table.get(2).add("2");
        table.get(2).add("Bob");
        Data.TABLES.put(tableName, table);
        // Invalid row numbers.
        assertThrows(InvalidInputException.class, () -> service.deleteRow(tableName, "0"));
        assertThrows(InvalidInputException.class, () -> service.deleteRow(tableName, "-1"));
        assertThrows(InvalidInputException.class, () -> service.deleteRow(tableName, "10"));
        // Successfully delete a row.
        assertDoesNotThrow(() -> service.deleteRow(tableName, "1"));
        assertEquals(2, Data.TABLES.get(tableName).size());
        assertEquals("2", Data.TABLES.get(tableName).get(1).getFirst());
    }

    @Test
    void testAddNewColumn() {
        // The table does not exist.
        String tableName = "Users";
        assertThrows(InvalidInputException.class, () -> service.addNewColumn(tableName, "Age"));
        // The table is corrupted.
        Data.TABLES.put(tableName, new ArrayList<>());
        assertThrows(DataBaseCorruptException.class, () -> service.addNewColumn(tableName, "Age"));
        List<List<String>> table = new ArrayList<>();
        table.add(new ArrayList<>());
        table.getFirst().add("ID");
        table.getFirst().add("Name");
        table.add(new ArrayList<>());
        table.get(1).add("1");
        table.get(1).add("Alice");
        table.add(new ArrayList<>());
        table.get(2).add("2");
        table.get(2).add("Bob");
        Data.TABLES.put(tableName, table);
        // The column already exists.
        assertThrows(InvalidInputException.class, () -> service.addNewColumn(tableName, "Name"));
        // Invalid column names.
        assertThrows(InvalidInputException.class, () -> service.addNewColumn(tableName, ""));
        assertThrows(InvalidInputException.class, () -> service.addNewColumn(tableName, "   "));
        assertThrows(InvalidInputException.class, () -> service.addNewColumn(tableName, "Column  Name"));
        assertThrows(InvalidInputException.class, () -> service.addNewColumn(tableName, " Column"));
        assertThrows(InvalidInputException.class, () -> service.addNewColumn(tableName, "Column "));
        // Successfully add a new column.
        assertDoesNotThrow(() -> service.addNewColumn(tableName, "Age"));
        // Verify that the column has been added.
        assertTrue(Data.TABLES.get(tableName).getFirst().contains("Age"));
        // Ensure new column values are initialized to empty strings.
        Data
                .TABLES
                .get(tableName)
                .stream()
                .skip(1)
                .forEach(row -> assertEquals("", row.getLast()));
    }

    @Test
    void testDeleteColumn() {
        // The table does not exist.
        String tableName = "Users";
        assertThrows(InvalidInputException.class, () -> service.deleteColumn(tableName, "Age"));
        // The table is corrupted.
        Data.TABLES.put(tableName, new ArrayList<>());
        assertThrows(DataBaseCorruptException.class, () -> service.deleteColumn(tableName, "Age"));
        // Initialize a valid table structure.
        List<List<String>> table = new ArrayList<>();
        table.add(new ArrayList<>());
        table.getFirst().add("ID");
        table.getFirst().add("Name");
        table.getFirst().add("Age");
        table.add(new ArrayList<>());
        table.get(1).add("1");
        table.get(1).add("Alice");
        table.get(1).add("25");
        table.add(new ArrayList<>());
        table.get(2).add("2");
        table.get(2).add("Bob");
        table.get(2).add("30");
        Data.TABLES.put(tableName, table);
        // The column does not exist.
        assertThrows(InvalidInputException.class,
                () -> service.deleteColumn(tableName, "NonExistentColumn")
        );
        // Successfully delete an existing column.
        assertDoesNotThrow(() -> service.deleteColumn(tableName, "Age"));
        assertFalse(Data.TABLES.get(tableName).getFirst().contains("Age"));
        Data
                .TABLES
                .get(tableName)
                .forEach(row -> assertEquals(2, row.size()));
    }

}
