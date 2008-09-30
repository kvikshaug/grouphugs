package grouphug;

import java.io.*;
import java.util.GregorianCalendar;
import java.sql.SQLException;

class Dinner implements GrouphugModule {

    private static Grouphug bot;
    private static final String TRIGGER = "dinner ";
    private static final String SQL_HOST = "heiatufte.net";
    private static final String SQL_DB = "narvikdata";
    private static final String SQL_USER = "narvikdata";
    private static String SQL_PASSWORD;
    private static boolean pwOk = false;

    public Dinner(Grouphug bot) {
        Dinner.bot = bot;
    }

    private String replaceHTML(String str) {
        str = str.replace("&oslash;", "Ã¸");
        str = str.replace("&aring;", "Ã¥");
        str = str.replace("&aelig;", "Ã¦");
        str = str.replace("&quot;", "\"");
        return str;
    }

    public void helpTrigger(String channel, String sender, String login, String hostname, String message) {
        bot.sendNotice(sender, "Dinner: Shows what's for dinner at HiN.");
        bot.sendNotice(sender, " - Trigger 1: "+Grouphug.MAIN_TRIGGER+Dinner.TRIGGER);
        bot.sendNotice(sender, " - Trigger 2: "+Grouphug.MAIN_TRIGGER+Dinner.TRIGGER +"<weekday>");
        bot.sendNotice(sender, " - Trigger 3: "+Grouphug.MAIN_TRIGGER+Dinner.TRIGGER +"all");
    }
    
    public void specialTrigger(String channel, String sender, String login, String hostname, String message) {
        // do nothing
    }

    public void trigger(String channel, String sender, String login, String hostname, String message) {
        if(!message.startsWith(Dinner.TRIGGER))
            return;

        if(!pwOk) {
            bot.sendMessage("Couldn't fetch SQL password from file, please fix and reload the module.", false);
            return;
        }

        // Fetch the data
        SQL sql = new SQL();
        try {
            sql.connect(SQL_HOST, SQL_DB, SQL_USER, SQL_PASSWORD);
            sql.query("SELECT middag_dato, middag_mandag, middag_tirsdag, middag_onsdag, middag_torsdag, middag_fredag FROM narvikdata;");
            sql.getNext();
        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            bot.sendMessage("Sorry, an SQL error occurred.", false);
            sql.disconnect();
            return;
        }

        Object[] values = sql.getValueList();

        values[0] = replaceHTML((String)values[0]);
        values[1] = replaceHTML((String)values[1]);
        values[2] = replaceHTML((String)values[2]);
        values[3] = replaceHTML((String)values[3]);
        values[4] = replaceHTML((String)values[4]);
        values[5] = replaceHTML((String)values[5]);

        sql.disconnect();

        // Figure out what day is wanted; default being today's day
        WeekDay wantedDay;
        if(message.endsWith("all"))
            wantedDay = WeekDay.ALL;
        else if(message.endsWith("monday"))
            wantedDay = WeekDay.MONDAY;
        else if(message.endsWith("tuesday"))
            wantedDay = WeekDay.TUESDAY;
        else if(message.endsWith("wednesday"))
            wantedDay = WeekDay.WEDNESDAY;
        else if(message.endsWith("thursday"))
            wantedDay = WeekDay.THURSDAY;
        else if(message.endsWith("friday"))
            wantedDay = WeekDay.FRIDAY;
        else {
            switch(new GregorianCalendar().get(GregorianCalendar.DAY_OF_WEEK)) {
                case 7:
                case 1:
                    bot.sendMessage("Middag blir ikke servert i helgen.", false);
                    return;
                case 2: wantedDay = WeekDay.MONDAY; break;
                case 3: wantedDay = WeekDay.TUESDAY; break;
                case 4: wantedDay = WeekDay.WEDNESDAY; break;
                case 5: wantedDay = WeekDay.THURSDAY; break;
                case 6: wantedDay = WeekDay.FRIDAY; break;
                default:
                    bot.sendMessage("Vet ikke hvilken dag det er idag, vennligst spesifiser.", false);
                    return;
            }
        }

        bot.sendMessage("Dagens middag ("+values[0]+"):", false);

        if(wantedDay == WeekDay.MONDAY || wantedDay == WeekDay.ALL)
            bot.sendMessage("Mandag: "+values[1], false);
        if(wantedDay == WeekDay.TUESDAY || wantedDay == WeekDay.ALL)
            bot.sendMessage("Tirsdag: "+values[2], false);
        if(wantedDay == WeekDay.WEDNESDAY || wantedDay == WeekDay.ALL)
            bot.sendMessage("Onsdag: "+values[3], false);
        if(wantedDay == WeekDay.THURSDAY || wantedDay == WeekDay.ALL)
            bot.sendMessage("Torsdag: "+values[4], false);
        if(wantedDay == WeekDay.FRIDAY || wantedDay == WeekDay.ALL)
            bot.sendMessage("Fredag: "+values[5], false);

    }

    public static void loadPassword() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File("pw/narvikdata")));
            SQL_PASSWORD = reader.readLine();
            reader.close();
            if(SQL_PASSWORD.equals(""))
                throw new FileNotFoundException("No data extracted from MySQL password file!");
            pwOk = true;
        } catch(IOException e) {
            // Do nothing - pwOk will be false
        }
    }
}