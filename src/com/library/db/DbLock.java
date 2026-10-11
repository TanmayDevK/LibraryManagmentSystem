package com.library.db;

/**
 * One shared lock for everything that touches the shared database connection
 * from more than one thread (the Swing thread and the background monitors),
 * or that runs a multi-statement transaction.
 */
public final class DbLock {
    public static final Object INSTANCE = new Object();

    private DbLock() {
    }
}
