package dev.addition.ten.util.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface RowMapper<T> {
    T map(ResultSet rs) throws SQLException;
}