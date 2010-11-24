package no.kvikshaug.gh;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
    public List<String> CHANNELS = new ArrayList<String>();
    public static List<String> SERVERS = new ArrayList<String>();

    // The trigger characters (as Strings since startsWith takes String)
    public static final String MAIN_TRIGGER = "!";
    public static final String SPAM_TRIGGER = "@";
    public static final String HELP_TRIGGER = "help";

    // A list over all the nicknames we want
    protected static List<String> nicks = new ArrayList<String>();

    // The number of characters upon which lines are splitted
    // Note that the 512 max limit includes the channel name, \r\n, and probably some other stuff.
    // maxing out on 440 seems to be a reasonable amount, both ways.
    private static final int MAX_LINE_CHARS = 440;

    // How many lines we can send to the channel in one go without needing spam-trigger
    private static final int MAX_SPAM_LINES = 5;

    // How often to try to reconnect to the server when disconnected, in ms
    private static final int RECONNECT_TIME = 15000;

    // Used to specify if it is ok to spam a large message to the channel
    private static boolean spamOK = false;

    // Handles modules
    private static ModuleHandler moduleHandler;

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
            bot.sendMessageChannel(channel, "Sorry, this functionality has been disabled. Patches are welcome though :)");
            return;
        }

        // First, check for the universal normal help-trigger
        if(message.startsWith(MAIN_TRIGGER + HELP_TRIGGER)) {
            // we send the message, however trimming the help trigger itself
            moduleHandler.onHelp(channel, message.substring(MAIN_TRIGGER.length() + HELP_TRIGGER.length()).trim());
        }

        // First check if the message starts with a normal or spam-trigger
        if(message.startsWith(MAIN_TRIGGER) || message.startsWith(SPAM_TRIGGER)) {
            // Enable spam if triggered
            spamOK = message.startsWith(SPAM_TRIGGER);

            // But not for everyone
            if(spamOK && (sender.contains("icc") || login.contains("icc"))) {
                sendMessageChannel(channel, "icc, you are not allowed to use the spam trigger.");
                return;
            }

            // Now send the call to the module handler (stripping the trigger character)
            moduleHandler.onTrigger(channel, sender, login, hostname, message.substring(1));
        }

        moduleHandler.onMessage(channel, sender, login, hostname, message);
    }

    /**
     * This method is called whenever someone (possibly us) joins a channel
     * which we are on.
     * @param channel The channel which somebody joined.
     * @param sender The nick of the user who joined the channel.
     * @param login The login of the user who joined the channel.
     * @param hostname The hostname of the user who joined the channel.
     */
    @Override
    protected void onJoin(String channel, String sender, String login, String hostname) {
        moduleHandler.onJoin(channel, sender, login, hostname);
    }

    @Override
    protected void onPrivateMessage(String sender, String login, String hostname, String message) {
        // First, check for the universal normal help-trigger
        if(message.startsWith(MAIN_TRIGGER + HELP_TRIGGER)) {
            // we send the message, however trimming the help trigger itself
            moduleHandler.onHelp(sender, message.substring(MAIN_TRIGGER.length() + HELP_TRIGGER.length()).trim());
        }
    }

    @Override
    protected void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
        if (recipientNick.equalsIgnoreCase(getNick())) {
            joinChannel(channel);
            sendMessageChannel(channel, "sry :(");
        }
    }

    /**
     * This method is called whenever someone (possibly us) changes nick on any of the channels that we are on.
     * @param oldNick The old nick
     * @param login The login of the user
     * @param hostname The hostname of the user
     * @param newNick The new nick
     */
    @Override
    protected void onNickChange(String oldNick, String login, String hostname, String newNick) {
        moduleHandler.onNickChange(oldNick, login, hostname, newNick);
    }

    /**
     * Sends a message to the specified channel.
     *
     * The message will NOT be protected against spam.
     *
     * The messages are splitted by maximum line number characters and by the newline character (\n), then
     * each line is sent to the pircbot sendMessage function, which adds the lines to the outgoing message queue
     * and sends them at the earliest possible opportunity.
     *
     * @param channel - The channel where the message should be sent
     * @param message - The message to send
     */
    public void sendMessageChannel(String channel,String message) {
        sendMessageChannel(channel, message, false);
    }

    /**
     * Sends a message to the specified channel.
     *
     * If verifySpam is true, the message will not be sent if it is longer than Grouphug.MAX_SPAM_LINES,
     * but instead replaced with a message telling the user to use the spam trigger (@) instead.
     *
     * verifySpam should not be used if the output is random, because then using the spam trigger obviously
     * won't resend the message that was too long.
     *
     * The messages are splitted by maximum line number characters and by the newline character (\n), then
     * each line is sent to the pircbot sendMessage function, which adds the lines to the outgoing message queue
     * and sends them at the earliest possible opportunity.
     *
     * @param receiver - Either the channel to send to, or a nick to send a PM to
     * @param message - The message to send
     * @param verifySpam - true if verifying that spamming is ok before sending large messages
     */
    public void sendMessageChannel(String receiver, String message, boolean verifySpam) {

        // First create a list of the lines we will send separately.
        List<String> lines = new ArrayList<String>();

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
            sendMessage(receiver, "This would spam the channel with "+lines.size()+" lines, replace "+MAIN_TRIGGER+" with "+SPAM_TRIGGER+" if you really want that.");
            return;
        }

        // Finally send all the lines to the channel
        for(String line : lines) {
            this.sendMessage(receiver, line);
        }
    }




    public void parseConfig(){
        try {

            File xmlDocument = new File("props.xml");
            SAXBuilder saxBuilder = new SAXBuilder();
            Document jdomDocument = saxBuilder.build(xmlDocument);

            Element botNode = jdomDocument.getRootElement();

            List<Element> nicks = botNode.getChild("Nicks").getChildren();

            for(Element nick: nicks){
                String nickString = (String)nick.getValue();

                if(nickString.length() > 9 ){
                    nickString = nickString.substring(0, 9);
                }
                this.nicks.add(nickString);
            }

            List<Element> channelNodes = botNode.getChild("Channels").getChildren();

            for (Element e : channelNodes) {
                this.CHANNELS.add(e.getAttribute("chan").getValue());
            }

            List<Element> servers = botNode.getChild("Servers").getChildren();
            for(Element server: servers) {
                this.SERVERS.add((String)server.getValue());
            }
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The main method, starting the bot, connecting to the server and joining its main channel.
     *
     * @param args Command-line arguments, unused
     * @throws UnsupportedEncodingException very rarely since the encoding is almost never changed
     */
    public static void main(String[] args) throws UnsupportedEncodingException {

        // Load up the bot, enable debugging output, and specify encoding
        Grouphug.bot = new Grouphug();
        bot.setVerbose(true);
        bot.setEncoding("UTF-8");

        bot.parseConfig();
        moduleHandler = new ModuleHandler(bot);

        try {
            connect(bot);
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

        // Join the channels
        for (String channel : bot.CHANNELS) {
            bot.joinChannel(channel);
        }
    }

    private static void connect(Grouphug bot) throws IOException, IrcException {
        final String prefix = "[connection] ";

        for(String server : SERVERS) {
            System.out.print("\n" + prefix + "Connecting to " + server + "...");
            for(String nick : nicks) {
                bot.setName(nick);
                System.out.println("\n" + prefix + " -> Trying nick '" + nick + "'...");
                try {
                    bot.connect(server);
                    System.out.println("\n" + prefix + "Connected as '" + nick + "'!");
                    break;
                } catch(NickAlreadyInUseException ignored) {}
            }
            if(!bot.isConnected()) {
                bot.setAutoNickChange(true);
                bot.connect(server);
            }
            if(bot.isConnected()) {
                break;
            }
        }
        if(!bot.isConnected()) {
            System.err.println("\n" + prefix + "Noes! Couldn't connect to any of the specified servers!");
        }
        // start a thread for polling back our first nick if unavailable
        NickPoller.load(bot);
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
            while(!isConnected()) {
                try { Thread.sleep(RECONNECT_TIME); } catch(InterruptedException ignored) { }
                Grouphug.connect(this);
            }
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
        for (String channel : this.CHANNELS) {
            this.joinChannel(channel);
        }
    }

}
