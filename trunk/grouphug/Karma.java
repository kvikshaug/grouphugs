package grouphug;

import java.sql.SQLException;

public class Karma implements GrouphugModule {

    private static final String TRIGGER = "karma ";
    private static final String TRIGGER_TOP = "karmatop";
    private static final String TRIGGER_BOTTOM = "karmabottom";

    private static final int LIMIT = 5; // how many items to show in karmatop/karmabottom

    private static final String KARMA_DB = "gh_karma";

    public void trigger(Grouphug bot, String channel, String sender, String login, String hostname, String message) {

        // First, check for triggers: keywords, ++, -- 
        if(message.startsWith(TRIGGER))
            print(bot, message.substring(TRIGGER.length()));
        else if(message.equals(TRIGGER_TOP))
            showScore(bot, true);
        else if(message.equals(TRIGGER_BOTTOM))
            showScore(bot, false);
        else if(message.endsWith("++"))
            add(bot, sender, message.substring(0, message.length()-2), 1);
        else if(message.endsWith("--"))
            add(bot, sender, message.substring(0, message.length()-2), -1);

    }

    private void print(Grouphug bot, String name) {
        KarmaItem ki;
        try {
            ki = find(name);
        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            bot.sendMessage(name+" has probably bad karma, because an SQL error occured.");
            return;
        }
        if(ki == null)
            bot.sendMessage(name+" has neutral karma.");
        else
            bot.sendMessage(name+" has "+ki+" karma.");
    }

    private void add(Grouphug bot, String sender, String name, int karma) {
        if(name.equals(sender)) {
            bot.sendMessage(sender+", self karma is bad karma.");
            return;
        }
          
        SQL sql = new SQL();
        try {
            sql.connect();
            // we sleep for a little while, in case the user is very fast - to avoid duplicate inserts
            try {
                Thread.sleep(600);
            } catch(InterruptedException e) {
                // interrupted, ok, just continue
            }
            KarmaItem ki = find(name);
            if(ki == null) {
                sql.query("INSERT INTO "+KARMA_DB+" (name, value) VALUES ('"+name+"', '"+karma+"');");
            } else {
                sql.query("UPDATE "+KARMA_DB+" SET value='"+(ki.getKarma() + karma)+"' WHERE id='"+ki.getID()+"';");
            }
        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            bot.sendMessage("Sorry, an SQL error occurred.");
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
        sql.query("SELECT id, name, value FROM "+KARMA_DB+";");
        sql.getNext();
        Object[] values = sql.getValueList();
        sql.disconnect();
        if((values[1]).equals(karma)) {
            return new KarmaItem((Integer)values[0], (String)values[1], (Integer)values[2]);
        }
        return null;
    }

    private void showScore(Grouphug bot, boolean top) {
        Grouphug.spamOK = true;
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
                reply += (place++)+". "+values[0]+" ("+values[1]+")\n";
            }
            if(top)
                reply += "May their lives be filled with sunlight and pink stuff.";    
            else
                reply += "May they burn forever in the pits of "+ Grouphug.CHANNEL+".";
            bot.sendMessage(reply);
        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            bot.sendMessage("Sorry, an SQL error occured.");
        }
    }
}