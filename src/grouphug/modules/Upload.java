package grouphug.modules;


import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import grouphug.Grouphug;
import grouphug.GrouphugModule;
import grouphug.SQL;
import grouphug.util.PasswordManager;
import grouphug.util.FileDataDownload;

public class Upload implements GrouphugModule {

    private static final String DEFAULT_SQL_HOST = "127.0.0.1";
    private static final String DEFAULT_SQL_USER = "gh";
    private static final String TRIGGER_HELP = "upload";
    private static final String TRIGGER = "upload ";
    private static final String UPLOAD_DB= "gh_upload";
    private static final String TRIGGER_RANDOM = "uploadrandom";
    private static final String TRIGGER_KEYWORD = "keyword ";


    @Override
    public String helpMainTrigger(String channel, String sender, String login, String hostname, String message) {
        return TRIGGER_HELP;
    }

    @Override
    public String helpSpecialTrigger(String channel, String sender, String login, String hostname, String message) {
        if(message.equals(TRIGGER_HELP)) {
            return " A module to upload pictures and other things to gh\n" +
                    " To use the module type " +Grouphug.MAIN_TRIGGER + TRIGGER + "<url> <keyword>\n" +
                    " To get links associated with a keyword type "+ Grouphug.MAIN_TRIGGER + TRIGGER_KEYWORD+"<keyword>";
        }
        return null;
    }

    @Override
    public void specialTrigger(String channel, String sender, String login, String hostname, String message) {
        //TODO upload pictures and the like
    	//Only does something when asked

    }

    @Override
    public void trigger(String channel, String sender, String login, String hostname, String message) {
        if(message.startsWith(TRIGGER)){
            insert(message.substring(TRIGGER.length()), sender);
        }
        else if(message.equals(TRIGGER_RANDOM))
            showRandom();
        else if(message.startsWith(TRIGGER_KEYWORD))
            showUploads(message.substring(TRIGGER_KEYWORD.length()));

    }

    private void showUploads(String keyword) {
        SQL sql = new SQL();
        try{
        	sql.connect(DEFAULT_SQL_HOST, "murray", DEFAULT_SQL_USER, PasswordManager.getHinuxPass());
            sql.query("SELECT url, nick FROM "+ UPLOAD_DB+" WHERE keyword='"+keyword+"';");

            if(!sql.getNext()) {
                Grouphug.getInstance().sendMessage("Nothing has been uploaded with keyword "+keyword, false);
            }else{
                //Prints the URL(s) associated with the keyword
                Object[] values = sql.getValueList();
                Grouphug.getInstance().sendMessage(values[1] + " uploaded http://hinux.hin.no/~murray/gh/up/"+ values[0], false);

                while(sql.getNext()){
                    values = sql.getValueList();
                    Grouphug.getInstance().sendMessage(values[1] + " uploaded http://hinux.hin.no/~murray/gh/up/"+ values[0], false);
                }
            }
        }catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            Grouphug.getInstance().sendMessage("Sorry, an SQL error occured.", false);
        }finally {
            sql.disconnect();
        }


    }

    private void showRandom() {
        Grouphug.getInstance().sendMessage("Throwing 'Not yet implemented'-Exception",false);


    }

    private void insert(String message, String sender) {
        SQL sql = new SQL();
        //Split the message into URL and keyword, URL first
        String[] parts = message.split(" ");
        String filename = parts[0].substring(parts[0].lastIndexOf('/')+1);

        try{
			sql.connect(DEFAULT_SQL_HOST, "murray", DEFAULT_SQL_USER, PasswordManager.getHinuxPass());
			PreparedStatement statement = sql.getConnection().prepareStatement("INSERT INTO "+UPLOAD_DB+" (url, keyword, nick) VALUES (?,?,?);");
			statement.setString(1, filename);
			statement.setString(2, parts[1]);
			statement.setString(3, sender);
			sql.executePreparedUpdate(statement);

		}catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            Grouphug.getInstance().sendMessage("Sorry, an SQL error occured.", false);
		}finally {
            sql.disconnect();
        }
        //Now we download the file, at least we hope so
        FileDataDownload.fileDownload(parts[0],"/home/DT2006/murray/public_html/gh/up/");

        // And fix the permissions
        try {
            Runtime.getRuntime().exec("chmod o+r /home/DT2006/murray/public_html/gh/up/"+filename);
        } catch(IOException ex) {
            System.err.println(ex);
        }
        // Prints the URL to the uploaded file to the channel
        Grouphug.getInstance().sendMessage("http://hinux.hin.no/~murray/gh/up/"+filename,false);

    }
}
