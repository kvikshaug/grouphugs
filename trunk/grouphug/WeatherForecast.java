package grouphug;

import java.io.*;
import java.sql.SQLException;

public class WeatherForecast implements GrouphugModule {

    private static final String TRIGGER = "!weather";
    private static final String SQL_HOST = "heiatufte.net";
    private static final String SQL_DB = "narvikdata";
    private static final String SQL_USER = "narvikdata";
    private static String SQL_PASSWORD;
    private static boolean pwOk = false;

    private String fixOutput(String str) {
        str = str.replace("&oslash;", "o");
        str = str.replace("&aring;", "a");
        str = str.replace("&aelig;", "a");
        str = str.replace("&quot;", "\"");
        str = str.replace("<p><em>", "");
        str = str.replace("</em>", "");
        str = str.replace("</p>", "");
        str = str.replace("<br />", "\n");
        str = str.replace("&deg;", "°");
        return str;
    }

    public void trigger(Grouphug bot, String channel, String sender, String login, String hostname, String message) {
        if(!message.startsWith(WeatherForecast.TRIGGER))
            return;

        if(!pwOk) {
            bot.sendMessage("Couldn't fetch SQL password from file, please fix and reload the module.");
            return;
        }

        SQL sql = new SQL();
        try {
            sql.connect(SQL_HOST, SQL_DB, SQL_USER, SQL_PASSWORD);
            sql.query("SELECT korttidsvarsel FROM narvikdata;");
            sql.getNext();
        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            bot.sendMessage("Sorry, an SQL error occurred.");
            sql.disconnect();
            return;
        }

        Object[] values = sql.getValueList();
        sql.disconnect();

        values[0] = fixOutput((String)values[0]);

        Grouphug.spamOK = true;
        bot.sendMessage((String)values[0]);
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
