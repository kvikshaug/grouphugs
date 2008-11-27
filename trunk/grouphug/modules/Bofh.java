package grouphug.modules;

import grouphug.GrouphugModule;
import grouphug.Grouphug;
import grouphug.SQL;

import java.util.ArrayList;
import java.util.Random;
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
    private static Grouphug bot;
    Random r;
    private ArrayList<String> excuses;

    public Bofh(Grouphug grouphug)
    {
        bot = grouphug;
        r = new Random(System.nanoTime());
        initExcuses();
    }

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

        assert (excuses != null) : "BOFH startup: Init failed!";
    }

    private String getRandomExcuse()
    {
        return excuses.get(r.nextInt(excuses.size()));
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
        if (message.startsWith(RANDOM_TRIGGER))
        {
            reply = getRandomExcuse();
        }
        
        if (reply != null)
            bot.sendMessage(reply, false);
    }

    /**
     * This is called when the user is believed to ask for general help about the bot.
     * All modules should return a small string stripped for whitespace, containing a lowercase-representation
     * of its name, that could be used with the special help trigger, to get more specific help of the module.
     * <p/>
     * Example: If the bot's name is SuperModule, the string could be "super", so that on "!help super", this
     * bot would reply with how the supermodule is used.
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
        return "Hepl?";
    }

    /**
     * This method is similar to the <code>trigger</code> method, but is called when the chat line contains
     * no specific trigger command. The module may still choose to parse this, e.g. the karma module fetching
     * up sentences ending with ++/--, but be careful as an important part of this bot is not to bother anyone
     * unless specifically requested.
     *
     * @param channel  - The channel to which the message was sent.
     * @param sender   - The nick of the person who sent the message.
     * @param login    - The login of the person who sent the message.
     * @param hostname - The hostname of the person who sent the message.
     * @param message  - The actual message sent to the channel.
     */
    public void specialTrigger(String channel, String sender, String login, String hostname, String message)
    {
        // We're not using this.
    }

    /**
     * This is called when the user is believed to ask for specific help of a module.
     * The module should parse the message, and if it includes the trigger that would be sent back in the
     * helpMainTrigger method, then this module should reply, with notices in pm to the sender,
     * all info about how this module is used, under the presumtion that this is the only module replying.
     * Example output:
     * <p/>
     * SuperModule 1.1 - Does Super Magic Upon Request
     * - Triggered by: !super
     * - Alternative trigger: !superduper
     * (More info..?)
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
        return false; // We're not using this.
    }
}
