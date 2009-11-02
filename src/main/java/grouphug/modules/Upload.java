package grouphug.modules;


import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.listeners.TriggerListener;
import grouphug.util.FileDataDownload;
import grouphug.util.SQLHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

public class Upload implements TriggerListener {

    private static final String TRIGGER_HELP = "upload";
    private static final String TRIGGER = "upload";
    private static final String UPLOAD_DB= "upload";
    private static final String TRIGGER_KEYWORD = "keyword ";
    private static final String IMAGE_DIRECTORY = "img";

    private SQLHandler sqlHandler;

    // TODO fix for the new server

    public Upload(ModuleHandler moduleHandler) {
        System.err.println("Upload module has not been configured for its new server yet.");
        /*
        try {
            sqlHandler = SQLHandler.getSQLHandler();
            // moduleHandler.addMessageListener(this);
            moduleHandler.addTriggerListener(TRIGGER, this);
            moduleHandler.registerHelp(TRIGGER_HELP, " A module to upload pictures and other things to gh\n" +
                    " To use the module type " +Grouphug.MAIN_TRIGGER + TRIGGER + "<url> <keyword>\n" +
                    " To get links associated with a keyword type "+ Grouphug.MAIN_TRIGGER + TRIGGER_KEYWORD+"<keyword>");
        } catch(ClassNotFoundException ex) {
            System.err.println("Upload module startup error: SQL unavailable!");
        }
        */
    }


    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        //TODO upload pictures and the like
        //Only does something when asked
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message) {
        if(message.startsWith(TRIGGER)){
            insert(message.substring(TRIGGER.length()), sender);
        }
        else if(message.startsWith(TRIGGER_KEYWORD))
            showUploads(message.substring(TRIGGER_KEYWORD.length()));

    }

    private void showUploads(String keyword) {
        try{
            ArrayList<Object[]> rows = sqlHandler.select("SELECT url, nick FROM "+ UPLOAD_DB+" WHERE keyword='"+keyword+"';");

            if(rows.size() == 0) {
                Grouphug.getInstance().sendMessage("Nothing has been uploaded with keyword "+keyword);
            } else {
                for(Object[] row : rows) {
                    //Prints the URL(s) associated with the keyword
                    Grouphug.getInstance().sendMessage(row[1] + " uploaded http://hinux.hin.no/~murray/gh/up/"+ row[0]);
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
        String filename = parts[0].substring(parts[0].lastIndexOf('/')+1);

        try{
            ArrayList<String> params = new ArrayList<String>();
            params.add(filename);
            params.add(parts[1]);
            params.add(sender);
            sqlHandler.insert("INSERT INTO "+UPLOAD_DB+" (url, keyword, nick) VALUES (?,?,?);", params);

            /* The following is (was) a good faith-attempt to do SQL properly:

            PreparedStatement statement = sql.getConnection().prepareStatement("INSERT INTO "+UPLOAD_DB+" (url, keyword, nick) VALUES (?,?,?);");
            statement.setString(1, filename);
            statement.setString(2, parts[1]);
            statement.setString(3, sender);
            sql.executePreparedUpdate(statement);

            */

		} catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            Grouphug.getInstance().sendMessage("Sorry, an SQL error occured.");
        }
        //Now we download the file, at least we hope so
        FileDataDownload.fileDownload(parts[0], IMAGE_DIRECTORY);

        // And fix the permissions
        try {
            Runtime.getRuntime().exec("chmod o+r "+IMAGE_DIRECTORY+filename);
        } catch(IOException ex) {
            System.err.println(ex);
        }
        // Prints the URL to the uploaded file to the channel
        Grouphug.getInstance().sendMessage("http://gh.kvikshaug.no/?/"+filename,false);

    }
}
