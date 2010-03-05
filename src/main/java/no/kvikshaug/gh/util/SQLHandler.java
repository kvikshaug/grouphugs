package no.kvikshaug.gh.util;

import no.kvikshaug.gh.exceptions.SQLUnavailableException;

import java.io.File;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.ArrayList;
import java.util.List;

/**
 * The SQLHandler is a wrapper for the SQL class. Together, these two provide
 * a "simple as can get"-interface to the JDBC api.
 *
 * Typical usage:
 * 1) Create a new SQLHandler, specifying whether or not you want verbose output to System.err.
 * 2) Call the setConnection() method with the connection data. This can be recalled at any time to
 * make another connection.
 * 3) Call the query methods as needed. These are named after the query name (e.g. select(), insert() etc.)
 *
 * If you specify verbose output, you won't have to think about displaying error information when catching
 * an SQLException. You still have to catch it though, to perform proper error handling in your application.
 *
 * The SQLHandler will detect connection timeouts and automatically reconnect to the database. However, if
 * a reconnection also fails, the handler will give up and (if verbose was set to true) just output the exception info.
 * Upon any new query after that, the application will make a new attempt to reconnect.
 *
 * Author: Alex Kvikshaug
 */
public class SQLHandler {

    private static SQLUnavailableException sqlUnavailableException = null;

    public static SQLHandler getSQLHandler() throws SQLUnavailableException {
        if(sqlUnavailableException != null) {
            throw sqlUnavailableException;
        }
        try {
            return sqlHandler == null ? sqlHandler = new SQLHandler(true) : sqlHandler;
        } catch(SQLUnavailableException ex) {
            // we catch it the first time, so that we're able to display an error message this one time
            System.err.println("Couldn't load SQL!");
            System.err.println(ex.getMessage());
            System.err.println("Modules requiring SQL will be disabled.");
            System.err.println();
            sqlUnavailableException = ex;
            throw ex;
        }
    }

    private static SQLHandler sqlHandler;

    private SQL sql;
    private boolean connectionOK = false;
    private boolean verbose;

    private final String connectionUrl = "jdbc:sqlite:grouphugs.db";

    /**
     * Constructs a new SQLHandler object
     * @param verbose if true, this class will output information about its actions, and (mostly) error handling,
     * to System.err.
     * @throws SQLUnavailableException if SQL is unavailable
     */
    public SQLHandler(boolean verbose) throws SQLUnavailableException {
        try {
            File db = new File("grouphugs.db");
            if(!db.exists()) {
                throw new SQLUnavailableException("Unable to find the database file (expected in: " + db.getAbsolutePath() + ")");
            }
            sql = new SQL();
            this.verbose = verbose;
            setConnection();
        } catch (ClassNotFoundException e) {
            throw new SQLUnavailableException("Unable to load the SQL JDBC driver.");
        }
    }

    /**
     * Initiates an SQL connection. Should always be called before any query is attempted.
     * This method was originally public, however for the gh project it is not needed.
     */
    private void setConnection() {
        try {
            reconnect();
            connectionOK = true;
        } catch(SQLException ex) {
            if(verbose) {
                System.err.println("sql> Failed to connect to the SQL database!");
                System.err.println("sql> Message: "+ex.getMessage());
                System.err.println("sql> Cause: "+ex.getCause());
                System.err.println("sql> "+ex);
            }
            connectionOK = false;
        }
    }

    /**
     * Internal method.
     * Attempts to reconnect to the database. Will output info if cleaning
     * up the old connection fails, but not if establishing a new connection fails.
     * @throws SQLException if an error occurs while reconnecting.
     */
    private void reconnect() throws SQLException {
        try {
            sql.disconnect();
        } catch(SQLException ex) {
            if(verbose) {
                System.err.println("sql> Failed to clean up old SQL database connection!");
                System.err.println("sql> Message: "+ex.getMessage());
                System.err.println("sql> Cause: "+ex.getCause());
                System.err.println("sql> "+ex);
            }
            throw ex;
        }
        sql.connect(connectionUrl);
    }

    /**
     * Internal method.
     * Attempts to run a query on the connected database. If the query fails to execute, the method will
     * attempt to reconnect to the database and try again. If that ALSO fails, then the SQLException will be
     * thrown to the caller. If, when trying to reconnect, the connection fails, then that connection-exception
     * will be thrown to the caller.
     * @param query the query to run
     * @param parameters the parameters belonging to the query
     * @throws SQLException if a database error occurs
     */
    private void attemptQuery(String query, List<String> parameters) throws SQLException {
        attemptQuery(query, parameters, true);
    }

    /**
     * See attemptQuery(String, ArrayList&lt;String&gt;).
     * @param query the query to run
     * @param parameters the parameters belonging to the query
     * @param firstAttempt whether or not this is the first attempt
     * @throws SQLException if a database error occurs
     */
    private void attemptQuery(String query, List<String> parameters, boolean firstAttempt) throws SQLException {
        if(!firstAttempt || !connectionOK) {
            try {
                reconnect();
                connectionOK = true;
            } catch(SQLException ex) {
                connectionOK = false;
                throw ex;
            }
        }
        try {
            sql.query(query, parameters);
        } catch(SQLException ex) {
            if(firstAttempt) {
                if(verbose) {
                    System.err.println("sql> SQL query failed (timeout?), attempting reconnection...");
                }
                attemptQuery(query, parameters, false);
            } else {
                throw ex;
            }
        }
    }

    /**
     * Queries the database with a SELECT query, expecting only 1 row to be returned from the
     * query and returning that row. If no rows were found, null is returned.
     * This method expects no parameters to be given the query.
     * @param query the SELECT query, without parameters, to execute
     * @return the data of the first row from the query
     * @throws SQLException if a database error occurs - debug information will already have been printed out.
     */
    public Object[] selectSingle(String query) throws SQLException {
        return selectSingle(query, null);
    }

