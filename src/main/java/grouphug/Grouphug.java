package grouphug;

import org.jibble.pircbot.*;
import grouphug.util.PasswordManager;
import grouphug.util.Debugger;

import java.util.ArrayList;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLClassLoader;

/**
 * Grouphug
 *
 * A java-based IRC-bot created purely for entertainment purposes and as a personal excercise in design.
 *
 * This bot manages a list of modules that may make the bot react up certain triggers or messages sent to
 * the channel this bot resides on. It contains functionality for preventing spam, splitting lines and more.
 *
 * The modules are dynamically loaded - the demands for a module is simple:
 * - It must exist in the grouphug.modules package
 * - It must implement the grouphug.modules.GrouphugModule interface
 * - Its constructor must take no parameters (because of dynamic loading)
 * Any module filling these demands will be loaded and accessed as any other upon trigger calls.
 *
 * Some important concepts for the bot:
 * - It should never bother anyone unless it is clear that they want a response from it.
 * - It should never be unclear what a command or module does or intends to do. From a single !help trigger,
 *   a user should be able to dig down in detail and find out every interaction he/she is able to make to the bot,
 *   and what to be expected in return.
 *
 * Certain functionality is closely tied to the linux account it currently runs on, and shell scripts,
 * website access and the like located on that account.
 *
 * A future vision for the bot will be to changed the design to be event-based, an own event for each
 * overriden method (onMessage, onKick etc.). This is currently under development and anyone are free
 * to contribute at this stage.
 *
 * The grouphug bot was originally started by Alex Kvikshaug and continued as
 * an SVN project by the guys currently hanging in #grouphugs @ efnet.
 *
 * The bot extends the functionality of the well-designed PircBot, see http://www.jibble.org/
 */

// TODO - use sunn's grouphug.utils.Web on: Google/GoogleFight/Define/Tracking/Confession
// TODO - bash for #grouphugs ?
// TODO - tlf module ?

public class Grouphug extends PircBot {

    // Channel and server
    public static final String CHANNEL = Debugger.CHANNEL;
    public static final String SERVER = "irc.homelien.no";

    // Character encoding to use when communicating with the IRC server.
    public static final String ENCODING = "ISO8859-15";

    // The trigger characters (as Strings since startsWith takes String)
    public static final String MAIN_TRIGGER = "!";
    public static final String SPAM_TRIGGER = "@";
    public static final String HELP_TRIGGER = "help";

    // the root directory the bot is running from
    public static final String ROOT_DIR = "/home/DT2006/murray/gh/";

    // A list over all the nicknames we want
    protected static ArrayList<String> nicks = new ArrayList<String>();

    // The number of characters upon which lines are splitted
    // Note that the 512 max limit includes the channel name, \r\n, and probably some other stuff.
    // maxing out on 450 seems to be a reasonable amount, both ways.
    private static final int MAX_LINE_CHARS = 450;

    // How many lines we can send to the channel in one go without needing spam-trigger
    private static final int MAX_SPAM_LINES = 5;

    // How often to try to reconnect to the server when disconnected, in ms
    private static final int RECONNECT_TIME = 15000;

    // The file to log all messages to
    private static File logfile = new File(ROOT_DIR+"log-current");

    // The standard outputstream
    private static PrintStream stdOut;

    // A list over all loaded modules
    private static ArrayList<GrouphugModule> modules = new ArrayList<GrouphugModule>();

    // Used to specify if it is ok to spam a large message to the channel
    private static boolean spamOK = false;

    // A static reference and getter to our bot
    private static Grouphug bot;
    public static Grouphug getInstance() {
        return bot;
    }


