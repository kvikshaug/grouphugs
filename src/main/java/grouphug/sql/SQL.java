package grouphug.sql;

import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Provides a simple wrapper for the JDBC api.
 *
 * Its current form is tailored for MySQL, but it should be simple to change this to other
 * databases that implement JDBC.
 *
 * The constructor will throw an exception if the relevant JDBC driver isn't loaded.
 * After creating the SQL object, you can either call connect() including the connection details, OR you
 * can call setDefaults() with the connection details and then call connect() without any parameters.
 *
 * When a connection is established, use query() to execute a database query. Please look at that methods
 * javadoc for more information about parameters, etc.
 *
 * After a query has been executed, information from the query will be available, depending on the query type.
 *
 * Only for SELECT queries:
 * Extracted data should be fetched by first calling the getNext() method. getNext() will start at the first row
 * fetched from the database and return true as long as there are more rows. After getNext() has been called, and
 * returned true, the data fetched can be fetched by calling getRow(). That will return an object array. The first
 * index (0) will return the first column selected from the table, second index (1) the second column, etc.
 * The values can then be cast to the appropriate data type.
 *
 * Only for INSERT queries:
 * getLastInsertID() will return the auto-inserted ID for the last row.
 *
 * Only for UPDATE, INSERT, DELETE queries:
 * getAffectedRows() will return the number of affected rows.
 *
 * Finally, when no more queries are needed, the disconnect() method should be called to close the connection.
 *
 * Example code:
 *
 * try {
 *     SQL sql = new SQL();
 *     sql.connect(SQL_HOST, SQL_DB, SQL_USER, SQL_PASSWORD);
 *     sql.query("SELECT some_row, another_row FROM table;");
 *     while(sql.getNext()) {
 *       Object[] values = sql.getValueList();
 *       // values[0] will contain the data of some_row, values[1] from another_row, etc.
 *       String text = (String)values[0];
 *       // ....
 *     }
 * } catch(ClassNotFoundException e) {
 *     System.err.println("Fatal error: The JDBC driver could not be found! Please include the appropriate libraries.");
 *     System.exit(-1);
 * } catch(SQLSyntaxErrorException e) {
 *     // Handle the event of an SQLSyntaxErrorException, often caused by the programmer
 * } catch(SQLException e) {
 *     // Handle the event of an SQLException, happens mostly when connection or authentication fails
 * } finally {
 *     sql.disconnect();
 * }
 *
 * You can of course split the code in several try/catch blocks, in order to separate
 * a connection error from other errors.
 *
 * The SQL class also provides methods for converting between a String and java.sql.Date..
 *
 * Note that there really should not be a need for this class. It is simply used to make
 * the JDBC API *easier* to use, and therefore probably makes thing slower and less
 * efficient than optimal.
 * This will hide some of the JDBC functionality and expect that the user just wants a
 * simple and fast way to execute some SQL queries.
 *
 * See also the SQLHandler class which wraps this class, and the JDBC API, even further. 
 *
 * Authors: Alex Kvikshaug and Øyvind Øvergaard
 */
public class SQL {
    private static final DateFormat SQL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // default host, db, login credentials
    private String defaultDBFile;

    private int affectedRows;
    private int lastInsertID;
    private Object row[];

    private Connection connection;
    private Statement statement;
    private ResultSet resultset;