    /**
     * Queries the database with a SELECT query, expecting only 1 row to be returned from the query and
     * returning that row. If no rows were found, null is returned.
     * The query should contain a questionmark ("?") for each parameter input, and the
     * parameters should be included in the "parameters" parameter.
     * Single and double quotes in the parameters will be automatically escaped if they
     * aren't already.
     * @param query the SELECT query to execute
     * @param parameters the parameters for the query
     * @return the data of the first row from the query
     * @throws SQLException if a database error occurs - debug information will already have been printed out.
     */
    public Object[] selectSingle(String query, List<String> parameters) throws SQLException {
        try {
            return select(query, parameters).get(0);
        } catch(IndexOutOfBoundsException ex) {
            return null;
        }
    }

    /**
     * Queries the database with a SELECT query, and returns a list with the fetched rows.
     * This method expects no parameters to be given the query.
     * @param query the SELECT query, without parameters, to execute
     * @return an arraylist with the fetched rows
     * @throws SQLException if a database error occurs
     */
    public List<Object[]> select(String query) throws SQLException {
        return select(query, null);
    }

    /**
     * Queries the database with a SELECT query, and returns a list with the fetched rows.
     * The query should contain a questionmark ("?") for each parameter input, and the
     * parameters should be included in the "parameters" parameter.
     * Single and double quotes in the parameters will be automatically escaped if they
     * aren't already.
     * @param query the SELECT query to execute
     * @param parameters the parameters for the query
     * @return a list with the fetched rows
     * @throws SQLException if a database error occurs
     */
    public List<Object[]> select(String query, List<String> parameters) throws SQLException {
        try {
            List<Object[]> rows = new ArrayList<Object[]>();
            attemptQuery(query, parameters);
            while(sql.getNext()) {
                rows.add(sql.getRow().clone()); // clone the object since the reference will change
            }
            return rows;
        } catch(SQLException ex) {
            printExceptionOutput(ex);
            throw ex;
        }
    }

    /**
     * Queries the database with an INSERT query, and returns the inserted ID.
     * This method expects no parameters to be given to the query.
     * @param query the INSERT query, without parameters, to execute
     * @return the database ID of the last inserted row
     * @throws SQLException if a database error occurs
     */
    public int insert(String query) throws SQLException {
        return insert(query, null);
    }

    /**
     * Queries the database with an INSERT query, and returns the inserted ID.
     * The query should contain a questionmark ("?") for each parameter input, and the
     * parameters should be included in the "parameters" parameter.
     * Single and double quotes in the parameters will be automatically escaped if they
     * aren't already.
     * @param query the INSERT query to execute
     * @param parameters the parameters for the query
     * @return the database ID of the last inserted row
     * @throws SQLException if a database error occurs
     */
    public int insert(String query, List<String> parameters) throws SQLException {
        try {
            attemptQuery(query, parameters);
            return sql.getLastInsertID();
        } catch(SQLException ex) {
            printExceptionOutput(ex);
            throw ex;
        }
    }

    /**
     * Queries the database with a DELETE query.
     * This method expects no parameters to be given to the query.
     * @param query the DELETE query to execute
     * @throws SQLException if a database error occurs
     * @return the number of rows affected
     */
    public int delete(String query) throws SQLException {
        return delete(query, null);
    }

    /**
     * Queries the database with a DELETE query.
     * The query should contain a questionmark ("?") for each parameter input, and the
     * parameters should be included in the "parameters" parameter.
     * Single and double quotes in the parameters will be automatically escaped if they
     * aren't already.
     * @param query the DELETE query to execute
     * @param parameters the parameters for the query
     * @throws SQLException if a database error occurs
     * @return the number of rows affected
     */
    public int delete(String query, List<String> parameters) throws SQLException {
        try {
            attemptQuery(query, parameters);
            return sql.getAffectedRows();
        } catch(SQLException ex) {
            printExceptionOutput(ex);
            throw ex;
        }
    }

    /**
     * Queries the database with an UPDATE query.
     * This method expects no parameters to be given to the query.
     * @param query the UPDATE query to execute
     * @throws SQLException if a database error occurs
     * @return the number of rows affected
     */
    public int update(String query) throws SQLException {
        return update(query, null);
    }

    /**
     * Queries the database with a UPDATE query.
     * The query should contain a questionmark ("?") for each parameter input, and the
     * parameters should be included in the "parameters" parameter.
     * Single and double quotes in the parameters will be automatically escaped if they
     * aren't already.
     * @param query the UPDATE query to execute
     * @param parameters the parameters for the query
     * @throws SQLException if a database error occurs
     * @return the number of rows affected
     */
    public int update(String query, List<String> parameters) throws SQLException {
        try {
            attemptQuery(query, parameters);
            return sql.getAffectedRows();
        } catch(SQLException ex) {
            printExceptionOutput(ex);
            throw ex;
        }
    }


    private void printExceptionOutput(SQLException ex) {
        if(verbose) {
            if(ex instanceof SQLSyntaxErrorException) {
                System.err.println("sql> SQL syntax error!");
                System.err.println("sql> Message: "+ex.getMessage());
                System.err.println("sql> Cause: "+ex.getCause());
                System.err.println("sql> "+ex);
            } else {
                System.err.println("sql> SQL error!");
                System.err.println("sql> Message: "+ex.getMessage());
                System.err.println("sql> Cause: "+ex.getCause());
                System.err.println("sql> "+ex);
            }
        }
    }
}

