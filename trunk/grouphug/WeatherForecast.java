package grouphug;

import java.io.*;
import java.sql.SQLException;

class WeatherForecast implements GrouphugModule {

    private static Grouphug bot;
    private static final String TRIGGER = "weather";
    private static final String SQL_HOST = "heiatufte.net";
    private static final String SQL_DB = "narvikdata";
    private static final String SQL_USER = "narvikdata";
    private static String SQL_PASSWORD;
    private static boolean pwOk = false;

    WeatherForecast(Grouphug bot) {
        WeatherForecast.bot = bot;
    }

    public void helpTrigger(String channel, String sender, String login, String hostname, String message) {
        bot.sendNotice(sender, "WeatherForecast: General forecast for Narvik tomorrow.");
        bot.sendNotice(sender, " - Trigger: " + Grouphug.MAIN_TRIGGER + WeatherForecast.TRIGGER);
    }

    public void specialTrigger(String channel, String sender, String login, String hostname, String message) {
        // do nothing
    }

    public void trigger(String channel, String sender, String login, String hostname, String message) {
        if(!message.startsWith(WeatherForecast.TRIGGER))
            return;

        if(!pwOk) {
            bot.sendMessage("Couldn't fetch SQL password from file, please fix and reload the module.", false);
            return;
        }

        SQL sql = new SQL();
        try {
            sql.connect(SQL_HOST, SQL_DB, SQL_USER, SQL_PASSWORD);
            sql.query("SELECT korttidsvarsel FROM narvikdata;");
            sql.getNext();
        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            bot.sendMessage("Sorry, an SQL error occurred.", false);
            sql.disconnect();
            return;
        }

        Object[] values = sql.getValueList();
        sql.disconnect();

        values[0] = fixOutput((String)values[0]);

        bot.sendMessage((String)values[0], false);
    }

    private String fixOutput(String str) {
        str = str.replace("&deg;", "�");
        str = str.replace("&oslash;", "�");
        str = str.replace("&aring;", "�");
        str = str.replace("&aelig;", "�");
        str = str.replace("&quot;", "\"");
        str = str.replace("<p><em>", "");
        str = str.replace("</em>", "");
        str = str.replace("</p>", "");
        str = str.replace("<br />", "\n");
        return str;
    }


    // TODO - this method, and other parts of this file, is copy/paste from Dinner.java, revise and rewrite!
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