    /**
     * This method is called whenever a message is sent to a channel.
     * This triggers all loaded modules and lets them react to the message.
     *
     * @param channel - The channel to which the message was sent.
     * @param sender - The nick of the person who sent the message.
     * @param login - The login of the person who sent the message.
     * @param hostname - The hostname of the person who sent the message.
     * @param message - The actual message sent to the channel.
     */
    @Override
    protected void onMessage(String channel, String sender, String login, String hostname, String message) {

        // Rebooting?
        if(message.equals("!reboot")) {
            try {
                Runtime.getRuntime().exec("wget -qO - http://hinux.hin.no/~murray/gh/?reboot > /dev/null 2>&1");
            } catch(IOException ex) {
                System.err.println(ex);
            }
            return;
        }

        // Reloading?
        if(message.equals("!reload")) {
            if(!recompileModules())
                return;

            bot.sendMessage("Reloaded "+reloadModules()+" modules.", false);
            return;
        }

        // First check for help trigger
        checkForHelpTrigger(channel, sender, login, hostname, message);

        // Then, check if the message starts with a normal or spam-trigger
        if(message.startsWith(MAIN_TRIGGER) || message.startsWith(SPAM_TRIGGER)) {
            // Check if spam has been triggered
            if(message.startsWith(MAIN_TRIGGER)) {
                spamOK = false;
            } else {
                if(sender.contains("icc") || login.contains("icc")) {
                    sendMessage(CHANNEL, "icc, you are not allowed to use the spam trigger.");
                    return;
                }
                spamOK = true;
            }

            // For each module, call the trigger-method with the sent message
            for(GrouphugModule m : modules) {
                m.trigger(channel, sender, login, hostname, message.substring(1));
            }
        }

        // run the specialTrigger() method for special modules who might want to
        // react on messages without trigger
        for(GrouphugModule m : modules) {
            m.specialTrigger(channel, sender, login, hostname, message);
        }
    }

    @Override
    protected void onPrivateMessage(String sender, String login, String hostname, String message) {
        // Help triggers will also activate in PM
        checkForHelpTrigger(null, sender, login, hostname, message);
    }

    private void checkForHelpTrigger(String channel, String sender, String login, String hostname, String message) {
        // First, check for the universal normal help-trigger
        if(message.equals(MAIN_TRIGGER + HELP_TRIGGER)) {
            sendMessage("Currently implemented modules on "+this.getNick()+":", false);
            String helpString = "reboot, reload";
            for(GrouphugModule m : modules) {
                helpString += ", ";
                helpString += m.helpMainTrigger(channel, sender, login, hostname, message);
            }
            sendMessage(helpString, false);
            sendMessage("Use \"!help <module>\" for more specific info. This will also work in PM.", false);
        }
        // if not, check if help is triggered with a special module
        else if(message.startsWith(MAIN_TRIGGER+HELP_TRIGGER+" ")) {
            boolean replied = false;
            if(message.equals(MAIN_TRIGGER+HELP_TRIGGER+" reboot")) {
                sendMessage("\"Reboot\" shuts down the bot, recompiles everything, and restarts.", false);
                replied = true;
            } else if(message.equals(MAIN_TRIGGER+HELP_TRIGGER+" reload")) {
                sendMessage("\"Reload\" recompiles and reloads all modules, without restarting the bot.", false);
                replied = true;
            }
            for(GrouphugModule m : modules) {
                String reply = m.helpSpecialTrigger(channel, sender, login, hostname, message.substring(MAIN_TRIGGER.length() + HELP_TRIGGER.length() + 1));
                if(reply != null) {
                    bot.sendMessage(reply, false);
                    replied = true;
                }
            }
            if(!replied)
                sendMessage("No one has implemented a "+message.substring(MAIN_TRIGGER.length() + HELP_TRIGGER.length() + 1)+" module yet.", false);
        }
    }

