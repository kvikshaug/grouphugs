package grouphug.modules;

import grouphug.Grouphug;
import grouphug.SQL;
import grouphug.GrouphugModule;
import grouphug.util.PasswordManager;

import java.sql.SQLException;

public class WeatherForecast implements GrouphugModule {

    private static final String TRIGGER = "weather";
    private static final String TRIGGER_HELP = "weatherforecast";
    private static final String SQL_HOST = "heiatufte.net";
    private static final String SQL_DB = "narvikdata";
    private static final String SQL_USER = "narvikdata";

    public String helpMainTrigger(String channel, String sender, String login, String hostname, String message) {
        return TRIGGER_HELP;
    }

    public boolean helpSpecialTrigger(String channel, String sender, String login, String hostname, String message) {
        if(message.equals(TRIGGER_HELP)) {
            Grouphug.getInstance().sendNotice(sender, "WeatherForecast: General forecast for Narvik tomorrow.");
            Grouphug.getInstance().sendNotice(sender, "  " + Grouphug.MAIN_TRIGGER + TRIGGER);
            return true;
        }
        return false;
    }

    public void specialTrigger(String channel, String sender, String login, String hostname, String message) {
        // do nothing
    }

    public void trigger(String channel, String sender, String login, String hostname, String message) {
        if(message.equals("test")) {
            Grouphug.getInstance().sendMessage("text 5", false);
        }
        if(!message.startsWith(WeatherForecast.TRIGGER))
            return;

        if(PasswordManager.getGrimstuxPass() == null) {
            Grouphug.getInstance().sendMessage("Sorry, I don't have the password for the SQL db on grimstux.", false);
            return;
        }

        SQL sql = new SQL();
        try {
            sql.connect(SQL_HOST, SQL_DB, SQL_USER, PasswordManager.getGrimstuxPass());
            sql.query("SELECT korttidsvarsel FROM narvikdata;");
            sql.getNext();
        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            Grouphug.getInstance().sendMessage("Sorry, an SQL error occurred.", false);
            sql.disconnect();
            return;
        }

        Object[] values = sql.getValueList();
        sql.disconnect();

        values[0] = fixOutput((String)values[0]);

        Grouphug.getInstance().sendMessage((String)values[0], false);
    }

    private String fixOutput(String str) {
        str = str.replace("&deg;", "°");
        str = str.replace("&oslash;", "ø");
        str = str.replace("&aring;", "å");
        str = str.replace("&aelig;", "æ");
        str = str.replace("&quot;", "\"");
        str = str.replace("<p><em>", "");
        str = str.replace("</em>", "");
        str = str.replace("</p>", "");
        str = str.replace("<br />", "\n");
        return str;
    }
}
