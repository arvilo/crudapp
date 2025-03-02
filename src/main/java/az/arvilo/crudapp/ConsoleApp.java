package az.arvilo.crudapp;

import az.arvilo.crudapp.exception.DataBaseCorruptException;
import az.arvilo.crudapp.exception.InvalidInputException;
import az.arvilo.crudapp.service.Service;
import lombok.NonNull;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.stream.IntStream;

public class ConsoleApp implements AutoCloseable {

    private final Service service;
    private final Scanner scanner;

    public ConsoleApp(Service service) {
        scanner = new Scanner(System.in);
        this.service = service;
    }

    public String getInput(String menu) {
        clearConsole();
        System.out.print(menu);
        return scanner.nextLine();
    }

    public void showWelcomeMessage(int seconds) {
        IntStream
                .range(0, seconds)
                .map(i -> seconds - i)
                .forEach(i -> {
                    try {
                        clearConsole();
                        String message = String.format("""
                                        Welcome to the Crud app.
                                        Enter 0 to exit from any menu.
                                        The app will start in %d %s.""",
                                i,
                                i == 1 ? "second" : "seconds");
                        System.out.print(message);
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public void showInvalidInputAlert(int seconds) {
        showAlert(seconds, "Invalid input, please enter again.");
    }

    public void showAlert(int seconds, @NonNull String message) {
        try {
            clearConsole();
            System.out.print(message);
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void homeMenu() {
        clearConsole();
        String input = getInput("""
                1)Select table
                2)New table
                3)Drop table
                Enter your choice:\s""");
        input = input.trim();
        switch (input) {
            case "1":
                tableListMenu();
                break;
            case "2":
                newTableMenu();
                break;
            case "3":
                dropTableMenu();
                break;
            case "0":
                clearConsole();
                break;
            default:
                showInvalidInputAlert(2);
                homeMenu();
        }
    }

    public void tableListMenu() {
        StringBuilder message = new StringBuilder();
        List<String> tableNames = service.getTableNames();
        if (tableNames.isEmpty()) {
            showAlert(2, "No table exists. Please create a table.");
            homeMenu();
            return;
        }
        IntStream
                .range(0, tableNames.size())
                .forEach(i ->
                        message.append(
                                String.format(
                                        "%d)%s\n",
                                        i + 1,
                                        tableNames.get(i)
                                )
                        ));
        message.append("Enter your choice: ");
        String input = getInput(message.toString());
        input = input.trim();
        int index;
        try {
            index = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            showInvalidInputAlert(3);
            tableListMenu();
            return;
        }
        if (index == 0) {
            homeMenu();
            return;
        }
        try {
            tableMenu(tableNames.get(index - 1));
        } catch (IndexOutOfBoundsException e) {
            showInvalidInputAlert(3);
            tableListMenu();
        }
    }

    public void newTableMenu() {
        clearConsole();
        String input = getInput("Enter name for new table: ");
        input = input.trim();
        input = input.replaceAll("\\s+", " ");
        if (input.equals("0")) {
            homeMenu();
            return;
        }
        try {
            service.createTable(input);
            showAlert(
                    3,
                    String.format(
                            "The %s table was created successfully.",
                            input
                    )
            );
            homeMenu();
        } catch (InvalidInputException e) {
            if (service.getTableNames().contains(input)) {
                showAlert(4,
                        String.format("""
                                        The table named %s already exists.
                                        Please choose another name.""",
                                input
                        )
                );
            } else {
                showInvalidInputAlert(4);
            }
            newTableMenu();
        }
    }

    public void dropTableMenu() {
        clearConsole();
        List<String> tableNames = service.getTableNames();
        if (tableNames.isEmpty()) {
            showAlert(3, "No table exists.");
            homeMenu();
            return;
        }
        StringBuilder message = new StringBuilder();
        IntStream
                .range(0, tableNames.size())
                .forEach(i ->
                        message.append(
                                String.format(
                                        "%d)%s\n",
                                        i + 1,
                                        tableNames.get(i)
                                )
                        ));
        message.append("Choose a table: ");
        String input = getInput(message.toString());
        try {
            int index = Integer.parseInt(input);
            if (index == 0) {
                homeMenu();
                return;
            }
            service.dropTable(tableNames.get(index - 1));
            showAlert(
                    3,
                    String.format(
                            "The %s table was dropped successfully.",
                            tableNames.get(index - 1)
                    )
            );
            homeMenu();
        } catch (
                NumberFormatException |
                IndexOutOfBoundsException e
        ) {
            showInvalidInputAlert(3);
            dropTableMenu();
        } catch (InvalidInputException e) {
            throw new RuntimeException(e);
        }
    }

    public void tableMenu(@NonNull String tableName) {
        clearConsole();
        StringBuilder message = new StringBuilder();
        try {
            message.append(String.format("Table: %s\n", tableName));
            message.append(service.renderTable(tableName, false));
        } catch (InvalidInputException e) {
            throw new RuntimeException(e);
        } catch (DataBaseCorruptException e) {
            showAlert(
                    5,
                    "The database is corrupted. Please fix it and restart the app."
            );
            return;
        }
        message.append("""
                \n1)Update cell
                2)Add new row
                3)Delete row
                4)Add new column
                5)Delete column
                Enter your choice:\s""");
        String input = getInput(message.toString());
        input = input.trim();

        switch (input) {
            case "1":
                updateCellMenu(tableName);
                break;
            case "2":
                try {
                    service.addNewRow(tableName);
                    tableMenu(tableName);
                    break;
                } catch (DataBaseCorruptException e) {
                    showAlert(
                            5,
                            "The database is corrupted. Please fix it and restart the app."
                    );
                    break;
                } catch (InvalidInputException e) {
                    showAlert(3,
                            "Please,Add column to front, try it again."
                    );
                    tableMenu(tableName);
                    break;
                }
            case "3":
                deleteRowMenu(tableName);
                break;
            case "4":
                addNewColumnMenu(tableName);
                break;
            case "5":
                deleteColumnMenu(tableName);
                break;
            case "0":
                tableListMenu();
                break;
            default:
                showInvalidInputAlert(3);
                tableMenu(tableName);
                break;
        }
    }

    public void updateCellMenu(@NonNull String tableName) {
        if (service.hasNoRows(tableName)) {
            showAlert(
                    3,
                    "Doesn't exist any cell."
            );
            tableMenu(tableName);
            return;
        }
        clearConsole();
        StringBuilder message = new StringBuilder();
        try {
            message.append(String.format("Table: %s\n", tableName));
            message.append(service.renderTable(tableName, true));
        } catch (InvalidInputException e) {
            throw new RuntimeException(e);
        } catch (DataBaseCorruptException e) {
            showAlert(
                    5,
                    "The database is corrupted. Please fix it and restart the app."
            );
            return;
        }
        message.append("\nChoose a row: ");
        String rowNumber = getInput(message.toString());
        rowNumber = rowNumber.trim();
        rowNumber = rowNumber.replaceAll("\\s+", " ");
        if (rowNumber.equals("0")) {
            tableMenu(tableName);
            return;
        }
        try {
            message.append(String.format("Table: %s\n", tableName));
            message = new StringBuilder(service.renderRow(tableName, rowNumber));
        } catch (DataBaseCorruptException e) {
            showAlert(
                    5,
                    "The database is corrupted. Please fix it and restart the app."
            );
            return;
        } catch (InvalidInputException | NumberFormatException e) {
            showInvalidInputAlert(3);
            updateCellMenu(tableName);
        }
        message.append("\nEnter name of column: ");
        clearConsole();
        String columnName = getInput(message.toString());
        columnName = columnName.trim();
        columnName = columnName.replaceAll("\\s+", " ");
        if (!service.doesColumnOfTableExist(columnName, tableName)) {
            showAlert(
                    3,
                    String.format("%s column doesn't exist.", columnName)
            );
            updateCellMenu(tableName);
            return;
        }
        clearConsole();
        String newValue = getInput(
                String.format(
                        "Enter new value(row: %s,column: %s): ",
                        rowNumber,
                        columnName
                )
        );
        clearConsole();
        String confirm = getInput(
                String.format("""
                                Table: %s
                                Row: %s
                                Column: %s
                                New value: %s
                                Do you want to confirm(Y/N)?(default: Y):\s""",
                        tableName,
                        rowNumber,
                        columnName,
                        newValue
                )
        );
        confirm = confirm.replaceAll(" ", "");
        confirm = confirm.toLowerCase();
        switch (confirm) {
            case "n":
            case "no":
            case "not":
                updateCellMenu(tableName);
                return;
        }
        try {
            service.updateCell(tableName, rowNumber, columnName, newValue);
        } catch (InvalidInputException e) {
            showInvalidInputAlert(3);
            updateCellMenu(tableName);
            return;
        } catch (DataBaseCorruptException e) {
            showAlert(
                    5,
                    "The database is corrupted. Please fix it and restart the app."
            );
            return;
        }
        tableMenu(tableName);
    }

    public void deleteRowMenu(@NonNull String tableName) {
        if (service.hasNoRows(tableName)) {
            showAlert(
                    3,
                    "Doesn't exist any row."
            );
            tableMenu(tableName);
            return;
        }
        StringBuilder message = new StringBuilder();
        try {
            message.append(String.format("Table: %s\n", tableName));
            message.append(service.renderTable(tableName, true));
        } catch (InvalidInputException e) {
            throw new RuntimeException(e);
        } catch (DataBaseCorruptException e) {
            showAlert(
                    5,
                    "The database is corrupted. Please fix it and restart the app."
            );
            return;
        }
        message.append("\nEnter row number: ");
        String rowNumber = getInput(message.toString());
        rowNumber = rowNumber.trim();
        rowNumber = rowNumber.replaceAll("\\s+", " ");
        if (rowNumber.equals("0")) {
            tableMenu(tableName);
            return;
        }
        try {
            service.deleteRow(tableName, rowNumber);
        } catch (InvalidInputException | NumberFormatException e) {
            showInvalidInputAlert(3);
            deleteRowMenu(tableName);
        } catch (DataBaseCorruptException e) {
            showAlert(
                    5,
                    "The database is corrupted. Please fix it and restart the app."
            );
        }
    }

    public void addNewColumnMenu(@NonNull String tableName) {
        StringBuilder message = new StringBuilder();
        try {
            message.append(String.format("Table: %s\n", tableName));
            message.append(service.renderTable(tableName, false));
        } catch (DataBaseCorruptException e) {
            showAlert(
                    5,
                    "The database is corrupted. Please fix it and restart the app."
            );
        } catch (InvalidInputException e) {
            throw new RuntimeException(e);
        }
        message.append("\nEnter new column name: ");
        String input = getInput(message.toString());
        input = input.trim();
        input = input.replaceAll("\\s+", " ");
        if (input.isEmpty()) {
            showAlert(
                    4, """
                            Column name can't be empty string.
                            Please enter another name."""
            );
            addNewColumnMenu(tableName);
            return;
        }
        if (service.doesColumnOfTableExist(input, tableName)) {
            showAlert(
                    3,
                    String.format("%s column exists.", input)
            );
            tableMenu(tableName);
            return;
        }
        clearConsole();
        String confirm = getInput(
                String.format("""
                                Table: %s
                                New column: %s
                                Do you want to confirm(Y/N)?(default: Y):\s""",
                        tableName,
                        input
                )
        );
        confirm = confirm.replaceAll(" ", "");
        confirm = confirm.toLowerCase();
        switch (confirm) {
            case "n":
            case "no":
            case "not":
                tableMenu(tableName);
                return;
        }
        if (service.doesColumnOfTableExist(input, tableName)) {
            showAlert(3,
                    String.format("%s column is already exist.", input)
            );
        }
        try {
            service.addNewColumn(tableName, input);
            tableMenu(tableName);
        } catch (InvalidInputException e) {
            throw new RuntimeException(e);
        } catch (DataBaseCorruptException e) {
            showAlert(
                    5,
                    "The database is corrupted. Please fix it and restart the app."
            );
        }
    }

    public void deleteColumnMenu(@NonNull String tableName) {
        if (service.isTableEmpty(tableName)) {
            showAlert(
                    3,
                    "Doesn't exist any column."
            );
            tableMenu(tableName);
            return;
        }
        StringBuilder message = new StringBuilder();
        try {
            message.append(String.format("Table: %s\n", tableName));
            message.append(service.renderTable(tableName, false));
        } catch (DataBaseCorruptException e) {
            showAlert(
                    5,
                    "The database is corrupted. Please fix it and restart the app."
            );
        } catch (InvalidInputException e) {
            throw new RuntimeException(e);
        }
        message.append("\nEnter column name to remove: ");
        String input = getInput(message.toString());
        input = input.trim();
        input = input.replaceAll("\\s+", " ");
        if (!service.doesColumnOfTableExist(input, tableName)) {
            showAlert(3,
                    String.format("%s column doesn't exist.", input)
            );
            tableMenu(tableName);
        }
        clearConsole();
        String confirm = getInput(
                String.format("""
                                Table: %s
                                Column: %s
                                Do you want to confirm(Y/N)?(default: Y):\s""",
                        tableName,
                        input
                )
        );
        confirm = confirm.replaceAll(" ", "");
        confirm = confirm.toLowerCase();
        switch (confirm) {
            case "n":
            case "no":
            case "not":
                tableMenu(tableName);
                return;
        }
        try {
            service.deleteColumn(tableName, input);
            tableMenu(tableName);
        } catch (InvalidInputException e) {
            showAlert(
                    3,
                    String.format("%s column doesn't exist.", input)
            );
            deleteColumnMenu(tableName);
        } catch (DataBaseCorruptException e) {
            showAlert(
                    5,
                    "The database is corrupted. Please fix it and restart the app."
            );
        }
    }

    public void clearConsole() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void run() {
        showWelcomeMessage(4);
        homeMenu();
    }

    @Override
    public void close() {
        scanner.close();
    }

}
