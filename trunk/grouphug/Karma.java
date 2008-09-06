package grouphug;

public class Karma implements GrouphugModule {

    // TODO karmatop, karmabottom

    private static final String TRIGGER = "karma ";
    private static final String KARMA_DB = "gh_karma";

    public void trigger(Grouphug bot, String channel, String sender, String login, String hostname, String message) {

        // First, check for triggers: keywords, ++, -- 
        if(message.startsWith(TRIGGER))
            print(bot, message.substring(TRIGGER.length()));
        else if(message.endsWith("++"))
            add(bot, message.substring(0, message.length()-2), 1);
        else if(message.endsWith("--"))
            add(bot, message.substring(0, message.length()-2), -1);

    }

    private void print(Grouphug bot, String name) {
        KarmaItem ki = find(name, null);
        if(ki == null) {
            bot.sendMessage(name+" has neutral karma.");
        } else {
            String karmaStr;
            if(ki.getKarma() == 0)
                karmaStr = "neutral";
            else
                karmaStr = ""+ki.getKarma();

            bot.sendMessage(name+" has "+karmaStr+" karma.");
        }
    }

    private void add(Grouphug bot, String name, int karma) {
        SQL sql = new SQL();
        if(!sql.connect()) {
            System.err.println("Couldn't connect to the SQL database!");
            bot.sendMessage("ERROR: Unable to connect to SQL database.");
            return;
        }

        // TODO fugly fugly hack; can also be null on errors :( even tho we use this method's sql connection
        // TODO assumes that null just means "not found"
        KarmaItem ki = find(name, sql);
        if(ki == null) {
            sql.query("INSERT INTO "+KARMA_DB+" (name, value) VALUES ('"+name+"', '"+karma+"');");
        } else {
            sql.query("UPDATE "+KARMA_DB+" SET value='"+(ki.getKarma() + karma)+"' WHERE id='"+ki.getID()+"';");
        }
        sql.disconnect();
    }

    /**
     * Finds a karma-item in the DB based on its name. Returns null if no item is found.
     * @param karma The karma string to search for in the DB
     * @param sql Optional SQL object. If nullpointed, a custom SQL connection is made, if not, the provided SQL object is used.
     * @return a KarmaItem-object of the item found in the DB, or null if no item was found or an error occured (TODO should rather throw an exception)
     */
    private KarmaItem find(String karma, SQL sql) {
        boolean customSQL = false; // indicates if we have to manage our own sql connection
        if(sql == null) {
            customSQL = true;
            sql = new SQL();
            if(!sql.connect()) {
                System.err.println("Couldn't connect to the SQL database!");
                return null;
            }
        }

        if(!sql.query("SELECT id, name, value FROM "+KARMA_DB+";")) {
            System.err.println("ERROR: Unable to query SQL database.");
            return null;
        }

        while(sql.getNext()) {
            Object[] values = sql.getValueList();
            if((values[1]).equals(karma)) {
                if(customSQL)
                    sql.disconnect();
                return new KarmaItem((Integer)values[0], (String)values[1], (Integer)values[2]);
            }
        }
        return null;
    }
}
