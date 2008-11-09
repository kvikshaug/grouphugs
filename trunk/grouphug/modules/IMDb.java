package grouphug.modules;

import grouphug.GrouphugModule;
import grouphug.Grouphug;

import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class IMDb implements GrouphugModule {

    private static Grouphug bot;
    private static final String TRIGGER = "imdb ";

    public IMDb(Grouphug bot) {
        IMDb.bot = bot;
    }

    public void helpTrigger(String channel, String sender, String login, String hostname, String message) {
        bot.sendNotice(sender, "IMDb: Show IMDb info for a movie");
        bot.sendNotice(sender, "  "+Grouphug.MAIN_TRIGGER+TRIGGER +"<movie name>");
    }

    public void specialTrigger(String channel, String sender, String login, String hostname, String message) {
        // do nothing
    }

    public void trigger(String channel, String sender, String login, String hostname, String message) {
        if(!message.startsWith(IMDb.TRIGGER))
            return;

        String query = message.substring(IMDb.TRIGGER.length());

        URL imdbURL;
        try {
            imdbURL = Google.search(query+"+site:www.imdb.com");
        } catch(IOException e) {
            bot.sendMessage("But I don't want to. (IOException)", false);
            return;
        }
        if(imdbURL == null) {
            bot.sendMessage("Sorry, I didn't find "+query+" on IMDb.", false);
            return;
        }

        String title = "";
        double score = 0;
        String votes = "";
        String tagline = "";
        String plot = "";
        String commentTitle = "";

        URLConnection urlConn;
        try {
            urlConn = imdbURL.openConnection();
            urlConn.setRequestProperty("User-Agent", "Firefox/3.0"); // Trick google into thinking we're a proper browser. ;)

            BufferedReader imdb = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

            String line;

            String titleString = "<title>";
            String scoreString = "<div class=\"meta\">";
            String votesString = "&nbsp;&nbsp;<a href=\"ratings\" class=\"tn15more\">";
            String taglineString = "<h5>Tagline:</h5>";
            String plotString = "<h5>Plot:</h5>";
            String commentString = "<h5>User Comments:</h5>";

            // A bit of copy-pasta and wtf's in here, enjoy :)
            while((line = imdb.readLine()) != null) {
                if(line.startsWith(titleString)) {
                    title = Grouphug.entitiesToChars(line.substring(line.indexOf(">") + 1, line.substring(1).indexOf("<")+1));
                }
                if(line.trim().equals(scoreString)) {
                    line = imdb.readLine();
                    score = Double.parseDouble(line.substring(line.indexOf("<b>") + 3, line.indexOf("/")));
                }
                if(line.startsWith(votesString)) {
                    votes = line.substring(votesString.length()).substring(0, line.substring(votesString.length()).indexOf(" "));
                }
                if(line.startsWith(taglineString)) {
                    tagline = imdb.readLine().trim();
                    int ind = tagline.indexOf("<");
                    if(ind != -1) {
                        tagline = tagline.substring(0, ind).trim();
                    }
                    tagline = Grouphug.entitiesToChars(" - "+tagline.replace("|", " "));
                }
                if(line.startsWith(plotString)) {
                    plot = imdb.readLine().trim();
                    int ind = plot.indexOf("<");
                    if(ind != -1) {
                        plot = plot.substring(0, ind).trim();
                    }
                    plot = Grouphug.entitiesToChars(plot.replace("|", " "));
                }
                if(line.startsWith(commentString)) {
                    commentTitle = imdb.readLine().trim();
                    int ind = commentTitle.indexOf("<");
                    if(ind != -1) {
                        commentTitle = commentTitle.substring(0, ind).trim();
                    }
                    commentTitle = Grouphug.entitiesToChars(commentTitle.replace("|", " "));
                }
            }

        } catch(StringIndexOutOfBoundsException ex) {
            bot.sendMessage("The IMDb site layout may have changed, I was unable to parse it.", false);
            return;
        } catch(NumberFormatException ex) {
            bot.sendMessage("The IMDb site layout may have changed, I was unable to parse it.", false);
            return;
        } catch(MalformedURLException ex) {
            bot.sendMessage("Wtf just happened? I caught a MalformedURLException.", false);
            return;
        } catch(IOException ex) {
            bot.sendMessage("Sorry, the intartubes seem to be clogged up.", false);
            return;
        }

        try {
            bot.sendMessage(title+tagline+"\n"+plot+"\n"+"Comment: "+commentTitle+"\n"+score+"/10 ("+votes+" votes) - "+imdbURL.toString(), false);
        } catch(NullPointerException ex) {
            bot.sendMessage("The IMDb site layout may have changed, I was unable to parse it.", false);
        }
    }
}
