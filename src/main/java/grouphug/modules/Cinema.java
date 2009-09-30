package grouphug.modules;

import grouphug.Grouphug;
import grouphug.SQL;
import grouphug.GrouphugModule;
import grouphug.util.PasswordManager;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Cinema implements GrouphugModule {

    private static final String TRIGGER = "kino";
    private static final String TRIGGER_HELP = "cinema";
    private static final String SQL_HOST = "heiatufte.net";
    private static final String SQL_DB = "narvikdata";
    private static final String SQL_USER = "narvikdata";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("d.M E HH:mm");

    public Cinema() {
    }

    private String replaceHTML(String str) {
        str = str.replace("&oslash;", "ø");
        str = str.replace("&aring;", "å");
        str = str.replace("&aelig;", "æ");
        str = str.replace("&quot;", "\"");
        return str;
    }

    public String helpMainTrigger(String channel, String sender, String login, String hostname, String message) {
        return TRIGGER_HELP;
    }

    public String helpSpecialTrigger(String channel, String sender, String login, String hostname, String message) {
        if(message.equals(TRIGGER_HELP)) {
            return "Cinema module: Display upcoming movies at Narvik Cinema\n" +
                   "  "+Grouphug.MAIN_TRIGGER+TRIGGER+"\n" +
                   "  "+Grouphug.MAIN_TRIGGER+TRIGGER +" <nr of films>\n" +
                   "  "+Grouphug.MAIN_TRIGGER+TRIGGER +" all";
        }
        return null;
    }


    public void specialTrigger(String channel, String sender, String login, String hostname, String message) {
        // do nothing
    }

    public void trigger(String channel, String sender, String login, String hostname, String message) {
        if(!message.startsWith(Cinema.TRIGGER))
            return;

        // TODO find a new db
        if(PasswordManager.getSQLPassword() == null) {
            Grouphug.getInstance().sendMessage("Sorry, I don't have the password for the SQL db on grimstux.", false);
            return;
        }

        // Find out how many lines the user wants
        String limit = "";
        if(!message.endsWith(" all")) {
            limit = " LIMIT 5";
        }

        int number = -1;
        try {
            number = Integer.parseInt(message.substring(message.length() - 1, message.length()));
            number = Integer.parseInt(message.substring(message.length() - 2, message.length()));
        } catch(NumberFormatException ex) {
            // do nothing - if the number hasn't been set, we know it didn't work
        }
        if(number > 0) {
            limit = " LIMIT "+number;
        }

        // Fetch the data
        String lineToSend = "";
        SQL sql = new SQL();
        try {
            sql.connect(SQL_HOST, SQL_DB, SQL_USER, PasswordManager.getSQLPassword());
            sql.query("SELECT datetime, title, cencor, theater, price FROM narvikdata_kino ORDER BY datetime"+limit+";");
            while(sql.getNext()) {
                Object[] values = sql.getValueList();
                java.sql.Timestamp time = (java.sql.Timestamp)values[0];
                String title = replaceHTML((String)values[1]);
                String cencor = replaceHTML((String)values[2]);
                String theater = (String)values[3];
                String price = replaceHTML((String)values[4]);
                lineToSend += "\n"+DATE_FORMAT.format(time)+" - "+title+" ("+cencor+") @ "+theater+" - "+price+" kr";
            }
        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            Grouphug.getInstance().sendMessage("Sorry, an SQL error occurred.", false);
            sql.disconnect();
            return;
        }

        Grouphug.getInstance().sendMessage(lineToSend, true);
    }
}
