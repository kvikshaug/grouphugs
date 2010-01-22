package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.listeners.TriggerListener;
import grouphug.util.Web;

import java.io.IOException;

/**
 * Find a weather forecast for the given location and send it to the channel.
 */
public class Weather implements TriggerListener {

    private static final String TRIGGER = "weather";
    private static final String ALT_TRIGGER = "temp";
    private static final String TRIGGER_HELP = "weather";
    private static final String ALT_TRIGGER_HELP = "temp";

    public Weather(ModuleHandler moduleHandler) {
        moduleHandler.addTriggerListener(TRIGGER, this);
        moduleHandler.addTriggerListener(ALT_TRIGGER, this);
        moduleHandler.registerHelp(TRIGGER_HELP, "weather:\n" +
                "  !weather <location>\n" +
                "Tries to find a weather forecast for the given location.");
        moduleHandler.registerHelp(ALT_TRIGGER_HELP, "temp: alias for \"weather\"");
        System.out.println("weather module loaded.");
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        try {
            if (message.equals(""))
                Grouphug.getInstance().sendMessage("Try \"!help weather\".");
            else {
                String forecast = Web.weather(message);

                if (forecast.equals(""))
                    Grouphug.getInstance().sendMessage("Sorry no weather forecast for \"" + message + "\".");
                else
                    Grouphug.getInstance().sendMessage(forecast);
            }
            
        } catch(IOException ex) {
            Grouphug.getInstance().sendMessage("Sorry, the intartubes seems to be clogged up (IOException)");
            System.err.println(ex);
            ex.printStackTrace();
        }
    }
}
