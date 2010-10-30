package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.exceptions.NoTitleException;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.Web;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

public class IMDb implements TriggerListener {

	private static final String TRIGGER = "imdb";
	private static final String TRIGGER_HELP = "imdb";

	public IMDb(ModuleHandler moduleHandler) {
		moduleHandler.addTriggerListener(TRIGGER, this);
		moduleHandler.registerHelp(TRIGGER_HELP, "IMDb: Show IMDb info for a movie\n" +
				"  "+Grouphug.MAIN_TRIGGER+TRIGGER +"<movie name>");
		System.out.println("IMDb module loaded.");
	}


	public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
		URL imdbURL;
		try {
			imdbURL = Web.googleSearch(message+"+site:www.imdb.com").get(0);
		} catch(IndexOutOfBoundsException ex) {
			Grouphug.getInstance().sendMessage("Sorry, I didn't find "+message+" on IMDb.");
			return;
		} catch(IOException e) {
			Grouphug.getInstance().sendMessage("But I don't want to. (IOException)");
			return;
		} catch (JDOMException e) {
			Grouphug.getInstance().sendMessage("I seem to have thrown a JDOMException. Woopsie!");
			return;
		}


		String title = "";
		String score = "";
		String plot = "";

		Document doc;
		try {
			doc = Web.getJDOMDocument(imdbURL);

			title = Web.fetchTitle(imdbURL);
			title = title.substring(0, title.length()-7); //To remove the  - IMDb part of the title

			// find the score of the movie element using XPath
			XPath scorePath = XPath.newInstance("//h:span[@id='star-bar-user-rate']/h:b");
			scorePath.addNamespace("h","http://www.w3.org/1999/xhtml");

			Element scoreElement = (Element)scorePath.selectSingleNode(doc);
			if(scoreElement == null) {
				throw new JDOMException("No score element in DOM");
			}
			score = scoreElement.getText();


			// find the score of the movie element using XPath
			XPath plotPath = XPath.newInstance("//h:table[@id='title-overview-widget-layout']/h:tbody/h:tr/h:td[@id='overview-top']/h:p[2]");
			plotPath.addNamespace("h","http://www.w3.org/1999/xhtml");

			Element plotElement = (Element)plotPath.selectSingleNode(doc);
			if(plotElement == null) {
				throw new JDOMException("No plot element in DOM");
			}
			plot = plotElement.getText();


		} catch (IOException e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		} catch (JDOMException e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		} catch (NoTitleException e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		try {
			Grouphug.getInstance().sendMessage(title+"\n"+plot+"\n"+"\n"+score+"/10\n"+imdbURL.toString());
		} catch(NullPointerException ex) {
			Grouphug.getInstance().sendMessage("The IMDb site layout may have changed, I was unable to parse it.");
		}
	}
}
