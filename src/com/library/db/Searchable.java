package com.library.db;

import java.sql.SQLException;
import java.util.List;

/** Anything that can be searched by a keyword. */
public interface Searchable<T> {
    List<T> search(String keyword) throws SQLException;
}