    /**
     * Constructs a new SQL object
     * @throws ClassNotFoundException if the JDBC driver is not linked to this application
     */
    public SQL() throws ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
    }

    /**
     * Sets the default connection data before connecting.
     * @param defaultSQLDB the default database
     */
    public void setDefaults(String defaultSQLDB) {
        this.defaultDBFile = defaultSQLDB;
    }

    /**
     * Connects to the default database
     * @throws SQLException - if we were unable to connect to the database
     */
    public void connect() throws SQLException {
        try {
            connect(defaultDBFile);
        } catch(NullPointerException ex) {
            throw new SQLException("Trying to connect to unspecified default values.");
        }
    }

    /**
     * Connects to the specified database
     * @param dbFile - Name of the database to open
     * @throws SQLException - if we were unable to connect to the database
     */
    public void connect(String dbFile) throws SQLException {
        connection = DriverManager.getConnection ("jdbc:sqlite:" + dbFile);
        statement = connection.createStatement();
    }

    /**
     * Attempts to close the database connection on a "trying my best"-approach.
     * @throws SQLException if a database error occurs
     */
    public void disconnect() throws SQLException {
        if (connection != null) {
            connection.close();
        }

        if (resultset != null) {
            resultset.close();
        }

        if (statement != null) {
            statement.close();
        }
        connection = null;
        resultset = null;
        statement = null;
    }

    /**
     * Performs an SQL query on the database.
     * The query should contain a questionmark ("?") for each parameter input, and the
     * parameters should be included in the "parameters" parameter.
     * Single and double quotes in the parameters will be automatically escaped if they
     * aren't already.
     * WARNING: If user input is put directly in the query, without using parameters,
     * the application will be vurnerable to SQL Injection. So please put all parameters
     * in the parameters list.
     * If there are no parameters to the query, the "parameters" parameter may be null.
     *
     * @param query the SQL query to send to the database
     * @param parameters the parameters for the SQL query
     * @throws SQLSyntaxErrorException - if there was a syntax error in the query
     * @throws SQLException - if the query failed
     */
    public void query(String query, ArrayList<String> parameters) throws SQLException {
        if(statement == null) {
            throw new SQLException("Connection does not seem to be initialized.");
        }

        if(parameters != null) {
            for (int i = 0; i < parameters.size(); i++) {
                parameters.set(i, escapeQuotes(parameters.get(i)));
            }
            int paramIndex;
            for(int i=0; (paramIndex = query.indexOf("?")) != -1; i++) {
                query = query.substring(0, paramIndex) + parameters.get(i) + query.substring(paramIndex + 1);
                //query = query.replaceFirst("\\?", parameters.get(i));
            }
        }

        // this line can be useful to measure the query sequence in an app
        //System.out.println("Running query: "+query);
        // if execute() returns true, the query was a SELECT statement, so we want to retrieve the result set
        if (statement.execute(query)) {
            resultset = statement.getResultSet();
            row = new Object[resultset.getMetaData().getColumnCount()];
        }
        // if execute() returns false, the query was either an UPDATE, INSERT or DELETE statement,
        // so we retrieve the number of rows affected
        else {
            affectedRows = statement.getUpdateCount();
            ResultSet key = statement.getGeneratedKeys();
            if(key != null && key.next()) {
                lastInsertID = (Integer)key.getObject(1);
            }
        }
    }

    /**
     * When a SELECT query has been performed, this method should be called to retrieve
     * each row.
     * After getNext has completed, the values for this row will be accessible by the
     * getRow() method.
     * @return boolean true as long as a new row was fetched, false when there are no more rows
     * @throws SQLException if an SQLException occurs
     */
    public boolean getNext() throws SQLException {
        if(!resultset.next()) {
            return false;
        }
        for(int i = 1; i <= resultset.getMetaData().getColumnCount(); i++) {
            row[i-1] = resultset.getObject(i);
        }
        return true;
    }

    /**
     * This is where the data is actually fetched. After a query has been run, AND getNext()
     * has been called, this will return an array with the current row. The indexes will be
     * placed accordingly to the arrangement in the query. F.ex., "SELECT some_row, another_row FROM..."
     * will yield row[0] = some_row, row[1] = another_row.
     * @return an array with the data from the current row
     */
    public Object[] getRow() {
        return row;
    }

    /**
     * Returns the last generated ID in the database from the last INSERT query.
     * @return the last generated ID
     */
    public long getLastInsertID() {
        return lastInsertID;
    }

    /**
     * Returns the number of affected rows in the database from the last executed
     * UPDATE, INSERT or DELETE query.  
     * @return the number of affected rows
     */
    public int getAffectedRows() {
        return affectedRows;
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


    /**
     * Escapes quotes that are not already escaped.
     * @param string the string to escape quotes
     * @return the string with escaped quotes
     */
    private String escapeQuotes(String string) {
        int quoteIndex = 0;
        while((quoteIndex = string.indexOf("'", quoteIndex)) != -1) {
            string = string.substring(0, quoteIndex) + "\\'" + string.substring(quoteIndex + 1);
            quoteIndex += 2;
        }
        quoteIndex = 0;
        while((quoteIndex = string.indexOf("\"", quoteIndex)) != -1) {
            string = string.substring(0, quoteIndex) + "\\\"" + string.substring(quoteIndex + 1);
            quoteIndex += 2;
        }
        return string;
    }


    /**
     * getConnection()
     * Returns the current connection for excentric classes who wishes to prepare their own statements.
     * @return the current connection
     */
    /*
    public Connection getConnection() {
        return connection;
    }
    */

    /*
    public void executePreparedUpdate(PreparedStatement statement) throws SQLException{
    	statement.executeUpdate();
    }
    public void executePreparedSelect(PreparedStatement statement) throws SQLException{
    	if(statement.execute()){
    		resultset = statement.getResultSet();
            row = new Object[resultset.getMetaData().getColumnCount()];
    	}
    }
    */
    /**
     * Returns the result set returned from a SQL query
     * @return resultset
     */
    /*
    public ResultSet getResultset() {
        return resultset;
    }
    */

}

