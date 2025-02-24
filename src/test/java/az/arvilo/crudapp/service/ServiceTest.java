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
    void testGetTableNamesWhenEmpty() {
        List<String> tableNames = service.getTableNames();
        assertTrue(tableNames.isEmpty(), "Table list should be empty");
    }

    @Test
    void testGetTableNamesWithData() {
        Data.TABLES.put("Users", List.of(List.of("ID", "Name")));
        Data.TABLES.put("Orders", List.of(List.of("OrderID", "Amount")));

        List<String> tableNames = service.getTableNames();

        assertEquals(2, tableNames.size(), "Table count should be 2");
        assertTrue(tableNames.contains("Users"), "Table list should contain 'Users'");
        assertTrue(tableNames.contains("Orders"), "Table list should contain 'Orders'");
    }

    @Test
    void testCreateTableWithValidName() {
        String tableName = "Users";
        assertDoesNotThrow(() -> service.createTable(tableName));
        assertTrue(Data.TABLES.containsKey(tableName));
    }

    @Test
    void testCreateTableWithNullName() {
        assertThrows(NullPointerException.class, () -> service.createTable(null));
    }

    @Test
    void testCreateTableWithEmptyName() {
        assertThrows(InvalidInputException.class, () -> service.createTable(""));
    }

    @Test
    void testCreateTableWithBlankName() {
        assertThrows(InvalidInputException.class, () -> service.createTable("   "));
    }

    @Test
    void testCreateTableWithConsecutiveSpaces() {
        assertThrows(InvalidInputException.class, () -> service.createTable("Table    Name"));
    }

    @Test
    void testCreateTableAlreadyExists() {
        String tableName = "Orders";
        try {
            service.createTable(tableName);
        } catch (InvalidInputException e) {
            throw new RuntimeException(e);
        }
        assertThrows(InvalidInputException.class, () -> service.createTable(tableName));
    }

    @Test
    void testDropTableWithExistName() {
        String tableName = "Students";
        try {
            service.createTable(tableName);
        } catch (InvalidInputException e) {
            throw new RuntimeException(e);
        }
        assertDoesNotThrow(() -> service.dropTable(tableName));
        assertFalse(Data.TABLES.containsKey(tableName));
    }

    @Test
    void testDropTableWithNullName() {
        assertThrows(NullPointerException.class, () -> service.dropTable(null));
    }

    @Test
    void testDropTableWithNotExistName() {
        assertThrows(InvalidInputException.class, () -> service.dropTable("Persons"));
    }

    @Test
    void testRenderTableWhenTableEmpty() {
        String tableName = "Users";
        List<List<String>> table = new ArrayList<>();
        table.add(new ArrayList<>());
        Data.TABLES.put(tableName, table);
        try {
            assertEquals("Empty table.", service.renderTable(tableName));
        } catch (DataBaseCorruptException | InvalidInputException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testRenderTableWhenExistData() {
        String tableName = "Users";
        List<List<String>> table = new ArrayList<>();
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
        String exceptedValue = """
                =========================
                |ID|   Name   | Surname |
                =========================
                |1 |Name1     |Surname1 |
                |--+----------+---------|
                |2 |Name     2|Surname 2|
                =========================""";
        try {
            assertEquals(exceptedValue, service.renderTable(tableName));
        } catch (DataBaseCorruptException | InvalidInputException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testRenderWhenCorruptedData() {
        String tableName = "Users";
        Data.TABLES.put(tableName, new ArrayList<>());
        List<List<String>> table = Data.TABLES.get(tableName);

        assertThrows(DataBaseCorruptException.class, () -> service.renderTable(tableName));

        table.add(new ArrayList<>());
        table.add(new ArrayList<>());
        assertThrows(DataBaseCorruptException.class, () -> service.renderTable(tableName));

        table.getFirst().add("Column1");
        table.getFirst().add("Column2");
        table.getLast().add("Value11");
        table.getLast().add("Value12");
        table.add(null);
        assertThrows(DataBaseCorruptException.class, () -> service.renderTable(tableName));

        table.set(2, new ArrayList<>());
        table.getLast().add(null);
        table.getLast().add("Value22");
        assertThrows(DataBaseCorruptException.class, () -> service.renderTable(tableName));

        table.get(2).set(0, "Value21");
        table.get(1).add("Value13");
        assertThrows(DataBaseCorruptException.class, () -> service.renderTable(tableName));
    }

    @Test
    void testRenderWhenTableIsNotExist() {
        String tableName = "Users";
        assertThrows(InvalidInputException.class, () -> service.renderTable(tableName));
    }

    @Test
    void testAddNewRowWhenTableIsNotExist() {
        String tableName = "Users";
        assertThrows(InvalidInputException.class, () -> service.addNewRow(tableName));
    }

    @Test
    void testAddNewRowWhenCorruptedData() {
        String tableName = "Users";
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
    }

    @Test
    void testAddNewRowWhenTableEmpty() {
        String tableName = "Users";
        List<List<String>> table = new ArrayList<>();
        table.add(new ArrayList<>());
        Data.TABLES.put(tableName, table);
        assertThrows(InvalidInputException.class, () -> service.addNewRow(tableName));
    }

    @Test
    void testAddNewRowWhenExistData() {
        String tableName = "Users";
        List<List<String>> table = new ArrayList<>();
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

}
