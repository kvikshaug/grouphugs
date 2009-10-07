package grouphug;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;

import java.io.*;
import java.util.ArrayList;

/**
 * Grouphug
 *
 * A java-based IRC-bot created purely for entertainment purposes and as a personal excercise in design.
 * This bot extends the functionality of the well-designed PircBot, see http://www.jibble.org/
 *
 * The bot acts as a framework for modules that each serve their own purpose. With the help of SQL,
 * external web sites and clever imagination, the modules are used to entertain, insult, and/or inform the
 * users in its IRC channel.
 *
 * It is (supposed to be) easy to add a new module to the mix, and several utilities exist to make
 * writing a module as easy as possible.
 *
 * Some important concepts for the bot:
 * - It should never bother anyone unless it is clear that they want a response from it.
 * - It should never be unclear what a command or module does or intends to do. From a single !help trigger,
 *   a user should be able to dig down in detail and find out every interaction he/she is able to make to
 *   the bot, and what to be expected in return.
 *
 * The bot is currently maintained by most of the people hanging in #grouphugs @ efnet.
 * For more information, please join our channel or visit the web site: http://gh.kvikshaug.no/
 */

public class Grouphug extends PircBot {

    // Channel and server
    public static final String CHANNEL = "#grouphugs";
    public static final String SERVER = "irc.homelien.no";

    // The trigger characters (as Strings since startsWith takes String)
    public static final String MAIN_TRIGGER = "!";
    public static final String SPAM_TRIGGER = "@";
    public static final String HELP_TRIGGER = "help";

    // A list over all the nicknames we want
    protected static ArrayList<String> nicks = new ArrayList<String>();

    // The number of characters upon which lines are splitted
    // Note that the 512 max limit includes the channel name, \r\n, and probably some other stuff.
    // maxing out on 445 seems to be a reasonable amount, both ways.
    private static final int MAX_LINE_CHARS = 445;

    // How many lines we can send to the channel in one go without needing spam-trigger
    private static final int MAX_SPAM_LINES = 5;

    // How often to try to reconnect to the server when disconnected, in ms
    private static final int RECONNECT_TIME = 15000;

    // The file to log all messages to
    private static File logfile = new File("log-current");

    // A list over all loaded modules
    private static ArrayList<GrouphugModule> modules = new ArrayList<GrouphugModule>();

    // Used to specify if it is ok to spam a large message to the channel
    private static boolean spamOK = false;

    // A static reference and getter to our bot
    private static Grouphug bot;
    public static Grouphug getInstance() {
        return bot;
    }

    @Override
    protected void onMessage(String channel, String sender, String login, String hostname, String message) {

        // Old reboot/reload functions - this is strictly not necessary, but maybe these
        // should be reimplemented properly sometime?
        if(message.equals("!reboot") || message.equals("!reload")) {
            bot.sendMessage("Sorry, this functionality has been disabled. Patches are welcome though :)", false);
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
        // TODO if the message was unrecognized, say something like "hi, i'm a bot. visit
        // TODO the people in #grouphugs to ask about me, or say !help to learn what i can do."
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
            System.out.flush();
            System.err.flush();
            System.exit(-1);
        } catch(IOException e) {
            // No idea how to handle this. So print the message and exit
            System.err.println(e.getMessage());
            System.out.flush();
            System.err.flush();
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
     * The main method, starting the bot, connecting to the server and joining its main channel.
     *
     * @param args Command-line arguments, unused
     * @throws UnsupportedEncodingException very rarely since the encoding is almost never changed
     */
    public static void main(String[] args) throws UnsupportedEncodingException {

        // Redirect standard output to logfile
        try {
            System.out.println("Standard input will be redirected the following logfile:");
            System.out.println("'"+logfile.getAbsolutePath()+"'.");
            if(!logfile.createNewFile()) {
                System.out.println("Note: The logfile already exists, any existing data will be overwritten.");
            }
            PrintStream stdOut = new PrintStream(new BufferedOutputStream(new FileOutputStream(logfile)));
            System.setOut(stdOut);
            System.setErr(stdOut);
        } catch(IOException e) {
            System.err.println("WARNING: Unable to load or create logfile \""+logfile.toString()+"\" in default dir.\n" +
                    "Reported problem: " + e + "\n" +
                    "I will continue WITHOUT a logfile, and let stdout/stderr go straight to console.\n");
        }

        // Load up the bot, enable debugging output, and specify encoding
        Grouphug.bot = new Grouphug();
        bot.setVerbose(true);
        bot.setEncoding("UTF-8");

        // Load up modules
        modules.add(new grouphug.modules.Bofh());
        modules.add(new grouphug.modules.Confession());
        modules.add(new grouphug.modules.Decider());
        modules.add(new grouphug.modules.Define());
        modules.add(new grouphug.modules.EightBall());
        modules.add(new grouphug.modules.Factoid());
        modules.add(new grouphug.modules.Google());
        modules.add(new grouphug.modules.GoogleCalc());
        modules.add(new grouphug.modules.GoogleFight());
        modules.add(new grouphug.modules.IMDb());
        modules.add(new grouphug.modules.Insulter());
        modules.add(new grouphug.modules.IsSiteUp());
        modules.add(new grouphug.modules.Karma());
        modules.add(new grouphug.modules.Seen());
        modules.add(new grouphug.modules.Slang());
        modules.add(new grouphug.modules.Tracking());
        modules.add(new grouphug.modules.Translate());
        modules.add(new grouphug.modules.Upload());
        modules.add(new grouphug.modules.URLCatcher());
        modules.add(new grouphug.modules.WordCount());

        // Start own threads
        new Thread(new LogFlusher(bot)).start();

        // Save the nicks we want, in prioritized order
        nicks.add("gh");
        nicks.add("gh`");
        nicks.add("hugger");
        nicks.add("klemZ");

        try {
            connect(bot, false);
        } catch(IrcException e) {
            // No idea how to handle this. So print debug information and exit
            e.printStackTrace();
            System.out.flush();
            System.err.flush();
            System.exit(-1);
        } catch(IOException e) {
            // No idea how to handle this. So print debug information and exit
            e.printStackTrace();
            System.out.flush();
            System.err.flush();
            System.exit(-1);
        }

        // Join the channel
        bot.joinChannel(CHANNEL);
    }

    /**
     * This static method tries to connect the specified bot to the irc server.
     * The method contains some spaghetti code which serves the purpose of getting
     * the most wanted nick from the nicklist
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
}
