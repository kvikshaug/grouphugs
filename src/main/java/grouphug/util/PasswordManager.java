package grouphug.util;

import java.io.*;

// TODO a lot
/**
 * PasswordManager handles the MySQL passwords that will be loaded from files so they
 * are not uploaded in cleartext to the publicly accessible svn repository
 */
public class PasswordManager {

    private static final String PW_FILE = "sql-pw";

    private static String hinuxPass;

    /**
     * Returns the password for the HiNux MySQL DB account read from file, or null if it wasn't correctly read
     * @return the password for the HiNux MySQL DB account read from file, or null if it wasn't correctly read
     */
    public static String getSQLPassword() {
        return hinuxPass;
    }

    /**
     * Tries to load the sql- password from file into memory
     *
     * @return true if the password was successfully loaded
     */
    public static boolean loadPasswords() {
        boolean readOK = true;

        if(Debugger.DEBUG) {
            if(Debugger.HINUX_PASSWORD != null) {
                hinuxPass = Debugger.HINUX_PASSWORD;
            } else {
                System.err.println("Debug-mode: MySQL password for HiNux not set.");
                readOK = false;
            }
        } else {
            // The read attempts are in separate try/catch blocks, so that if the first fails, it still tries the others
            try {
                hinuxPass = loadPassword(PW_FILE);
            } catch(IOException ex) {
                System.err.println("Failed to load hinux password: "+ex.getMessage());
                readOK = false;
            }
        }
        return readOK;
    }

    /**
     * Load up the SQL password from the first line of the specified textfile
     *
     * @param file the filename to retrieve
     * @return true if the password was successfully fetched and saved, false otherwise
     * @throws java.io.IOException if an IOException occurs :)
     */
    private static String loadPassword(String file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(new File(file)));
        String pass = reader.readLine();
        reader.close();
        if(pass == null || pass.equals(""))
            throw new FileNotFoundException("No data extracted from MySQL password file!");
        return pass;
    }
}
