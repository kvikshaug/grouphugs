package grouphug;

import grouphug.util.PasswordManager;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * Provides functionality to connect to and perform operations on an SQL database.
 * Typical usage:
 *
 * SQL sql = new SQL();
 * try {
 *     sql.connect(SQL_HOST, SQL_DB, SQL_USER, SQL_PASSWORD);
 *     sql.query("SELECT some_row, another_row FROM table;");
 *     while(sql.getNext()) {
 *       Object[] values = sql.getValueList();
 *       // values[0] will contain the data of some_row, values[1] from another_row, etc.
 *       String text = (String)values[0];
 *       useSomeMethodOn(text);
 *       // ....
 *     }
 * } catch(SQLSyntaxErrorException e) {
 *   // Handle the event of an SQLSyntaxErrorException, often caused by the programmer
 * } catch(SQLException e) {
 *   // Handle the event of an SQLException, happens mostly when connection or authentication fails
 * } finally {
 *   sql.disconnect();
 * }
 *
 * You can of course split the code in several try/catch blocks, in order to separate
 * a connection error from other errors.
 *
 * Thinking of this, actually, a SQLConnectionError should be thrown instead of a generic
 * SQLException! But that is not up to this class, but rather, mysql-jdbc. Hmm. 
 *
 */
public class SQL {
    // TODO defend against sql injection

    private static final DateFormat SQL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // MySQL host, db, login credentials
    private static final String DEFAULT_SQL_HOST = "127.0.0.1";
    private static final String DEFAULT_SQL_DB = "murray";
    private static final String DEFAULT_SQL_USER = "gh";
    private static String DEFAULT_SQL_PASSWORD;

    private int affectedRows;
    private Object valueList[];

    private Connection connection = null;
    private Statement statement = null;
    private ResultSet resultset = null;

    /**
     * Constructs a new SQL object
     */
    public SQL() {
        try {
            Class.forName("org.gjt.mm.mysql.Driver");
        } catch(ClassNotFoundException e) {
            System.err.println("Fatal error: The mysql-jdbc driver could not be found!");
            System.exit(-1);
        }
    }

    public Object[] getValueList() {
        return valueList;
    }

    /**
     * Returns the result set returned from a SQL query
     * @return resultset
     */
    public ResultSet getResultset() {
        return resultset;
    }

    /**
     * The number of rows in the database affected by a UPDATE, INSERT or DELETE statement
     * @return  affectedRows
     */
    public int getAffectedRows() {
        return affectedRows;
    }

    /**
     * Connects to the default database
     * @throws SQLException - if we were unable to connect to the database
     */
    public void connect() throws SQLException {
        if(DEFAULT_SQL_PASSWORD == null) {
            if((DEFAULT_SQL_PASSWORD = PasswordManager.getHinuxPass()) == null) {
                throw new SQLException("The default password for the hinux SQL DB hasn't been properly loaded from file!");
            }
        }
        connect(DEFAULT_SQL_HOST, DEFAULT_SQL_DB, DEFAULT_SQL_USER, DEFAULT_SQL_PASSWORD);
    }

    /**
     * Connects to the specified database
     * @param host - IP address or DNS of the host
     * @param db - Name of the database to open
     * @param user - Username used to authenticate
     * @param password - Password used to authenticate
     * @throws SQLException - if we were unable to connect to the database
     */
    public void connect(String host, String db, String user, String password) throws SQLException {
        connection = DriverManager.getConnection ("jdbc:mysql://" + host + '/' + db, user, password);
        statement = connection.createStatement();
    }

    /**
     * Close the connection to the news database.
     */
    public void disconnect() {
        // close the database connection, the resultset and the statement
        try {
            if (connection != null) {
                connection.close();
            }

            if (resultset != null) {
                resultset.close();
            }

            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            System.err.println(" > ERROR: Unable to close database connection!");
        }
    }

    /**
     * getConnection()
     * Returns the current connection for excentric classes who wishes to prepare their own statements.
     * @return the current connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Performs a SQL query on the database
     * @param query - the SQL query to send to the database
     * @throws SQLSyntaxErrorException - if there was a syntax error in the query
     * @throws SQLException - if the query failed
     */
    public void query(String query) throws SQLException {
        // if execute() returns true, the query was a SELECT statement, so we want to retrieve the result set
        if (statement.execute(query)) {
            resultset = statement.getResultSet();
            valueList = new Object[resultset.getMetaData().getColumnCount()];
        }
        // if execute() returns false, the query was either an UPDATE, INSERT or DELETE statement,
        // so we retrieve the number of rows affected
        else {
            affectedRows = statement.getUpdateCount();
        }
    }

    /**
     * Iterates trough the rows in the result set, saving the values for each coloumn
     * in the row in valueList - accessible by getValueList.
     * @return boolean - returns false if there are no more rows, true otherwise.
     * @throws SQLException - if an SQLException occurs
     */
    public boolean getNext() throws SQLException {
        if(!resultset.next()) {
            return false;
        }
        for(int i = 1; i <= resultset.getMetaData().getColumnCount(); i++) {
            valueList[i-1] = resultset.getObject(i);
        }
        return true;
    }

    /**
     * Converts a java.util.Date object to a SQL datetime string (with quotes).
     * @param date the Date object we want to convert.
     * @return a String representation of the date and time <code>date</code> represents, in the SQL datetime format.
     */
    public static String dateToSQLDateTime(java.util.Date date) {
        return SQL_DATE_FORMAT.format(date);
    }

    /**
     * Converts a SQL datetime string to a java.util.Date object.
     * @param dateTime the SQL datetime string we want to convert.
     * @return a Date object that represents the SQL datetime defined in <code>dateTime</code>.
     * @throws ParseException if the beginning of the specified string cannot be parsed.
     */
    public static java.util.Date sqlDateTimeToDate(String dateTime) throws ParseException {
        return SQL_DATE_FORMAT.parse(dateTime);
    }
}
