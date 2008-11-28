package grouphug.modules;

import grouphug.GrouphugModule;
import grouphug.Grouphug;
import grouphug.SQL;

import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Pattern;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;

/**
 * Need a quick excuse to shut a luser up? Look no further, the BOFH module will assist you.
 *
 * User: sunn/oyvindio
 * Date: 27.nov.2008
 * Time: 23:34:16
 */
public class Bofh implements GrouphugModule
{
    private static final String RANDOM_TRIGGER = "bofh";
    private static final String SPECIFIC_TRIGGER = RANDOM_TRIGGER + " \\d+";
    public static final String HELP_TRIGGER = RANDOM_TRIGGER;
    private static Grouphug bot;

    private Random random;
    private ArrayList<String> excuses;

    public Bofh(Grouphug grouphug)
    {
        bot = grouphug;
        random = new Random(System.nanoTime());
        initExcuses();
    }

    /**
     * Initializes the excuses arraylist by fetching all rows from the database and filling the arraylist with their
     * contents
     */
    private void initExcuses()
    {
        SQL sql = new SQL();
        excuses = new ArrayList<String>(500); // there's just short of 500 rows in the db at the moment.

        try
        {
            sql.connect();
            sql.query("SELECT `excuse` FROM gh_bofh;");

            int i = 1;
            while(sql.getNext())
            {
                excuses.add("BOFH excuse #" + i + ": " +sql.getValueList()[0]);
                i++;
            }
            excuses.trimToSize();
        }
        catch (SQLSyntaxErrorException ssee)
        {
            System.err.println("BOFH startup: SQL syntax error - MySQL said: " + ssee);
        }
        catch (SQLException se)
        {
            System.err.println("BOFH startup: SQL exception - MySQL said: " + se);
        }
        finally
        {
            sql.disconnect();
        }

        assert (excuses != null) : "BOFH startup: Init failed!";
    }

    /**
     * This method is called by the bot when someone sends a chat line that starts with the trigger command.
     * It is then up to each module to parse the line and, if applicable, respond.
     *
     * @param channel  - The channel to which the message was sent.
     * @param sender   - The nick of the person who sent the message.
     * @param login    - The login of the person who sent the message.
     * @param hostname - The hostname of the person who sent the message.
     * @param message  - The actual message sent to the channel.
     */
    public void trigger(String channel, String sender, String login, String hostname, String message)
    {
        String reply = null;
        // check for specific trigger first.
        if (message.matches(SPECIFIC_TRIGGER))
        {
            try
            {
                // RANDOM_TRIGGER.length() + 1 to account for the extra space character before the number in SPECIAL_TRIGGER
                int number  = Integer.parseInt(message.substring(RANDOM_TRIGGER.length() + 1));

                if (number < 1 || number > excuses.size())
                    reply = "Invalid number. Valid numbers are 1-" + excuses.size() + ".";
                else
                    reply = excuses.get(number-1); // 0-indexed, hence the -1.
            }
            catch (NumberFormatException nfe)
            {
                reply = "That's not a number, is it now?";
            }
        }
        else if (message.startsWith(RANDOM_TRIGGER))
        {
            reply = excuses.get(random.nextInt(excuses.size()));
        }
        if (reply != null)
            bot.sendMessage(reply, false);
    }

    /**
     * This is called when the user is believed to ask for general help about the bot.
     * All modules should return a small string stripped for whitespace, containing a lowercase-representation
     * of its name, that could be used with the special help trigger, to get more specific help of the module.
     *
     * @param channel  - The channel to which the message was sent.
     * @param sender   - The nick of the person who sent the message.
     * @param login    - The login of the person who sent the message.
     * @param hostname - The hostname of the person who sent the message.
     * @param message  - The actual message sent to the channel.
     * @return the name of the current module that will trigger a help-message on the special help trigger
     */
    public String helpMainTrigger(String channel, String sender, String login, String hostname, String message)
    {
        return "bofh";
    }

    public void specialTrigger(String channel, String sender, String login, String hostname, String message)
    {
        // We're not using this.
    }

    /**
     * This is called when the user is believed to ask for specific help of a module.
     * The module should parse the message, and if it includes the trigger that would be sent back in the
     * helpMainTrigger method, then this module should reply, with notices in pm to the sender,
     * all info about how this module is used, under the presumtion that this is the only module replying.
     *
     * @param channel  - The channel to which the message was sent.
     * @param sender   - The nick of the person who sent the message.
     * @param login    - The login of the person who sent the message.
     * @param hostname - The hostname of the person who sent the message.
     * @param message  - The actual message sent to the channel, stripped for the triggers + 1 char for space
     * @return boolean - true if the module reacted to the message, false otherwise
     */
    public boolean helpSpecialTrigger(String channel, String sender, String login, String hostname, String message)
    {
        if (message.equals(HELP_TRIGGER))
        {
            bot.sendNotice(sender, "BOFH - Fend off lusers with Bastard Operator From Hell excuses for their system \"problems\".");
            bot.sendNotice(sender, "Usage:");
            bot.sendNotice(sender, Grouphug.MAIN_TRIGGER + RANDOM_TRIGGER + " returns a random excuse.");
            bot.sendNotice(sender, Grouphug.MAIN_TRIGGER + SPECIFIC_TRIGGER + " returns an excuse by number. (\\d+ means any digit, 1-n times)");
            return true;
        }

        return false;
    }
}
