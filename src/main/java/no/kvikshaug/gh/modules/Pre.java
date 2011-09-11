package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.Web;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Pre implements TriggerListener, Runnable {

    private Grouphug bot;
    private String message;
    private String channel;

    public Pre(ModuleHandler handler) {
        bot = Grouphug.getInstance();
        handler.addTriggerListener("pre", this);
        handler.registerHelp("pre", "Searches secret topsite pre-db's and displays pretime for <release>\n!pre <release>\n" +
                "Note: Searches are performed in separate threads");
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        if(message.equals("")) {
            bot.msg(channel, "Search for what?");
        } else {
        	this.channel = channel; //Ugly :<
            // perform all searches in a different thread because doopes.com is
            // usually unstable and may take a loooong time to reply
            this.message = message;
            new Thread(this).start();
        }
    }

    public void run() {
        try {
            Document doc = Web.getJDOMDocument(new URL(
                    "http://doopes.com/?cat=32800&lang=1&num=0&mode=0&from=&to=&exc=&opt=0&inc=" + message));
            XPath xpath = XPath.newInstance("//h:table[@class='rlz']/h:tbody/h:tr");
            xpath.addNamespace("h", "http://www.w3.org/1999/xhtml");

            Object node = xpath.selectSingleNode(doc);
            if(node == null) {
                bot.msg(channel, "Found no release containing '"+message+"'.");
                return;
            }

            // 1. date, 2. type (e.g. TV), 3. releasename
            String[] data = new String[3];
            int i = 0;
            for(Object objectElement : ((Element)node).getChildren()) {
                data[i++] = ((Element)objectElement).getText();
            }

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            DateTime preTime = new DateTime(format.parse(data[0]).getTime());

            Period rp = new Period(preTime, new DateTime());
            StringBuilder releasedTime = new StringBuilder();
            if(rp.getYears() > 0) {
                releasedTime.append(rp.getYears()).append("y ");
            }
            if(rp.getMonths() > 0) {
                releasedTime.append(rp.getMonths()).append("m ");
            }
            if(rp.getWeeks() > 0) {
                releasedTime.append(rp.getWeeks()).append("w ");
            }
            if(rp.getDays() > 0) {
                releasedTime.append(rp.getDays()).append("d ");
            }
            releasedTime.append(rp.getHours()).append("h ");
            releasedTime.append(rp.getMinutes()).append("m ");
            releasedTime.append(rp.getSeconds()).append("s");

            bot.msg(channel, data[2].trim() + ": " + releasedTime.toString()+" ago in "+data[1].trim().toUpperCase());
        } catch (IOException e) {
            e.printStackTrace();
            bot.msg(channel, "IOException :/");
        } catch (JDOMException e) {
            e.printStackTrace();
            bot.msg(channel, "zomg JDOMException :/");
        } catch (ParseException e) {
            e.printStackTrace();
            bot.msg(channel, "zomg, the date format was unparseable :/");
        }
    }
}
