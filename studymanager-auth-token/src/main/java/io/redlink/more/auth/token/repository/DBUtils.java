package io.redlink.more.auth.token.repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class DBUtils {
    public static OptionalInt readOptionalInt(ResultSet row, String columnLabel) throws SQLException {
        final int value = row.getInt(columnLabel);
        if (row.wasNull()) {
            return OptionalInt.empty();
        } else {
            return OptionalInt.of(value);
        }
    }

    /**
     * Consumes Array elements from a column of the result set
     * @param rs the result set
     * @param columnLabel the name of the column in the parsed result set
     * @param type the expected type. Elements that are not of that type are filtered
     * @param collector the collector for the array elements
     * @return <code>true</code> if the column was present. <code>false</code> if <code>null</code>
     * @param <T>
     * @throws SQLException
     */
    public static <T> boolean consumeArray(ResultSet rs, String columnLabel, Class<T> type, Consumer<T> collector) throws SQLException {
        Array sqlArray = rs.getArray(columnLabel);
        if (!rs.wasNull()) {
            Stream.of((Object[]) sqlArray.getArray())
                    .filter(Objects::nonNull) //instead of an empty Array SQL adds a NULL element at idx:0 ...
                    .filter(e -> type.isAssignableFrom(e.getClass()))
                    .map(type::cast)
                    .forEach(collector);
            return true;
        } else { //no need to process a NULL value
            return false;
        }
    }

    public static <T> Set<T> readSet(ResultSet rs, String columnLabel, Class<T> type) throws SQLException {
        Set<T> set = new HashSet<>();
        consumeArray(rs, columnLabel, type, set::add);
        return set;
    }
}
