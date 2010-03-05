package no.kvikshaug.gh.exceptions;

import java.sql.SQLException;

/**
 * Thrown when SQL is unavailable for use to the modules, because
 * the driver isn't loaded, the database file doesn't exist or for other reasons.
 */
public class SQLUnavailableException extends SQLException {
    public SQLUnavailableException(String message) {
        super(message);
    }
}
