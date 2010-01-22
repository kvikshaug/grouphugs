package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.listeners.TriggerListener;
import grouphug.util.Web;
//import org.jdom.JDOMException;
a
import java.io.IOException;


/**
 * Give the temperature of the given location to the channel.
 */
public class Temperature implements TriggerListener {

    private static final String TRIGGER = "temp";
    private static final String TRIGGER_HELP = "temp";

    public Temperature(ModuleHandler moduleHandler) {
        moduleHandler.addTriggerListener(TRIGGER, this);;
        moduleHandler.registerHelp(TRIGGER_HELP, "temp:\n" +
                "  !temp [location]\n" +
                "If location is invalid Tromsø is used." +
                "No location gives Tromsø, Trondheim, Grimstad and Oslo.");
        System.out.println("Temperature module loaded.");
    }


    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        try {
            String location = message;
            String temperature;
            if(!location.equals("")) {
                temperature = location + ": " + Web.temperature(location);
            } else {
                temperature = "Tromsø: " + Web.temperature("Tromsø") +
                              "Trondheim: " + Web.temperature("Trondheim") +
                              "Grimstad: " + Web.temperature("Grimstad") +
                              "Oslo: " + Web.temperature("Oslo");
            }

            Grouphug.getInstance().sendMessage(temperature);
            
        } catch(IOException ex) {
            Grouphug.getInstance().sendMessage("Sorry, the intartubes seems to be clogged up (IOException)");
            System.err.println(ex);
            ex.printStackTrace();
        }
    }
}
