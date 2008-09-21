package grouphug;

import java.io.*;
import java.util.GregorianCalendar;

public class Dinner implements GrouphugModule {

    private static final String TRIGGER = "!dinner";
    private static final String SQL_HOST = "heiatufte.net";
    private static final String SQL_DB = "narvikdata";
    private static final String SQL_USER = "narvikdata";
    private static String SQL_PASSWORD;
    private static boolean pwOk = false;

    private String replaceHTML(String str) {
        str = str.replace("&oslash;", "o");
        str = str.replace("&aring;", "a");
        str = str.replace("&aelig;", "a");
        str = str.replace("&quot;", "\"");
        return str;
    }
    
    public void trigger(Grouphug bot, String channel, String sender, String login, String hostname, String message) {
        if(!message.startsWith(Dinner.TRIGGER))
            return;

        if(!pwOk) {
            bot.sendMessage("Couldn't fetch SQL password from file.");
            return;
        }

        // Fetch the data
        SQL sql = new SQL();
        sql.connect(SQL_HOST, SQL_DB, SQL_USER, SQL_PASSWORD);
        sql.query("SELECT middag_dato, middag_mandag, middag_tirsdag, middag_onsdag, middag_torsdag, middag_fredag FROM narvikdata;");

        sql.getNext();
        Object[] values = sql.getValueList();

        values[0] = replaceHTML((String)values[0]);
        values[1] = replaceHTML((String)values[1]);
        values[2] = replaceHTML((String)values[2]);
        values[3] = replaceHTML((String)values[3]);
        values[4] = replaceHTML((String)values[4]);
        values[5] = replaceHTML((String)values[5]);

        // Figure out what day is wanted; default being today's day
        WeekDay wantedDay;
        if(message.contains("all"))
            wantedDay = WeekDay.ALL;
        else if(message.contains("monday"))
            wantedDay = WeekDay.MONDAY;
        else if(message.contains("tuesday"))
            wantedDay = WeekDay.TUESDAY;
        else if(message.contains("wednesday"))
            wantedDay = WeekDay.WEDNESDAY;
        else if(message.contains("thursday"))
            wantedDay = WeekDay.THURSDAY;
        else if(message.contains("friday"))
            wantedDay = WeekDay.FRIDAY;
        else {
            switch(new GregorianCalendar().get(GregorianCalendar.DAY_OF_WEEK)) {
                case 7:
                case 1:
                    bot.sendMessage("Middag blir ikke servert i helgen.");
                    return;
                case 2: wantedDay = WeekDay.MONDAY; break;
                case 3: wantedDay = WeekDay.TUESDAY; break;
                case 4: wantedDay = WeekDay.WEDNESDAY; break;
                case 5: wantedDay = WeekDay.THURSDAY; break;
                case 6: wantedDay = WeekDay.FRIDAY; break;
                default:
                    bot.sendMessage("Vet ikke hvilken dag det er idag, vennligst spesifiser.");
                    return;
            }
        }

        bot.sendMessage("Dagens middag ("+values[0]+"):");

        if(wantedDay == WeekDay.MONDAY || wantedDay == WeekDay.ALL)
            bot.sendMessage("Mandag: "+values[1]);
        if(wantedDay == WeekDay.TUESDAY || wantedDay == WeekDay.ALL)
            bot.sendMessage("Tirsdag: "+values[2]);
        if(wantedDay == WeekDay.WEDNESDAY || wantedDay == WeekDay.ALL)
            bot.sendMessage("Onsdag: "+values[3]);
        if(wantedDay == WeekDay.THURSDAY || wantedDay == WeekDay.ALL)
            bot.sendMessage("Torsdag: "+values[4]);
        if(wantedDay == WeekDay.FRIDAY || wantedDay == WeekDay.ALL)
            bot.sendMessage("Fredag: "+values[5]);

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