package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.listeners.TriggerListener;
import grouphug.util.Web;

import java.io.IOException;
import java.util.ArrayList;

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
        if (message.equals(""))
            Grouphug.getInstance().sendMessage("Try \"!help weather\".");
        else {  
            String[] split = message.split(" ", 2);
            boolean isSplited = false;
            int n = 0;
            try {
                n = Integer.parseInt(split[0]) - 1;
                isSplited = true;
            } catch (NumberFormatException ex) { }

            ArrayList<String[]> results = new ArrayList<String[]>();
            try {
                if (isSplited)
                    results = Web.weatherLocationSearch(split[1]);
                else
                    results = Web.weatherLocationSearch(message);

                String[] location = results.get(n)[0].split("/");
                String output = location[4] + "(" + location[3] + ", " +
                                location[2] + ") " + results.get(n)[2];

                if (!results.get(n)[1].equals(""))
                    output = output + " " + results.get(n)[1] + " moh";

                String forecast = "";
                try {
                    forecast = Web.weatherForecast(results.get(n)[0]);
                } catch (IOException ex) {}

                if (forecast.equals(""))
                    forecast = "No forecast.";

                Grouphug.getInstance().sendMessage(output + ": "+ forecast);
            }
            catch (IndexOutOfBoundsException ex) {
                if (isSplited)
                    Grouphug.getInstance().sendMessage("Sorry, I could only find " + results.size() +
                                                       " locations matching \"" + split[1] + "\".");
                else
                    Grouphug.getInstance().sendMessage("Sorry, I could only find " + results.size() +
                                                       " locations matching \"" + message + "\".");
            }
            catch (IOException ex) {
                Grouphug.getInstance().sendMessage("Sorry, yr.no seems to be broken.");
                ex.printStackTrace();
            }
        }
    }
}
