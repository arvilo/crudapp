package az.arvilo.crudapp.service;

import az.arvilo.crudapp.Data;
import az.arvilo.crudapp.exception.DataBaseCorruptException;
import az.arvilo.crudapp.exception.InvalidInputException;
import lombok.NonNull;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Service {

    public List<String> getTableNames() {

        return new ArrayList<>(Data.TABLES.keySet());
    }

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

    public void dropTable(@NonNull String tableName)
            throws InvalidInputException {
        if (!isTableExist(tableName)) {
            String errorMessage = String.format("%s is not exist.", tableName);
            throw new InvalidInputException(errorMessage);
        } else {
            Data.TABLES.remove(tableName);
        }
    }

    public String renderTable(@NonNull String tableName, boolean verticalRuler)
            throws InvalidInputException, DataBaseCorruptException {
        if (!isTableExist(tableName)) {
            String errorMessage = String.format("%s is not exist.", tableName);
            throw new InvalidInputException(errorMessage);
        }
        if (isTableInvalid(tableName)) {
            String errorMessage = String.format("%s table is corrupted.", tableName);
            throw new DataBaseCorruptException(errorMessage);
        } else {
            if (Data.TABLES.get(tableName).getFirst().isEmpty()) {

                return "Empty table.";
            } else if (Data.TABLES.get(tableName).size() == 1) {
                StringBuilder tableText = new StringBuilder();
                appendHeavyLine(tableText, tableName, verticalRuler);
                appendRowToText(tableText, tableName, 0, verticalRuler);
                appendHeavyLine(tableText, tableName, verticalRuler);
                tableText.deleteCharAt(tableText.length() - 1);

                return tableText.toString();
            } else {
                StringBuilder tableText = new StringBuilder();
                appendHeavyLine(tableText, tableName, verticalRuler);
                appendRowToText(tableText, tableName, 0, verticalRuler);
                appendHeavyLine(tableText, tableName, verticalRuler);
                appendRowToText(tableText, tableName, 1, verticalRuler);
                IntStream
                        .range(2, Data.TABLES.get(tableName).size())
                        .forEach(i -> {
                            appendLine(tableText, tableName, verticalRuler);
                            appendRowToText(tableText, tableName, i, verticalRuler);
                        });
                appendHeavyLine(tableText, tableName, verticalRuler);
                tableText.deleteCharAt(tableText.length() - 1);

                return tableText.toString();
            }
        }
    }

    public void addNewRow(@NonNull String tableName)
            throws InvalidInputException, DataBaseCorruptException {
        if (!isTableExist(tableName)) {
            String errorMessage = String.format("%s is not exist.", tableName);
            throw new InvalidInputException(errorMessage);
        }
        if (isTableInvalid(tableName)) {
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
                            .collect(Collectors.toList())
            );
        }
    }

    public String renderRow(@NonNull String tableName, @NonNull String rowNumber)
            throws InvalidInputException, DataBaseCorruptException {
        int rowNumberInt = Integer.parseInt(rowNumber);
        if (!isTableExist(tableName)) {
            String errorMessage = String.format("%s is not exist.", tableName);
            throw new InvalidInputException(errorMessage);
        }
        if (isTableInvalid(tableName)) {
            String errorMessage = String.format("%s table is corrupted.", tableName);
            throw new DataBaseCorruptException(errorMessage);
        } else if (rowNumberInt < 1 || rowNumberInt >= Data.TABLES.get(tableName).size()) {
            String errorMessage = String.format("Row number couldn't be %d", rowNumberInt);
            throw new InvalidInputException(errorMessage);
        } else {
            StringBuilder text = new StringBuilder();
            appendHeavyLine(text, tableName, true);
            appendRowToText(text, tableName, 0, true);
            appendLine(text, tableName, true);
            appendRowToText(text, tableName, rowNumberInt, true);
            appendHeavyLine(text, tableName, true);
            text.deleteCharAt(text.length() - 1);

            return text.toString();
        }
    }

    public void updateCell(@NonNull String tableName,
                           @NonNull String rowNumber,
                           @NonNull String columnName,
                           @NonNull String newValue)
            throws InvalidInputException, DataBaseCorruptException {
        Predicate<String> isValueValid = (inputValue) ->
                !inputValue.startsWith(" ") &&
                        !inputValue.endsWith(" ") &&
                        !inputValue.contains("  ");
        if (!isTableExist(tableName)) {
            String errorMessage = String.format("%s table is not exist", tableName);
            throw new InvalidInputException(errorMessage);
        } else if (isTableInvalid(tableName)) {
            String errorMessage = String.format("%s table is corrupted.", tableName);
            throw new DataBaseCorruptException(errorMessage);
        } else if (
                Integer.parseInt(rowNumber) < 1 ||
                        Integer.parseInt(rowNumber) >= Data.TABLES.get(tableName).size()
        ) {
            String errorMessage = "Row does not exist.";
            throw new InvalidInputException(errorMessage);
        } else if (!Data.TABLES.get(tableName).getFirst().contains(columnName)) {
            String errorMessage = String.format(
                    "%s column does not exist in %s.",
                    columnName,
                    tableName
            );
            throw new InvalidInputException(errorMessage);
        } else if (!isValueValid.test(newValue)) {
            String errorMessage = "Invalid input: The value does not meet the required format.";
            throw new InvalidInputException(errorMessage);
        } else {
            Data.TABLES.get(tableName)
                    .get(Integer.parseInt(rowNumber))
                    .set(
                            Data.TABLES.get(tableName).getFirst().indexOf(columnName)
                            , newValue
                    );
        }
    }

    public void deleteRow(@NonNull String tableName,
                          @NonNull String rowNumber)
            throws InvalidInputException, DataBaseCorruptException {
        if (!isTableExist(tableName)) {
            String errorMessage = String.format("%s table is not exist", tableName);
            throw new InvalidInputException(errorMessage);
        } else if (isTableInvalid(tableName)) {
            String errorMessage = String.format("%s table is corrupted.", tableName);
            throw new DataBaseCorruptException(errorMessage);
        } else if (
                Integer.parseInt(rowNumber) < 1 ||
                        Integer.parseInt(rowNumber) >= Data.TABLES.get(tableName).size()
        ) {
            String errorMessage = String.format(
                    "Row %s does not exist in %s table.",
                    rowNumber,
                    tableName);
            throw new InvalidInputException(errorMessage);
        } else {
            Data.TABLES.get(tableName).remove(Integer.parseInt(rowNumber));
        }
    }

    public void addNewColumn(@NonNull String tableName,
                             @NonNull String newColumnName)
            throws InvalidInputException, DataBaseCorruptException {
        Predicate<String> isValueValid = (inputValue) ->
                !inputValue.isBlank() &&
                        !inputValue.startsWith(" ") &&
                        !inputValue.endsWith(" ") &&
                        !inputValue.contains("  ");
        if (!isTableExist(tableName)) {
            String errorMessage = String.format("%s table is not exist", tableName);
            throw new InvalidInputException(errorMessage);
        } else if (isTableInvalid(tableName)) {
            String errorMessage = String.format("%s table is corrupted.", tableName);
            throw new DataBaseCorruptException(errorMessage);
        } else if (Data.TABLES.get(tableName).getFirst().contains(newColumnName)) {
            String errorMessage = String.format(
                    "%s column of %s table already exists.",
                    newColumnName,
                    tableName);
            throw new InvalidInputException(errorMessage);
        } else if (!isValueValid.test(newColumnName)) {
            String errorMessage = "Invalid input: The value does not meet the required format.";
            throw new InvalidInputException(errorMessage);
        } else {
            Data.TABLES.get(tableName).getFirst().add(newColumnName);
            Data
                    .TABLES
                    .get(tableName)
                    .stream()
                    .skip(1)
                    .forEach(row -> row.add(""));
        }
    }

    public void deleteColumn(@NonNull String tableName,
                             @NonNull String columnName)
            throws InvalidInputException, DataBaseCorruptException {
        if (!isTableExist(tableName)) {
            String errorMessage = String.format("%s table is not exist", tableName);
            throw new InvalidInputException(errorMessage);
        } else if (isTableInvalid(tableName)) {
            String errorMessage = String.format("%s table is corrupted.", tableName);
            throw new DataBaseCorruptException(errorMessage);
        } else if (!Data.TABLES.get(tableName).getFirst().contains(columnName)) {
            String errorMessage = String.format(
                    "%s column of %s table does not exist.",
                    columnName,
                    tableName);
            throw new InvalidInputException(errorMessage);
        } else {
            int indexOfColumn = Data.TABLES.get(tableName).getFirst().indexOf(columnName);
            Data
                    .TABLES
                    .get(tableName)
                    .forEach(row -> row.remove(indexOfColumn));
        }
    }

    public boolean doesColumnOfTableExist(@NonNull String columnName,
                                          @NonNull String tableName) {

        return Data.TABLES.containsKey(tableName) &&
                Data.TABLES.get(tableName).getFirst().contains(columnName);
    }

    public boolean isTableEmpty(@NonNull String tableName) {

        return !Data.TABLES.containsKey(tableName) ||
                Data.TABLES.get(tableName).getFirst().isEmpty();
    }

    public boolean hasNoRows(@NonNull String tableName) {

        return !Data.TABLES.containsKey(tableName) ||
                Data.TABLES.get(tableName).size() <= 1;
    }

    private void appendRowToText(@NonNull StringBuilder text,
                                 @NonNull String tableName,
                                 int number,
                                 boolean verticalRuler) {
        List<String> row = Data.TABLES.get(tableName).get(number);
        List<Integer> lengths = getColumnLengths(tableName);
        boolean header = number == 0;
        int lengthOfLastNumber = String.valueOf(Data.TABLES.get(tableName).size() - 1).length();
        int lengthOfCurrentNumber = String.valueOf(number).length();
        text.append(verticalRuler ?
                header ?
                        " ".repeat(lengthOfLastNumber + 4) + "|" :
                        " ".repeat(lengthOfLastNumber - lengthOfCurrentNumber) + number + " -> |"
                : "|");
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
                            @NonNull String tableName,
                            boolean verticalRuler) {
        List<Integer> lengths = getColumnLengths(tableName);
        int lengthOfLastNumber = String.valueOf(Data.TABLES.get(tableName).size() - 1).length();
        if (verticalRuler) {
            text.append(" ".repeat(lengthOfLastNumber + 4));
        }
        text.append("|");
        lengths
                .forEach(length -> {
                            text.append("-".repeat(length));
                            text.append("+");
                        }
                );
        text.deleteCharAt(text.length() - 1);
        text.append("|\n");
    }

    private void appendHeavyLine(@NonNull StringBuilder text,
                                 @NonNull String tableName,
                                 boolean verticalRuler) {
        int lengthOfLastNumber = String.valueOf(Data.TABLES.get(tableName).size() - 1).length();
        if (verticalRuler) {
            text.append(" ".repeat(lengthOfLastNumber + 4));
        }
        int length = getTableCharLength(tableName);
        text.append("=".repeat(length));
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

        return " ".repeat(beforeSpaces) +
                value +
                " ".repeat(afterSpaces);
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

    private boolean isTableInvalid(@NonNull String tableName) {
        List<List<String>> table = Data.TABLES.get(tableName);

        if (table.isEmpty()) {

            return true;
        }

        if (table.stream().anyMatch(Objects::isNull)) {

            return true;
        }

        if (
                table
                        .stream()
                        .anyMatch(row ->
                                row.stream().anyMatch(Objects::isNull))
        ) {

            return true;
        }

        if (table.getFirst().isEmpty() && table.size() > 1) {

            return true;
        }

        return table
                .stream()
                .skip(1)
                .anyMatch(row ->
                        row.size() != table.getFirst().size());
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
