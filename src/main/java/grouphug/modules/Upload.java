package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.exceptions.SQLUnavailableException;
import grouphug.listeners.TriggerListener;
import grouphug.util.SQL;
import grouphug.util.SQLHandler;
import grouphug.util.Web;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

public class Upload implements TriggerListener {

    private static final String TRIGGER_HELP = "upload";
    private static final String TRIGGER = "upload";
    private static final String UPLOAD_DB= "uploads";
    private static final String TRIGGER_KEYWORD = "keyword";
    private static final String DESTINATION_DIR = "/var/www/gh/public/images/up/";

    private SQLHandler sqlHandler;

    public Upload(ModuleHandler moduleHandler) {
        try {
            sqlHandler = SQLHandler.getSQLHandler();
            // moduleHandler.addMessageListener(this);
            moduleHandler.addTriggerListener(TRIGGER, this);
            moduleHandler.addTriggerListener(TRIGGER_KEYWORD, this);
            moduleHandler.registerHelp(TRIGGER_HELP, " A module to upload pictures and other things to gh\n" +
                    " To use the module type " +Grouphug.MAIN_TRIGGER + TRIGGER + " <url> <keyword>\n" +
                    " To search for a keyword or image name, use: "+ Grouphug.MAIN_TRIGGER + TRIGGER_KEYWORD+" <searchphrase>");
        } catch(SQLUnavailableException ex) {
            System.err.println("Upload module startup error: SQL is unavailable!");
        }
    }


    /*public void onMessage(String channel, String sender, String login, String hostname, String message) {
        TODO always upload pictures when posted
    }*/

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        if(trigger.equals(TRIGGER)) {
            insert(message, sender);
        } else if(trigger.equals(TRIGGER_KEYWORD)) {
            showUploads(message);
        }
    }

    private void showUploads(String keyword) {
        try {
            if(keyword.length() <= 1) {
                Grouphug.getInstance().sendMessage("Please use at least 2 search characters.");
            }
            ArrayList<Object[]> rows = sqlHandler.select("SELECT filename, nick, keyword FROM "+ UPLOAD_DB+" WHERE " +
                    "keyword LIKE '%"+keyword+"%' OR filename LIKE '%"+keyword+"%';");

            if(rows.size() == 0) {
                Grouphug.getInstance().sendMessage("No results for '" + keyword + "'.");
            } else {
                for(Object[] row : rows) {
                    //Prints the URL(s) associated with the keyword
                    Grouphug.getInstance().sendMessage("http://gh.kvikshaug.no/images/up/" + row[0] +
                            " ('" + row[2] + "' by " + row[1] + ")");
                }
            }
        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            Grouphug.getInstance().sendMessage("Sorry, an SQL error occured.");
        }
    }

    private void insert(String message, String sender) {
        //Split the message into URL and keyword, URL first
        String[] parts = message.split(" ");
        int lastSlashIndex = parts[0].lastIndexOf('/');
        if(lastSlashIndex == -1) {
            Grouphug.getInstance().sendMessage("That's not a valid URL now, is it?");
            return;
        }
        if(parts.length <= 1) {
            Grouphug.getInstance().sendMessage("Please provide both a valid URL and keyword.");
            return;
        }
        int nextQuestionmarkIndex = parts[0].indexOf('?', lastSlashIndex);
        String filename;
        if(nextQuestionmarkIndex != -1) {
            filename = parts[0].substring(lastSlashIndex+1, nextQuestionmarkIndex);
        } else {
            filename = parts[0].substring(lastSlashIndex+1);
        }

        // if the file exists, add a number to the front of it
        if(new File(DESTINATION_DIR + filename).exists()) {
            int number = 1;
            while(new File(DESTINATION_DIR + number + filename).exists()) {
                number++;
            }
            filename = number + filename;
        }

        try {
            Web.downloadFile(parts[0], filename, DESTINATION_DIR);
            ArrayList<String> params = new ArrayList<String>();
            params.add(parts[1]);
            params.add(sender);
            params.add(filename);
            params.add(SQL.dateToSQLDateTime(new Date()));
            sqlHandler.insert("INSERT INTO "+UPLOAD_DB+" (keyword, nick, filename, date) VALUES (?,?,?,?);", params);
        } catch (IOException e) {
            System.err.println("Failed to copy the file to the local filesystem.");
            Grouphug.getInstance().sendMessage("Why am I expected to be able to upload everything?");
            e.printStackTrace();
            return;
        } catch(SQLException e) {
            System.err.println("SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            e.printStackTrace();
            Grouphug.getInstance().sendMessage("An SQL error occured, but the file was probably saved successfully " +
                    "before that happened. Go check the logs and clean up my database, you fool.");
            return;
        }

        // Print the URL to the uploaded file to the channel
        Grouphug.getInstance().sendMessage("Saved to http://gh.kvikshaug.no/images/up/" + filename);
    }
}
