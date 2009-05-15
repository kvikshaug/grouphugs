package grouphug.util;

/**
 * If you want to run/test the bot from your local machine, use this class.
 *
 * *** ALWAYS remember to revert this file back before committing to svn! ***
 *
 * 1. First, go through all the vars in this class and set them to your preferences.
 *
 * 2. No modules are loaded by default when debugging so you need to do this manually.
 *    Visit the psvm (main) method in the Grouphug class. Find the "// Load up modules" comment.
 *    Right below that, load up the modules you need as explained by the example.
 *
 * Note: Local SQL debugging will not work because:
 *       1) HiNux doesn't allow external mysql connections
 *       2) I'm not giving away the grimstux password
 *       Hence, there is no point in setting the passwords.
 *       Note that default SQL host for hinux is "127.0.0.1" so some modules will
 *       attempt to connect directly on your local machine.
 */
public class Debugger {

    // set this to true
    public final static boolean DEBUG = false; // default: false

    // if you want your own playground-channel, change this
    public final static String CHANNEL = "#grouphugs"; // default: "#grouphugs"

    // here you *could* set the password(s) to the sql db's.
    // if not set, modules using SQL will fail - the bot however will still run.
    public final static String HINUX_PASSWORD = null; // default: null
    public final static String GRIMSTUX_PASSWORD = null; // default: null

    /* Again - ALWAYS REMEMBER TO REVERT THIS FILE BACK BEFORE COMMITTING! */
}
