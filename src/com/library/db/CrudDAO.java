package com.library.db;

import java.sql.SQLException;
import java.util.List;

/** Generic interface for DAOs that can list and delete rows of type T. */
public interface CrudDAO<T> {
    List<T> getAll() throws SQLException;

    void delete(int id) throws SQLException;
}
