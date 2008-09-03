package grouphug;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.io.*;

/**
 * Provides functionality to connect to and perform operations on the news database.
 */
public class SQL {
    public static final boolean DEBUG = true;

    private static final DateFormat SQL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    // JDBC Driver
    private static final String SQL_DRIVER = "org.gjt.mm.mysql.Driver";

    //MySQL host and db
    private static final String SQL_HOST = "127.0.0.1";
    private static final String SQL_DB = "gh";

    // Login credentials
    private static final String SQL_USER = "murray";
    private static String SQL_PASS;

    private int affectedRows;
    private Object valueList[];

    private Connection connection = null;
    private Statement statement = null;
    private ResultSet resultset = null;

    public static void loadPassword() throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(new File("mysqlpw.txt")));
        SQL_PASS = reader.readLine();
        reader.close();
        if(SQL_PASS.equals(""))
            throw new Exception("No data extracted from MySQL password file!");
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
     * Connects to the news database
     * @return boolean - true if connection is established, false otherwise.
     */
    public boolean connect() {
        // Load the JDBC driver
        try {
            Class.forName(SQL_DRIVER);
        } catch (ClassNotFoundException e) {
            System.err.println(" > ERROR: Unable to load grouphug.SQL driver: " + SQL_DRIVER);
            if (DEBUG) { e.printStackTrace(); }

            return false;
        }

        // Connect to DB
        try {
            connection = DriverManager.getConnection
                    ( "jdbc:mysql://" + SQL_HOST + '/' + SQL_DB, SQL_USER, SQL_PASS);

            statement = connection.createStatement();
        } catch (SQLException e) {
            System.err.println(" > ERROR: Unable to connect to database!");
            if (DEBUG) { e.printStackTrace(); }
            return false;
        }

        return true;
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
     * @return boolean - returns true if the query was successful, false otherwise
     */
    public boolean query(String query) {
        try {
            // if execute() returns true, the query was a SELECT statement, so we want to retrieve the result set
            if (statement.execute(query)) {
                resultset = statement.getResultSet();
                valueList = new Object[resultset.getMetaData().getColumnCount()];
            }
            // if execute() returns false, the query was either an UPDATE, INSERT or DELETE statement, so we retrieve
            // the number of rows affected by the
            else {
                affectedRows = statement.getUpdateCount();
            }

        } catch (SQLSyntaxErrorException e) {
            // if we have an error in our grouphug.SQL syntax, let us know!
            System.err.println(e.getMessage());

            return false;

        } catch (SQLException e) {
            System.err.println(" > ERROR: Failed to execute query on DB!");
            if (DEBUG) { e.printStackTrace(); }
            return false;
        }

        return true;
    }

    /**
     * Iterates trough the rows in the result set, saving the values for each coloumn in the row in valueList - accessible
     * by getValueList.
     * @return boolean - returns false if there are no more rows, true otherwise.
     */
    public boolean getNext() {

        try {
            if(!resultset.next()) {
                return false;
            }
            for(int i = 1; i <= resultset.getMetaData().getColumnCount(); i++) {
                valueList[i-1] = resultset.getObject(i);
            }


        } catch (SQLException e) {
            return false;
        }


        return true;
    }

    /**
     * Converts a java.util.Date object to a SQL datetime string (with quotes).
     * @param date the Date object we want to convert.
     * @return a String representation of the date and time <code>date</code> represents, in the SQL datetime format.
     */
    public static String dateToSQLDateTime(java.util.Date date) {
        if(date == null)
            return "NULL";
        else
            return "'"+SQL_DATE_FORMAT.format(date)+"'";
    }

    /**
     * Converts a SQL datetime string to a java.util.Date object.
     * @param dateTime the SQL datetime string we want to convert.
     * @return a Date object that represents the SQL datetime defined in <code>dateTime</code>. Returns null if parsing
     *         somehow fails - if this happens, it is probably because the date format is wrong.
     */
    public static java.util.Date sqlDateTimeToDate(String dateTime) {
        java.util.Date date = null;

        if (dateTime != null) {
            try {
                date = SQL_DATE_FORMAT.parse(dateTime);
            } catch (ParseException e) {
                System.err.println(" > ERROR: Failed to parse grouphug.SQL datetime!");
            }
        }
        return date;
    }
}