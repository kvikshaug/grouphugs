package grouphug;

import org.jibble.pircbot.*;
import grouphug.modules.*;

import java.util.ArrayList;
import java.io.*;

/**
 * GrouphugBot.java
 *
 * A java-based IRC-bot created purely for entertainment purposes and as a personal excercise in design.
 *
 * Instead of describing the current design of the bot, which hardly can be called design at all, I will rather
 * explain the vision for it:
 *
 * The bot is to be able to load function modules, that are triggered for each message sent to the bot's channel,
 * and it is up to the modules to react to a message. The bot should never bother anyone unless it is clear that they
 * want a response from it.
 *
 * As an example, the original function module was something that reacted on the trigger word "!gh" (may have changed),
 * and on this request fetched a random grouphug confession from the http://grouphug.us/ site. Further functionality
 * was added for searching for a specific confession topic, getting the newest confession, and so forth.
 *
 * The grouphug bot was originally started by Alex Kvikshaug and hopefully continued as an SVN project
 * by the guys currently hanging in #grouphugs @ efnet.
 *
 * The bot extends the functionality of the well-designed PircBot, see http://www.jibble.org/
 */

// TODO - write a websiteconnection-class - for easier use - and to avoid copypasta code (Google/Define/Tracking/Confession)
// TODO - bash for #grouphugs
// TODO - tlf module
// TODO - review access modifiers everywhere according to the new 'modules' package
// TODO - current logfile flushes are kind of fugly

public class Grouphug extends PircBot {

    static final String CHANNEL = "#grouphugs";     // The main channel
    static final String SERVER = "irc.homelien.no"; // The main IRC server
    static final String ENCODING = "ISO8859-15";    // Character encoding to use when communicating with the IRC server.

    public static String getChannel() {
        return CHANNEL;
    }

    // The number of characters upon which lines are splitted
    // Note that the 512 max limit includes the channel name, \r\n, and probably some other stuff.
    // maxing out on 450 seems to be a reasonable amount, both ways.
    private static final int MAX_LINE_CHARS = 450;

    // How many lines we can send to the channel in one go without needing spam-trigger
    private static final int MAX_SPAM_LINES = 5;

    // How often to try to reconnect to the server when disconnected, in ms
    private static final int RECONNECT_TIME = 15000;

    // The file to log all messages to
    private static File logfile = new File("log-current");

    // The standard outputstream
    private static PrintStream stdOut;

    // A list over all loaded modules
    private static ArrayList<GrouphugModule> modules = new ArrayList<GrouphugModule>();

    // A list over all the nicknames we want
    protected static ArrayList<String> nicks = new ArrayList<String>();

    // Used to specify if it is ok to spam a large message to the channel 
    static boolean spamOK = false;

    // The trigger characters (as Strings since startsWith takes String)
    public static final String MAIN_TRIGGER = "!";
    public static final String SPAM_TRIGGER = "@";

    private static final String HELP_TRIGGER = "help";

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

        // Let's do a logfile flush when someone sends a message..
        stdOut.flush();
    }

    @Override
    protected void onPrivateMessage(String sender, String login, String hostname, String message) {
        // Help triggers will also activate in PM
        checkForHelpTrigger(null, sender, login, hostname, message);
    }

    private void checkForHelpTrigger(String channel, String sender, String login, String hostname, String message) {
        // First, check for the universal normal help-trigger
        if(message.equals(MAIN_TRIGGER + HELP_TRIGGER)) {
            // Remember that if the line is > MAX_LINE_CHARS, it will *automatically* be split
            // over several lines in the sendMessage() method, so we don't have to do that here
            sendNotice(sender, "Currently implemented modules on "+this.getNick()+":");
            String helpString = "";
            for(GrouphugModule m : modules) {
                helpString += ", ";
                helpString += m.helpMainTrigger(channel, sender, login, hostname, message);
            }
            // Remove the first comma
            helpString = helpString.substring(2);
            sendNotice(sender, helpString);
            sendNotice(sender, "Use \"!help <module>\" for more specific info. This will also work in PM.");
        }
        // if not, check if help is triggered with a special module
        else if(message.startsWith(MAIN_TRIGGER+HELP_TRIGGER+" ")) {
            boolean replied = false;
            for(GrouphugModule m : modules) {
                if(m.helpSpecialTrigger(channel, sender, login, hostname, message.substring(MAIN_TRIGGER.length() + HELP_TRIGGER.length() + 1)))
                    replied = true;
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

        // Let's do a logfile flush when we've sent data to the server..
        stdOut.flush();
    }

    // Let's do a logfile flush after every server ping..
    @Override
    protected void onServerPing(String response) {
        super.onServerPing(response);
        stdOut.flush();
    }

    /**
     * The main method, starting the bot, connecting to the server and joining its main channel.
     *
     * @param args - Command-line arguments
     */
    public static void main(String[] args) {

        // Redirect standard output to logfile
        try {
            logfile.createNewFile();
            stdOut = new PrintStream(new BufferedOutputStream(new FileOutputStream(logfile)));
            System.setOut(stdOut);
            System.setErr(stdOut);
        } catch(IOException e) {
            System.err.println("Fatal error: Unable to load or create logfile \""+logfile.toString()+"\" in default dir.");
            e.printStackTrace();
            System.exit(-1);
        }

        // Load the SQL password from file
        try {
            SQL.loadPassword("pw/hinux");
        } catch(IOException e) {
            System.err.println("Fatal error: Could not load MySQL-password file.");
            System.err.println(e.getMessage());
            System.err.println(e.getCause());
            e.printStackTrace();
            stdOut.flush();
            System.exit(-1);
        }

        // Load up the bot and enable debugging output
        Grouphug bot = new Grouphug();
        bot.setVerbose(true);

        // Tell the bot to use ISO8859-15
        try {
            bot.setEncoding(ENCODING);
        }
        catch (UnsupportedEncodingException e) {
            bot.sendMessage(Grouphug.CHANNEL, "Failed to set character encoding " + ENCODING);
        }

        // Load up modules
        // TODO - should be done differently?
        modules.add(new Confession(bot));
        modules.add(new Slang(bot));
        modules.add(new Karma(bot));
        modules.add(new Google(bot));
        modules.add(new Dinner(bot));
        modules.add(new WeatherForecast(bot));
        modules.add(new Define(bot));
        modules.add(new Tracking(bot));
        modules.add(new Cinema(bot));
        modules.add(new IMDb(bot));
        modules.add(new Factoid(bot));
        Grouphug.loadGrimstuxPassword();
        SVNCommit.load(bot);

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


    public static void loadGrimstuxPassword() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File("pw/narvikdata")));
            String pw = reader.readLine();
            reader.close();

            if(pw.equals(""))
              throw new FileNotFoundException("No data extracted from MySQL password file!");

            Dinner.SQL_PASSWORD = pw;
            Cinema.SQL_PASSWORD = pw;
            WeatherForecast.SQL_PASSWORD = pw;
        } catch(IOException e) {
            // Do nothing - SQL_PASSWORD will be empty, and we will detect the error upon usage
        }
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
        str = str.replace("&quot;", "\"");
        str = str.replace("&apos;", "'");
        str = str.replace("&lt;", "<");
        str = str.replace("&gt;", ">");
        str = str.replace("&#34;", "\"");
        str = str.replace("&#39;", "'");
        return str;
    }

    /**
     * This attempts to convert non-regular æøåÆØÅ's to regular ones. Or something.
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
