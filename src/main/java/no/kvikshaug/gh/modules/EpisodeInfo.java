package no.kvikshaug.gh.modules;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.jibble.pircbot.Colors;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.listeners.TriggerListener;

public class EpisodeInfo implements TriggerListener  {
	
    private static final String TRIGGER_HELP = "ep";
    private static final String TRIGGER = "ep";

    public EpisodeInfo(ModuleHandler moduleHandler) {
            moduleHandler.addTriggerListener(TRIGGER, this);
            moduleHandler.registerHelp(TRIGGER_HELP, "Searches for the latests pre of an episode of your favorite TV show\n" +
                    "To search for a show, use " +Grouphug.MAIN_TRIGGER + TRIGGER + " <name>\n");
            System.out.println("Epinfo module loaded.");
    }
    
    
    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        if(trigger.equals(TRIGGER)) {
            String reply = find(message);
            Grouphug.getInstance().sendMessageChannel(channel, reply, true);
        }
    }

    private String find(String message){
    	
    	message = message.replace(" ", "%20");
    	String inputLine, showName = "", latestEp = "", nextEp = "";
        try{
        	URL tvrage = new URL("http://services.tvrage.com/tools/quickinfo.php?show=" + message);
            URLConnection tvrc = tvrage.openConnection();
            BufferedReader in = new BufferedReader( new InputStreamReader(tvrc.getInputStream()));
            
            
            while ((inputLine = in.readLine()) != null)
            {
            	String[] splitted = inputLine.split("@");
            	if (splitted[0].equals("Show Name"))
            	{
            		showName = splitted[1];
            	}else if (splitted[0].equals("Latest Episode"))
            	{
            		latestEp = splitted[1].replace("^", ", ");
            	}else if (splitted[0].equals("Next Episode"))
            	{
            		nextEp = splitted[1].replace("^" , ", ");
            		break;
            	}
            }
            	
        	in.close();
        	String reply =  ""+Colors.BOLD + "Show: "+Colors.NORMAL+ showName + 
        		Colors.BOLD + " Latest episode: "+ Colors.NORMAL+ latestEp + 
        		Colors.BOLD + " Next episode: " +Colors.NORMAL+ nextEp;

        	return reply;
        	
        } catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		return "Something went wrong";
    }

}