    /**
     * This method is called whenever someone (possibly us) is kicked from any of the channels that we are in.
     * If we were kicked, try to rejoin with a sorry message.
     *
     * @param channel - The channel from which the recipient was kicked.
     * @param kickerNick - The nick of the user who performed the kick.
     * @param kickerLogin - The login of the user who performed the kick.
     * @param kickerHostname - The hostname of the user who performed the kick.
     * @param recipientNick - The unfortunate recipient of the kick.
     * @param reason - The reason given by the user who performed the kick.
     */
    @Override
    protected void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
        if (recipientNick.equalsIgnoreCase(getNick())) {
            joinChannel(channel);
            sendMessage(CHANNEL, "sry :(");
        }
    }


    /**
     * This method is called whenever someone (possibly us) joins a channel which we are on.
     * What we do is hug them :) <3
     *
     * @param channel - The channel which somebody joined.
     * @param sender - The nick of the user who joined the channel.
     * @param login - The login of the user who joined the channel.
     * @param hostname - The hostname of the user who joined the channel.
     */
    @Override
    protected void onJoin(String channel, String sender, String login, String hostname) {
        /*
        if(!sender.equals(getNick()))
            sendAction(CHANNEL, "laughs at icc");
        */
    }

    /**
     * This method is called when we receive a user list from the server after joining a channel.
     * Shortly after joining a channel, the IRC server sends a list of all users in that channel. The PircBot collects this information and calls this method as soon as it has the full list.
     * To obtain the nick of each user in the channel, call the getNick() method on each User object in the array.
     * At a later time, you may call the getUsers method to obtain an up to date list of the users in the channel.
     *
     * @param channel - The name of the channel.
     * @param users - An array of User objects belonging to this channel.
     */
    @Override
    protected void onUserList(String channel, User[] users) {
        /*
        String nicks = "";
        for(User u : users) {
            if(!u.getNick().equals(getNick()))
                nicks += ", "+u.getNick();
        }
        nicks = nicks.substring(2);
        if(nicks.contains(", ")) {
            String org = nicks;
            nicks = org.substring(0, org.lastIndexOf(", ")) + " and " + org.substring(org.lastIndexOf(", ") + 2);
        }
        sendAction(Grouphug.CHANNEL, "hugs "+nicks);
        */
    }

    /**
     * This method carries out the actions to be performed when the PircBot gets disconnected. This may happen if the
     * PircBot quits from the server, or if the connection is unexpectedly lost.
     * Disconnection from the IRC server is detected immediately if either we or the server close the connection
     * normally. If the connection to the server is lost, but neither we nor the server have explicitly closed the
     * connection, then it may take a few minutes to detect (this is commonly referred to as a "ping timeout").
     */
    @Override
    protected void onDisconnect() {
        try {
            Grouphug.connect(this, true);
        } catch(IrcException e) {
            // No idea how to handle this. So print the message and exit
            System.err.println(e.getMessage());
            stdOut.flush();
            System.exit(-1);
        } catch(IOException e) {
            // No idea how to handle this. So print the message and exit
            System.err.println(e.getMessage());
            stdOut.flush();
            System.exit(-1);
        }
        this.joinChannel(Grouphug.CHANNEL);
    }

    /**
     * Sends a message to a channel or a private message to a user.
     *
     * The messages are splitted by maximum line number characters and by the newline character (\n), then
     * each line is sent to the pircbot sendMessage function, which adds the lines to the outgoing message queue
     * and sends them at the earliest possible opportunity.
     *
     * @param message - The message to send
     * @param verifySpam - true if verifying that spamming is ok before sending large messages
     */
    public void sendMessage(String message, boolean verifySpam) {

        // First create a list of the lines we will send separately.
        ArrayList<String> lines = new ArrayList<String>();

        // This will be used for searching.
        int index;

        // Remove all carriage returns.
        for(index = message.indexOf('\r'); index != -1; index = message.indexOf('\r'))
            message = message.substring(0, index) + message.substring(index + 1);

        // Split all \n into different lines
        for(index = message.indexOf('\n'); index != -1; index = message.indexOf('\n')) {
            lines.add(message.substring(0, index).trim());
            message = message.substring(index + 1);
        }
        lines.add(message.trim());

        // If the message is longer than max line chars, separate them
        for(int i = 0; i<lines.size(); i++) {
            while(lines.get(i).length() > Grouphug.MAX_LINE_CHARS) {
                String line = lines.get(i);
                lines.remove(i);
                lines.add(i, line.substring(0, Grouphug.MAX_LINE_CHARS).trim());
                lines.add(i+1, line.substring(Grouphug.MAX_LINE_CHARS).trim());
            }
        }

        // Remove all empty lines
        for(int i = 0; i<lines.size(); i++) {
            if(lines.get(i).equals(""))
                lines.remove(i);
        }

        // Now check if we are spamming the channel, and stop if the spam-trigger isn't used
        if(verifySpam && !spamOK && lines.size() > MAX_SPAM_LINES) {
            sendMessage(Grouphug.CHANNEL, "This would spam the channel with "+lines.size()+" lines, replace "+MAIN_TRIGGER+" with "+SPAM_TRIGGER+" if you really want that.");
            return;
        }

        // Finally send all the lines to the channel
        for(String line : lines) {
            this.sendMessage(Grouphug.CHANNEL, line);
        }
    }

    /**
     * Clears all loaded modules, and runs the loadModules() method
     *
     * @return the number of loaded modules
     */
    // TODO - do this automatically upon SVNCommit, without output?
    private static int reloadModules() {
        modules.clear();
        return loadModules();
    }

    /**
     * Loads up all the modules in the modules package (skipping anything not ending
     * with ".class" or containing a '$'-char)
     *
     * @return the number of loaded modules
     */
    private static int loadModules() {
        System.out.println("(CL) Starting Class Loader...");
        File moduleDirectory = new File(ROOT_DIR+"out/grouphug/modules/");

        // Create a new classloader
        URL[] urls = null;
        try {
            URL url = moduleDirectory.toURI().toURL();
            urls = new URL[]{url};
        } catch (MalformedURLException e) {
            // this won't happen
        }

        ClassLoader cl = new URLClassLoader(urls);

        int loadedModules = 0;

        for(String s : moduleDirectory.list()) {
            if(s.contains("$")) {
                System.out.println("(CL) "+s+" : Skipped");
                continue;
            }
            if(!s.endsWith(".class")) {
                System.out.println("(CL) "+s+" : Skipped");
                continue;
            }
            s = s.substring(0, s.length()-6); // strip ".class"
            Class clazz;
            try {
                clazz = cl.loadClass(s);
                modules.add((GrouphugModule)clazz.newInstance());
                System.out.println("(CL) "+s+".class : Loaded OK");
                loadedModules++;
            } catch (InstantiationException e) {
                System.err.println("(CL) "+s+".class : Failed to load!");
                System.err.println(e);
            } catch (IllegalAccessException e) {
                System.err.println("(CL) "+s+".class : Failed to load!");
                System.err.println(e);
            } catch(ClassNotFoundException e) {
                System.err.println("(CL) "+s+".class : Failed to load!");
                System.err.println(e);
            }
        }
        if(loadedModules == 0) {
            System.out.println("(CL) No modules to load.");
        }
        return loadedModules;
    }

    private static boolean recompileModules() {
        try {
            Process reload = Runtime.getRuntime().exec(ROOT_DIR+"reload.sh");
            BufferedReader br = new BufferedReader(new InputStreamReader(reload.getInputStream()));
            String line;
            System.out.println("(RC) Starting recompilation of modules...");
            while ((line = br.readLine()) != null) {
                System.out.println("(RC) "+line);
            }
            reload.waitFor();
        } catch(IOException ex) {
            System.err.println("ERROR: Failed to run reload script: "+ex);
            System.err.println("Reported problem: "+ex);
            bot.sendMessage("Sorry, HiNux seems to have clogging problems, I caught in IOException while reloading modules.", false);
            return false;
        } catch(InterruptedException ex) {
            System.err.println("WARNING: I was interrupted before the compilation was done! NOT reloading modules.");
            System.err.println("Reported problem: "+ex);
            bot.sendMessage("I tried to reload modules, but was interrupted! Hmpf.", false);
            return false;
        }
        return true;
    }

    /**
     * The main method, starting the bot, connecting to the server and joining its main channel.
     *
     * @param args Command-line arguments, unused
     * @throws UnsupportedEncodingException very rarely since the encoding is almost never changed
     */
    public static void main(String[] args) throws UnsupportedEncodingException {

        // Redirect standard output to logfile
        if(!Debugger.DEBUG) {
            try {
                logfile.createNewFile();
                stdOut = new PrintStream(new BufferedOutputStream(new FileOutputStream(logfile)));
                System.setOut(stdOut);
                System.setErr(stdOut);
            } catch(IOException e) {
                System.err.println("WARNING: Unable to load or create logfile \""+logfile.toString()+"\" in default dir.\n" +
                        "Reported problem: " + e + "\n" +
                        "I will continue WITHOUT a logfile, and let stdout/stderr go straight to console.\n");
            }
        }

        // Load the SQL passwords from default files
        if(!PasswordManager.loadPasswords()) {
            System.err.println("WARNING: Unable to load one or more of the expected password files. " +
                    "I will continue, but modules dependant upon SQL may barf when they are used.\n");
        }

        // Load up the bot, enable debugging output, and specify encoding
        Grouphug.bot = new Grouphug();
        bot.setVerbose(true);
        bot.setEncoding(ENCODING);

        // Load up modules
        if(Debugger.DEBUG) {
            // When debugging, put the modules you want here!
            // Example:
            // modules.add(new grouphug.modules.ModuleName());
        } else {
            try {
                recompileModules();
                loadModules();
            } catch(NullPointerException ex) {
                System.err.println("\n" +
                        "Caught a NullPointerException while recompiling modules.\n\n" +
                        "This is usually caused by YOU trying to run/debug gh on your local machine.\n" +
                        "If that's the case, please take a look at the grouphug.util.Debugger class.");
                System.exit(-1);
            }
        }

        // Start own threads
        if(!Debugger.DEBUG) {
            SVNCommit.load(bot);
            new Thread(new LogFlusher(bot)).start();
        }

        // Save the nicks we want, in prioritized order
        //nicks.add("gh");
        nicks.add("gh`");
        nicks.add("hugger");
        nicks.add("klemZ");

        try {
            connect(bot, false);
        } catch(IrcException e) {
            // No idea how to handle this. So print the message and exit
            System.err.println(e.getMessage());
            stdOut.flush();
            System.exit(-1);
        } catch(IOException e) {
            // No idea how to handle this. So print the message and exit
            System.err.println(e.getMessage());
            stdOut.flush();
            System.exit(-1);
        }

        // Join the channel
        bot.joinChannel(CHANNEL);
    }

    /**
     * connect tries to connect the specified bot to the specified server, using the static nicklist
     *
     * @param bot The bot object that will try to connect
     * @param reconnecting true if we have lost a connection and are reconnecting to that
     * @throws IOException when this occurs in the pircbot connect(String) method
     * @throws IrcException when this occurs in the pircbot connect(String) method
     */
    private static void connect(Grouphug bot, boolean reconnecting) throws IOException, IrcException {
        int nextNick = 0;
        bot.setName(nicks.get(nextNick++));
        while(!bot.isConnected()) {
            try {
                if(reconnecting) {
                    Thread.sleep(RECONNECT_TIME);
                    bot.reconnect();
                } else {
                    bot.connect(Grouphug.SERVER);
                }
            } catch(NickAlreadyInUseException e) {
                // Nick was taken
                if(nextNick > nicks.size()-1) {
                    // If we've tried all the nicks, enable autonickchange
                    System.err.println("None of the specified nick(s) could be chosen, choosing automatically.");
                    bot.setAutoNickChange(true);
                } else {
                    // If not, try the next one
                    bot.setName(nicks.get(nextNick++));
                }
            } catch(InterruptedException e) {
                // do nothing, just try again once interrupted
            }
        }
        // start a thread for polling back our first nick if unavailable
        NickPoller.load(bot);
    }

    /**
     * Flushes the stdout buffer to the logfile
     */
    protected void flushLogs() {
        stdOut.flush();
    }

    /**
     * Convert HTML entities to their respective characters
     * @param str The unconverted string
     * @return The converted string
     */
    public static String entitiesToChars(String str) {
        str = str.replace("&amp;", "&");
        str = str.replace("&nbsp;", " ");
        str = str.replace("&#8216;", "'");
        str = str.replace("&#8217;", "'");
        str = str.replace("&#8220;", "\"");
        str = str.replace("&#8221;", "\"");
        str = str.replace("&#8230;", "...");
        str = str.replace("&#8212;", " - ");
        str = str.replace("&mdash;", " - ");
        str = str.replace("&quot;", "\"");
        str = str.replace("&apos;", "'");
        str = str.replace("&lt;", "<");
        str = str.replace("&gt;", ">");
        str = str.replace("&#34;", "\"");
        str = str.replace("&#39;", "'");
        str = str.replace("&laquo;", "«");
        str = str.replace("&lsaquo;", "‹");
        str = str.replace("&raquo;", "»");
        str = str.replace("&rsaquo;", "›");
        str = str.replace("&aelig;", "æ");
        str = str.replace("&Aelig;", "Æ");
        str = str.replace("&aring;", "å");
        str = str.replace("&Aring;", "Å");
        str = str.replace("&oslash;", "ø");
        str = str.replace("&Oslash;", "Ø");
        str = str.replace("&#228;", "ä");
        return str;
    }

    /**
     * I will not try to pretend like I know what this method does.
     * But it probably has something to do with fixing character encodings.
     * @param str The unconverted string
     * @return The attempted converted string
     */
    public static String fixEncoding(String str) {

        // lowercase iso-8859-1 encoded
        str = str.replace(new String(new char[] { (char)195, (char)352 }), "æ");
        str = str.replace(new String(new char[] { (char)195, (char)382 }), "ø");
        str = str.replace(new String(new char[] { (char)195, (char)165 }), "å");

        // uppercase iso-8859-1 encoded
        str = str.replace(new String(new char[] { (char)195, (char)134}), "Æ");
        str = str.replace(new String(new char[] { (char)195, (char)152}), "Ø");
        str = str.replace(new String(new char[] { (char)195, (char)195}), "Å");

        // not exactly sure what this is - supposed to be utf-8, not sure what happens really
        // not sure of the char values for Æ and Å, these are commented out, enable them when this gets applicable
        //str = str.replace(new String(new char[] { (char)195, (char)???}), "&AElig;");
        str = str.replace(new String(new char[] { (char)195, (char)732}), "Ø");
        //str = str.replace(new String(new char[] { (char)195, (char)???}), "&Aring;");

        return str;
    }
}
