package grouphug.modules;

import grouphug.GrouphugModule;
import grouphug.Grouphug;
import grouphug.SQL;

import java.sql.SQLException;

import com.sun.corba.se.impl.protocol.giopmsgheaders.Message;

public class Karma implements GrouphugModule {

    private static Grouphug bot;
    private static final String TRIGGER_HELP = "karma";
    private static final String TRIGGER = "karma ";
    private static final String TRIGGER_TOP = "karmatop";
    private static final String TRIGGER_BOTTOM = "karmabottom";
    private static final String TRIGGER_RESET = "!reset";

    private static final int LIMIT = 5; // how many items to show in karmatop/karmabottom

    private static final String KARMA_DB = "gh_karma";

    public Karma(Grouphug bot) {
        Karma.bot = bot;
    }

    public String helpMainTrigger(String channel, String sender, String login, String hostname, String message) {
        return TRIGGER_HELP;
    }

    public boolean helpSpecialTrigger(String channel, String sender, String login, String hostname, String message) {
        if(message.equals(TRIGGER_HELP)) {
            bot.sendNotice(sender, "Karma: Increase, decrease, or show an objects karma.");
            bot.sendNotice(sender, "  " + Grouphug.MAIN_TRIGGER + TRIGGER + "<object>");
            bot.sendNotice(sender, "  <object>++");
            bot.sendNotice(sender, "  <object>--");
            bot.sendNotice(sender, "  " + Grouphug.MAIN_TRIGGER + TRIGGER_TOP);
            bot.sendNotice(sender, "  " + Grouphug.MAIN_TRIGGER + TRIGGER_BOTTOM);
            bot.sendNotice(sender, "  " + Grouphug.MAIN_TRIGGER + TRIGGER + "<object> "+ TRIGGER_RESET);
            return true;
        }
        return false;
    }

    public void specialTrigger(String channel, String sender, String login, String hostname, String message) {
        if(message.endsWith("++") || message.endsWith("++;"))
            add(sender, message.substring(0, message.length()-2), 1);
        else if(message.endsWith("--") || message.endsWith("--;"))
            add(sender, message.substring(0, message.length()-2), -1);
        else if(message.endsWith("reset"))
        	add(sender, message.substring(0, message.length()-5), 0);
    }

    public void trigger(String channel, String sender, String login, String hostname, String message) {

        // First, check for triggers: keywords, ++, --
    	if(message.startsWith(TRIGGER) && message.endsWith(TRIGGER_RESET))
    		add(sender, message.substring(6, message.length()-6), 0);
    	else if(message.startsWith(TRIGGER))
            print(message.substring(TRIGGER.length()));
        else if(message.equals(TRIGGER_TOP))
            showScore(true);
        else if(message.equals(TRIGGER_BOTTOM))
            showScore(false);

    }

    private String htmlEntitiesToNorwegianChars(String str) {
        str = str.replace("&aelig;", "æ");
        str = str.replace("&oslash;", "ø");
        str = str.replace("&aring;", "å");
        str = str.replace("&AElig;", "Æ");
        str = str.replace("&Oslash;", "Ø");
        str = str.replace("&Aring;", "Å");
        return str;
    }

    private String norwegianCharsToHtmlEntities(String str) {
        str = Grouphug.fixEncoding(str);
        str = str.replace("æ", "&aelig;");
        str = str.replace("ø", "&oslash;");
        str = str.replace("å", "&aring;");
        str = str.replace("Æ", "&AElig;");
        str = str.replace("Ø", "&Oslash;");
        str = str.replace("Å", "&Aring;");
        return str;
    }

    private void print(String name) {
        String sqlName = norwegianCharsToHtmlEntities(name);
        KarmaItem ki;
        try {
            ki = find(sqlName);
        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            bot.sendMessage(name+" has probably bad karma, because an SQL error occured.", false);
            return;
        }
        if(ki == null)
            bot.sendMessage(name+" has neutral karma.", false);
        else
            bot.sendMessage(ki.getName()+" has "+ki.getKarma()+" karma.", false);
    }

    private void add(String sender, String name, int karma) {
        if(name.equals(sender)) {
            bot.sendMessage(sender+", self karma is bad karma.", false);
            return;
        }

        String sqlName = norwegianCharsToHtmlEntities(name);

        SQL sql = new SQL();
        try {
            sql.connect();
            // we sleep for a little while, in case the user is very fast - to avoid duplicate inserts
            try {
                Thread.sleep(600);
            } catch(InterruptedException e) {
                // interrupted, ok, just continue
            }
            KarmaItem ki = find(sqlName);
            if(ki == null)
                sql.query("INSERT INTO "+KARMA_DB+" (name, value) VALUES ('"+sqlName+"', '"+karma+"');");
            else if(karma == 0)
                sql.query("UPDATE "+KARMA_DB+" SET value='"+karma+"' WHERE id='"+ki.getID()+"';");
            else
                sql.query("UPDATE "+KARMA_DB+" SET value='"+(ki.getKarma() + karma)+"' WHERE id='"+ki.getID()+"';");
        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            bot.sendMessage("Sorry, an SQL error occurred.", false);
        } finally {
            sql.disconnect();
        }
    }

    /**
     * Finds a karma-item in the DB based on its name. Returns null if no item is found.
     * @param karma The karma string to search for in the DB
     * @return a KarmaItem-object of the item found in the DB, or null if no item was found
     * @throws SQLException - if an SQL error occured
     */
    private KarmaItem find(String karma) throws SQLException {
        SQL sql = new SQL();
        sql.connect();
        sql.query("SELECT id, name, value FROM "+KARMA_DB+" WHERE name='"+karma+"';");
        if(!sql.getNext()) {
            return null;
        }
        Object[] values = sql.getValueList();
        sql.disconnect();
        return new KarmaItem((Integer)values[0], htmlEntitiesToNorwegianChars((String)values[1]), (Integer)values[2]);
    }

    private void showScore(boolean top) {
        SQL sql = new SQL();
        String reply;
        if(top)
            reply = "Top five karma winners:\n";
        else
            reply = "Bottom five karma losers:\n";
        try {
            sql.connect();
            String query = "SELECT name, value FROM "+KARMA_DB+" ORDER BY value ";
            if(top)
                query += "DESC ";
            query += "LIMIT "+LIMIT+";";
            sql.query(query);
            int place = 1;
            while(sql.getNext()) {
                Object[] values = sql.getValueList();
                reply += (place++)+". "+htmlEntitiesToNorwegianChars((String)values[0])+" ("+values[1]+")\n";
            }
            if(top)
                reply += "May their lives be filled with sunlight and pink stuff.";
            else
                reply += "May they burn forever in the pits of "+ Grouphug.getChannel()+".";
            bot.sendMessage(reply, false);
        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            bot.sendMessage("Sorry, an SQL error occured.", false);
        }
    }

    private static class KarmaItem {

        private int ID;
        private String name;
        private int karma;

        public int getID() {
            return ID;
        }

        public String getName() {
            return name;
        }

        public int getKarma() {
            return karma;
        }

        public KarmaItem(int ID, String name, int karma) {
            this.ID = ID;
            this.name = name;
            this.karma = karma;
        }

        public String toString() {
            if(karma == 0)
                return "neutral";
            else
                return ""+karma;
        }
    }

}