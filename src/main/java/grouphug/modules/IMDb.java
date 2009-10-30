package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.listeners.TriggerListener;
import grouphug.util.Web;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class IMDb implements TriggerListener {

    private static final String TRIGGER = "imdb";
    private static final String TRIGGER_HELP = "imdb";

    public IMDb(ModuleHandler moduleHandler) {
        moduleHandler.addTriggerListener(TRIGGER, this);
        moduleHandler.registerHelp(TRIGGER_HELP, "IMDb: Show IMDb info for a movie\n" +
                   "  "+Grouphug.MAIN_TRIGGER+TRIGGER +"<movie name>");
        System.out.println("IMDb module loaded.");
    }


    public void onTrigger(String channel, String sender, String login, String hostname, String message) {
        URL imdbURL;
        try {
            imdbURL = Web.googleSearch(message+"+site:www.imdb.com").get(0);
        } catch(IndexOutOfBoundsException ex) {
            Grouphug.getInstance().sendMessage("Sorry, I didn't find "+message+" on IMDb.");
            return;
        } catch(IOException e) {
            Grouphug.getInstance().sendMessage("But I don't want to. (IOException)");
            return;
        }

        String title = "";
        String score = "";
        String votes = "";
        String tagline = "";
        String plot = "";
        String commentTitle = "";

        String line = "(uninitialized)";

        try {
            BufferedReader imdb = Web.prepareBufferedReader(imdbURL.toString());

            String titleString = "<title>";
            String scoreString = "<div class=\"meta\">";
            String votesString = "&nbsp;&nbsp;<a href=\"ratings\" class=\"tn15more\">";
            String taglineString = "<h5>Tagline:</h5>";
            String plotString = "<h5>Plot:</h5>";
            String commentString = "<h5>User Comments:</h5>";

            // A bit of copy-pasta and wtf's in here, enjoy :)
            while((line = imdb.readLine()) != null) {
                if(line.startsWith(titleString)) {
                    title = Web.entitiesToChars(line.substring(line.indexOf(">") + 1, line.substring(1).indexOf("<")+1));
                }
                if(line.trim().equals(scoreString)) {
                    line = imdb.readLine();
                    if(line.contains("<b>")) {
                        // we parse to double and get back the string because we want to verify that it indeed
                        // is a number, even though we save and show it as a string.
                        score = String.valueOf(Double.parseDouble(line.substring(line.indexOf("<b>") + 3, line.indexOf("/")))) + "/10";
                    } else if(line.contains("<small>")) {
                        score = line.substring(line.indexOf("<small>") + 7, line.indexOf("</small>"));
                    } else {
                        score = "Unkown score";
                    }
                }
                if(line.startsWith(votesString)) {
                    votes = " (" + line.substring(votesString.length()).substring(0, line.substring(votesString.length()).indexOf(" ")) + " votes)";
                }
                if(line.startsWith(taglineString)) {
                    tagline = imdb.readLine().trim();
                    int ind = tagline.indexOf("<");
                    if(ind != -1) {
                        tagline = tagline.substring(0, ind).trim();
                    }
                    tagline = Web.entitiesToChars(" - "+tagline.replace("|", " "));
                }
                if(line.startsWith(plotString)) {
                    plot = imdb.readLine().trim();
                    int ind = plot.indexOf("<");
                    if(ind != -1) {
                        plot = plot.substring(0, ind).trim();
                    }
                    plot = Web.entitiesToChars(plot.replace("|", " "));
                }
                if(line.startsWith(commentString)) {
                    commentTitle = imdb.readLine().trim();
                    int ind = commentTitle.indexOf("<");
                    if(ind != -1) {
                        commentTitle = commentTitle.substring(0, ind).trim();
                    }
                    commentTitle = Web.entitiesToChars(commentTitle.replace("|", " "));
                }
            }

        } catch(StringIndexOutOfBoundsException ex) {
            System.err.println("Couldn't parse IMDb site!");
            ex.printStackTrace();
            System.err.println("I was parsing the following line:");
            System.err.println(line);
            Grouphug.getInstance().sendMessage("The IMDb site layout may have changed, I was unable to parse it.");
            return;
        } catch(NumberFormatException ex) {
            System.err.println("Couldn't parse IMDb site!");
            ex.printStackTrace();
            System.err.println("I was parsing the following line:");
            System.err.println(line);
            Grouphug.getInstance().sendMessage("The IMDb site layout may have changed, I was unable to parse it.");
            return;
        } catch(MalformedURLException ex) {
            ex.printStackTrace();
            Grouphug.getInstance().sendMessage("Wtf just happened? I caught a MalformedURLException.");
            return;
        } catch(IOException ex) {
            ex.printStackTrace();
            Grouphug.getInstance().sendMessage("Sorry, the intartubes seem to be clogged up.");
            return;
        }

        try {
            Grouphug.getInstance().sendMessage(title+tagline+"\n"+plot+"\n"+"Comment: "+commentTitle+"\n"+score+votes+" - "+imdbURL.toString());
        } catch(NullPointerException ex) {
            Grouphug.getInstance().sendMessage("The IMDb site layout may have changed, I was unable to parse it.");
        }
    }
}
