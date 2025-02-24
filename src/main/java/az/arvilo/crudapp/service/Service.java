package az.arvilo.crudapp.service;

import az.arvilo.crudapp.Data;
import az.arvilo.crudapp.exception.DataBaseCorruptException;
import az.arvilo.crudapp.exception.InvalidInputException;
import lombok.NonNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Service {

    /**
     * Returns a list of all table names stored in memory.
     */
    public List<String> getTableNames() {

        return new ArrayList<>(Data.TABLES.keySet());
    }

    /**
     * Creates a new table with the given name.
     * - The table name must not be null, empty, contain leading/trailing spaces,
     * or have consecutive spaces within the name.
     * - It must not already exist in the database.
     *
     * @param newTableName The name of the table to be created.
     * @throws InvalidInputException if the table name is invalid or already exists.
     */
    public void createTable(@NonNull String newTableName) throws InvalidInputException {
        if (isTableExist(newTableName)) {
            String errorMessage = String.format("%s already exist.", newTableName);
            throw new InvalidInputException(errorMessage);
        } else if (!isValidTableName(newTableName)) {
            String errorMessage = String.format("%s is not valid name.", newTableName);
            throw new InvalidInputException(errorMessage);
        } else {
            Data.TABLES.put(newTableName, new ArrayList<>(List.of(new ArrayList<>())));
        }
    }

    /**
     * Removes the specified table from the database.
     * - The table name must not be null and must exist in the database.
     *
     * @param tableName The name of the table to be removed.
     * @throws InvalidInputException if the table name does not exist in the database.
     */
    public void dropTable(@NonNull String tableName)
            throws InvalidInputException {
        if (!isTableExist(tableName)) {
            String errorMessage = String.format("%s is not exist.", tableName);
            throw new InvalidInputException(errorMessage);
        } else {
            Data.TABLES.remove(tableName);
        }
    }

    /**
     * This method retrieves the table data and formats it into a human-readable
     * string representation.
     *
     * @param tableName The name of the table to be rendered.
     * @return A formatted string representation of the table.
     * @throws InvalidInputException    if the table does not exist.
     * @throws DataBaseCorruptException if the table is corrupted.
     */
    public String renderTable(@NonNull String tableName)
            throws InvalidInputException, DataBaseCorruptException {
        if (!isTableExist(tableName)) {
            String errorMessage = String.format("%s is not exist", tableName);
            throw new InvalidInputException(errorMessage);
        }
        if (!isTableValid(tableName)) {
            String errorMessage = String.format("%s table is corrupted.", tableName);
            throw new DataBaseCorruptException(errorMessage);
        } else {
            if (Data.TABLES.get(tableName).getFirst().isEmpty()) {

                return "Empty table.";
            } else if (Data.TABLES.get(tableName).size() == 1) {
                StringBuilder tableText = new StringBuilder();
                appendHeavyLine(tableText, tableName);
                appendRowToText(tableText, tableName, 0);
                appendHeavyLine(tableText, tableName);
                tableText.deleteCharAt(tableText.length() - 1);

                return tableText.toString();
            } else {
                StringBuilder tableText = new StringBuilder();
                appendHeavyLine(tableText, tableName);
                appendRowToText(tableText, tableName, 0);
                appendHeavyLine(tableText, tableName);
                appendRowToText(tableText, tableName, 1);
                IntStream
                        .range(2, Data.TABLES.get(tableName).size())
                        .forEach(i -> {
                            appendLine(tableText, tableName);
                            appendRowToText(tableText, tableName, i);
                        });
                appendHeavyLine(tableText, tableName);
                tableText.deleteCharAt(tableText.length() - 1);

                return tableText.toString();
            }
        }
    }

    /**
     * Adds a new row to the specified table in the database.
     *
     * @param tableName The name of the table to which a new row will be added.
     * @throws InvalidInputException    if the table does not exist, is corrupted, or has no columns.
     * @throws DataBaseCorruptException if the table is corrupted.
     */
    public void addNewRow(@NonNull String tableName)
            throws InvalidInputException, DataBaseCorruptException {
        if (!isTableExist(tableName)) {
            String errorMessage = String.format("%s is not exist", tableName);
            throw new InvalidInputException(errorMessage);
        }
        if (!isTableValid(tableName)) {
            String errorMessage = String.format("%s table is corrupted.", tableName);
            throw new DataBaseCorruptException(errorMessage);
        } else if (Data.TABLES.get(tableName).getFirst().isEmpty()) {
            String errorMessage = String.format("%s table has no columns.", tableName);
            throw new InvalidInputException(errorMessage);
        } else {
            List<List<String>> table = Data.TABLES.get(tableName);
            table.add(
                    IntStream
                            .range(0, table.getFirst().size())
                            .mapToObj(i -> "")
                            .toList()
            );
        }
    }

    private void appendRowToText(@NonNull StringBuilder text,
                                 @NonNull String tableName,
                                 int number) {
        List<String> row = Data.TABLES.get(tableName).get(number);
        List<Integer> lengths = getColumnLengths(tableName);
        boolean header = number == 0;
        text.append("|");
        IntStream
                .range(0, lengths.size())
                .forEach(i -> {
                    text.append(header
                            ? getColumnText(row.get(i), lengths.get(i))
                            : getCellText(row.get(i), lengths.get(i)));
                    text.append("|");
                });
        text.append("\n");
    }

    private void appendLine(@NonNull StringBuilder text,
                            @NonNull String tableName) {
        List<Integer> lengths = getColumnLengths(tableName);
        text.append("|");
        lengths
                .forEach(length -> {
                            IntStream.range(0, length)
                                    .forEach(i -> text.append("-"));
                            text.append("+");
                        }
                );
        text.deleteCharAt(text.length() - 1);
        text.append("|\n");
    }

    private void appendHeavyLine(@NonNull StringBuilder text,
                                 @NonNull String tableName) {
        int length = getTableCharLength(tableName);
        IntStream
                .range(0, length)
                .forEach(i -> text.append("="));
        text.append("\n");
    }

    private int getTableCharLength(@NonNull String tableName) {
        List<Integer> columnLength = getColumnLengths(tableName);

        return columnLength
                .stream()
                .reduce(0, Integer::sum) + columnLength.size() + 1;
    }

    private String getColumnText(@NonNull String value, int length) {
        int allSpaces = length - value.length();
        int beforeSpaces;
        int afterSpaces;
        if (allSpaces % 2 == 0) {
            beforeSpaces = allSpaces / 2;
            afterSpaces = allSpaces / 2;
        } else {
            beforeSpaces = (allSpaces - 1) / 2;
            afterSpaces = (allSpaces + 1) / 2;
        }
        StringBuilder text = new StringBuilder();
        IntStream.range(0, beforeSpaces).forEach(i -> text.append(" "));
        text.append(value);
        IntStream.range(0, afterSpaces).forEach(i -> text.append(" "));

        return text.toString();
    }

    private String getCellText(@NonNull String value, int length) {
        StringBuilder text = new StringBuilder(value);
        IntStream.range(value.length(), length).forEach(i -> text.append(" "));

        return text.toString();
    }

    private List<Integer> getColumnLengths(@NonNull String tableName) {
        List<List<String>> table = Data.TABLES.get(tableName);

        return IntStream.range(0, table.getFirst().size())
                .map(i ->
                        table
                                .stream()
                                .max(Comparator.comparingInt(row ->
                                        row.get(i).length()))
                                .orElseThrow()
                                .get(i)
                                .length()
                )
                .boxed()
                .collect(Collectors.toList());
    }

    private boolean isTableValid(@NonNull String tableName) {
        List<List<String>> table = Data.TABLES.get(tableName);

        if (table.isEmpty()) {

            return false;
        }

        if (table.stream().anyMatch(Objects::isNull)) {

            return false;
        }

        if (
                table
                        .stream()
                        .anyMatch(row ->
                                row.stream().anyMatch(Objects::isNull))
        ) {

            return false;
        }

        if (table.getFirst().isEmpty() && table.size() > 1) {

            return false;
        }

        return table
                .stream()
                .skip(1)
                .allMatch(row ->
                        row.size() == table.getFirst().size());
    }

    private boolean isValidTableName(String tableName) {

        return tableName != null
                && !tableName.isBlank()
                && tableName.trim().replaceAll("\\s+", " ").equals(tableName);
    }

    private boolean isTableExist(String tableName) {

        return Data.TABLES.containsKey(tableName);
    }

}
