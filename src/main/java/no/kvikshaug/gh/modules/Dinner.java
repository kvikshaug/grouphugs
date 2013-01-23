package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.CharEncoding;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.*;
import java.util.Map;
import java.util.HashMap;

import org.joda.time.DateTime;

public class Dinner implements TriggerListener {

    private static final String TRIGGER = "dinner";
    private static final String TRIGGER_NORWEGIAN = "middag";
    private static final String TRIGGER_HELP = "dinner";


    Map<Integer, String> dayMap = new HashMap<Integer, String>() {{
        put(1, "mandag");
        put(2, "tirsdag");
        put(3, "onsdag");
        put(4, "torsdag");
        put(5, "fredag");
        put(6, "lordag");
        put(7, "sondag");
    }};

    public Dinner(ModuleHandler moduleHandler) {
        moduleHandler.addTriggerListener(TRIGGER, this);
        moduleHandler.addTriggerListener(TRIGGER_NORWEGIAN, this);
        moduleHandler.registerHelp(TRIGGER_HELP, "Dinner: Shows what's for dinner at ntnu's most important campus.\n" +
                    Grouphug.MAIN_TRIGGER+TRIGGER+ " will show what's for dinner today\n" +
                    Grouphug.MAIN_TRIGGER+TRIGGER+" <day> will show what was/will be for dinner, SIT's API sucks. Use the norwegian name of the day");
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {

        URL hangarenUrl = null;
        try {
            hangarenUrl = new URL("http://www.sit.no/dagensmiddag_json/?campus=hangaren");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        BufferedReader in;
        String hangaren = "";
        String inputLine = "";
        try {
            in = new BufferedReader(new InputStreamReader(hangarenUrl.openStream(), CharEncoding.guessEncoding(hangarenUrl, "ISO-8859-1")));
            while ((inputLine = in.readLine()) != null) {
                hangaren += inputLine;
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        URL realfagUrl = null;
        try {
            realfagUrl = new URL("http://www.sit.no/dagensmiddag_json/?campus=realfag");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        String realfag = "";
        inputLine = "";
        try {
            in = new BufferedReader(new InputStreamReader(realfagUrl.openStream(), CharEncoding.guessEncoding(realfagUrl, "ISO-8859-1")));
            while ((inputLine = in.readLine()) != null) {
                realfag += inputLine;
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String dayToFind = "mandag"; //monday default, why not

        if (message.equals("")) {
            DateTime today = new DateTime();
            int dayOfWeek = today.getDayOfWeek();
            dayToFind = dayMap.get(dayOfWeek);
        } else {
            //Yeahyeah, do some sanity checks here prolly
            String[] messageParts = message.split(" ");
            if (messageParts.length > 0 ){
                dayToFind = messageParts[0];
            }
        }

        String hangarenOutput = "";
        String realfagOutput = "";

        String regexp = dayToFind +"\":\"(.*?)\\\\n\"";

        Pattern p = Pattern.compile(regexp);
        Matcher matcher = p.matcher(hangaren);
        if (matcher.find()) {
            hangarenOutput = matcher.group(1);
        }
        hangarenOutput = hangarenOutput.replace("\\n", " | ");


        matcher = p.matcher(realfag);
        if (matcher.find()){
            realfagOutput = matcher.group(1);
        }
        realfagOutput = realfagOutput.replace("\\n", " | ");

        String output = "Hangaren: " + hangarenOutput + "\n" + "Realfag: " + realfagOutput;

        Grouphug.getInstance().msg(channel, output);
    }
}
