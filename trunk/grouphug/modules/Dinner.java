package grouphug.modules;

import grouphug.Grouphug;
import grouphug.SQL;
import grouphug.WeekDay;
import grouphug.util.PasswordManager;

import java.util.GregorianCalendar;
import java.sql.SQLException;

public class Dinner implements GrouphugModule {

    private static final String TRIGGER = "middag";
    private static final String TRIGGER_DEPRECATED = "dinner";
    private static final String TRIGGER_HELP = "dinner";
    private static final String SQL_HOST = "heiatufte.net";
    private static final String SQL_DB = "narvikdata";
    private static final String SQL_USER = "narvikdata";

    private String replaceHTML(String str) {
        str = str.replace("&oslash;", "ø");
        str = str.replace("&aring;", "å");
        str = str.replace("&aelig;", "æ");
        str = str.replace("&quot;", "\"");
        str = str.replace("<br />", " - ");
        str = str.replace("<br>", " - ");
        str = str.replace("&amp;", "&");
        return str;
    }

    public String helpMainTrigger(String channel, String sender, String login, String hostname, String message) {
        return TRIGGER_HELP;
    }

    public boolean helpSpecialTrigger(String channel, String sender, String login, String hostname, String message) {
        if(message.equals(TRIGGER_HELP)) {
            Grouphug.getInstance().sendNotice(sender, "Dinner: Shows what's for dinner at HiN.");
            Grouphug.getInstance().sendNotice(sender, "  "+Grouphug.MAIN_TRIGGER+TRIGGER);
            Grouphug.getInstance().sendNotice(sender, "  "+Grouphug.MAIN_TRIGGER+TRIGGER +" <ukedag>");
            Grouphug.getInstance().sendNotice(sender, "  "+Grouphug.MAIN_TRIGGER+TRIGGER +" all");
            return true;
        }
        return false;
    }

    public void specialTrigger(String channel, String sender, String login, String hostname, String message) {
        // do nothing
    }

    public void trigger(String channel, String sender, String login, String hostname, String message) {
        if(!message.startsWith(Dinner.TRIGGER) && !message.startsWith(Dinner.TRIGGER_DEPRECATED))
            return;

        if(PasswordManager.getGrimstuxPass() == null) {
            Grouphug.getInstance().sendMessage("Sorry, I don't have the password for the SQL db on grimstux.", false);
            return;
        }

        // Fetch the data
        SQL sql = new SQL();
        try {
            sql.connect(SQL_HOST, SQL_DB, SQL_USER, PasswordManager.getGrimstuxPass());
            sql.query("SELECT middag_dato, middag_mandag, middag_tirsdag, middag_onsdag, middag_torsdag, middag_fredag FROM narvikdata;");
            sql.getNext();
        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            Grouphug.getInstance().sendMessage("Sorry, an SQL error occurred.", false);
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
        else if(message.endsWith("mandag"))
            wantedDay = WeekDay.MONDAY;
        else if(message.endsWith("tirsdag"))
            wantedDay = WeekDay.TUESDAY;
        else if(message.endsWith("onsdag"))
            wantedDay = WeekDay.WEDNESDAY;
        else if(message.endsWith("torsdag"))
            wantedDay = WeekDay.THURSDAY;
        else if(message.endsWith("freday"))
            wantedDay = WeekDay.FRIDAY;
        else {
            switch(new GregorianCalendar().get(GregorianCalendar.DAY_OF_WEEK)) {
                case 7:
                case 1:
                    Grouphug.getInstance().sendMessage("Middag blir ikke servert i helgen.", false);
                    return;
                case 2: wantedDay = WeekDay.MONDAY; break;
                case 3: wantedDay = WeekDay.TUESDAY; break;
                case 4: wantedDay = WeekDay.WEDNESDAY; break;
                case 5: wantedDay = WeekDay.THURSDAY; break;
                case 6: wantedDay = WeekDay.FRIDAY; break;
                default:
                    Grouphug.getInstance().sendMessage("Vet ikke hvilken dag det er idag, vennligst spesifiser.", false);
                    return;
            }
        }

        Grouphug.getInstance().sendMessage("Dagens middag ("+values[0]+"):", false);

        if(wantedDay == WeekDay.MONDAY || wantedDay == WeekDay.ALL)
            Grouphug.getInstance().sendMessage("Mandag: "+values[1], false);
        if(wantedDay == WeekDay.TUESDAY || wantedDay == WeekDay.ALL)
            Grouphug.getInstance().sendMessage("Tirsdag: "+values[2], false);
        if(wantedDay == WeekDay.WEDNESDAY || wantedDay == WeekDay.ALL)
            Grouphug.getInstance().sendMessage("Onsdag: "+values[3], false);
        if(wantedDay == WeekDay.THURSDAY || wantedDay == WeekDay.ALL)
            Grouphug.getInstance().sendMessage("Torsdag: "+values[4], false);
        if(wantedDay == WeekDay.FRIDAY || wantedDay == WeekDay.ALL)
            Grouphug.getInstance().sendMessage("Fredag: "+values[5], false);

    }
}