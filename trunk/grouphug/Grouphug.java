package grouphug;

import org.jibble.pircbot.*;

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
public class Grouphug extends PircBot {

    protected static final String CHANNEL = "#grouphugs";     // The main channel
    protected static final String SERVER = "irc.homelien.no"; // The main IRC server

    // The number of characters upon which lines are splitted
    protected static final int MAX_LINE_CHARS = 420;

    // How often to try to reconnect to the server when disconnected, in ms
    protected static final int RECONNECT_TIME = 15000;

    // The file to log all messages to
    protected static File logfile = new File("log-current");

    // The standard outputstream
    protected static PrintStream stdOut;

    // A list over all loaded modules
    private static ArrayList<GrouphugModule> modules = new ArrayList<GrouphugModule>();

    // A list over all the nicknames we want
    private static ArrayList<String> nicks = new ArrayList<String>();

    // TODO: this should be done differently
    // A temporary static variable
    private static boolean spamOK = false;



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
    protected void onMessage(String channel, String sender, String login, String hostname, String message) {

        if(message.startsWith("@")) {
            if(sender.contains("icc") || login.contains("icc")) {
                sendMessage("icc, you are not allowed to surpass spam-commands.");
                return;
            }
            spamOK = true;
            message = message.substring(1);
        } else {
            spamOK = false;
        }

        // For each "module", call the trigger-method with the sent message
        for(GrouphugModule m : modules) {
            m.trigger(this, channel, sender, login, hostname, message);
        }

        // A few hardcoded funnies
        // TODO: make factoid? "idiot bot is <action>pisses all over $sender" -> saved in db, triggered by own module
        if(message.equalsIgnoreCase("idiot bot"))
            sendAction(CHANNEL, "pisses all over "+sender);
        if(message.equalsIgnoreCase("homo bot"))
            sendAction(CHANNEL, "picks up the soap");
        if(message.equalsIgnoreCase("goosh"))
            sendMessage(CHANNEL, "http://youtube.com/watch?v=xrhLdDIQ5Kk");
        if(message.equalsIgnoreCase("fuck it"))
            sendMessage(CHANNEL, "WE'LL DO IT LIVE!");
        if (message.equalsIgnoreCase("!insult"))
            sendMessage(CHANNEL, sender + ", you fail at life.");
        if (message.equalsIgnoreCase("homos"))
            sendMessage(CHANNEL, "homos are always mad");

        stdOut.flush();
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
    protected void onDisconnect() {
        // Constantly try to reconnect
        while (!isConnected()) {
            try {
                Thread.sleep(RECONNECT_TIME);
                reconnect();
            } catch (InterruptedException e) {
                // do nothing; try again in specified time
            } catch(Exception e) {
                // TODO - handle these exceptions
            }
        }
    }

    /**
     * Sends a message to a channel or a private message to a user.
     *
     * The messages are splitted by maximum line number characters and by the newline character (\n), then
     * each line is sent to the pircbot sendMessage function, which adds the lines to the outgoing message queue
     * and sends them at the earliest possible opportunity.
     *
     * @param message - The message to send
     */
    protected void sendMessage(String message) {
        // First create a list of the lines we will send separately.
        ArrayList<String> lines = new ArrayList<String>();

        // If the message is longer than max line chars, separate them
        while(message.length() > Grouphug.MAX_LINE_CHARS) {
            lines.add(message.substring(0, Grouphug.MAX_LINE_CHARS));
            message = message.substring(Grouphug.MAX_LINE_CHARS);
        }
        lines.add(message);

        // For each line, split all \n into new lines
        // TODO: optimize; this line separator is quick n dirty
        for(int i=0; i<lines.size(); i++) {
            for(int j=0; j < lines.get(i).length(); j++) {
                if(lines.get(i).charAt(j) == '\n') {
                    lines.add((i+1), lines.get(i).substring(j+1));
                    lines.set(i, lines.get(i).substring(0, j));
                }
            }
        }

        // TODO: if we for some reason are to send an ENORMOUS amount of lines, maybe we should throw an exception or
        // TODO: something? or at least warn about the pending spam?

        if(!spamOK && lines.size() > 3) {
            sendMessage(Grouphug.CHANNEL, "This would spam the channel with "+lines.size()+" lines, start command with @ to override.");
            return;
        }

        // Now, for each line we have in lines, send them to the channel
        for(String line : lines) {
            // Empty lines may appear, so we skip them
            if(!line.equals("")) {
              this.sendMessage(Grouphug.CHANNEL, line);
            }
        }
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
            SQL.loadPassword("mysqlpw.txt");
        } catch(IOException e) {
            System.err.println("Fatal error: Could not load MySQL-password file.");
            e.printStackTrace();
            System.exit(-1);
        }

        // Load up the bot and enable debugging output
        Grouphug bot = new Grouphug();
        bot.setVerbose(true);

        // Load up modules
        // TODO - should be done differently
        modules.add(new Slang());
        modules.add(new Confession());
        modules.add(new Karma());
        SVNCommit.load(bot);

        // Save the nicks we will try
        nicks.add("gh");
        nicks.add("hugger");
        nicks.add("klemZ");

        // TODO create thread for polling back first nick if unavailable

        // Try connecting to the server
        boolean connected = false;
        boolean autoNick = false;
        int nextNick = 0;
        while(!connected) {
            try {
                if(!autoNick)
                    bot.setName(nicks.get(nextNick));
                bot.connect("irc.homelien.no");
                connected = true;
            } catch(IndexOutOfBoundsException e) {
                // We reached the end of the list, so enable autonickchange and retry
                System.err.println("None of the specified nick(s) could be chosen, choosing automatically.");
                bot.setAutoNickChange(true);
                autoNick = true;
            } catch(NickAlreadyInUseException e) {
                // Nick was taken, try the next in the list
                nextNick++;
            } catch(IrcException e) {
                // No idea how to handle this. So print the message and exit
                System.err.println(e.getMessage());
                System.exit(-1);
            } catch(IOException e) {
                // No idea how to handle this. So print the message and exit
                System.err.println(e.getMessage());
                System.exit(-1);
            }
        }

        // Join the channel
        bot.joinChannel(CHANNEL);
    }
}