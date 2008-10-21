package grouphug;

import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

class Phonebook implements GrouphugModule {
    
    private static Grouphug bot;
    private static final String TRIGGER = "tlf ";
    private static final int CONN_TIMEOUT = 10000; // ms

    public Phonebook(Grouphug bot) {
        Phonebook.bot = bot;
    }

    public void helpTrigger(String channel, String sender, String login, String hostname, String message) {
        bot.sendNotice(sender, "Telefonkatalogen.no phonebook search:");
        bot.sendNotice(sender, "  "+Grouphug.MAIN_TRIGGER+Phonebook.TRIGGER +"<package id / kollinr>");
    }

    public void specialTrigger(String channel, String sender, String login, String hostname, String message) {
        // do nothing
    }

    public void trigger(String channel, String sender, String login, String hostname, String message) {

        if(!message.startsWith(TRIGGER))
            return;

        String resultat = null;
        try {
            resultat = Phonebook.search(message.substring(TRIGGER.length()));
        } catch(IOException e) {
            bot.sendMessage("Sorry, the intartubes seems to be clogged up (IOException)", false);
            System.err.println(e.getMessage()+"\n"+e.getCause());
            return;
        }

        if(resultat == null) {
            bot.sendMessage("No results for "+message.substring(TRIGGER.length())+".", false);
        } else {
            bot.sendMessage(resultat, false);
        }
    }

    public static String search(String query) throws IOException {

        URLConnection urlConn;
        try {
            urlConn = new URL("http", "www.gulesider.no", "/tk/search.c?q="+query+"").openConnection();
        } catch(MalformedURLException ex) {
            System.err.println("Grouphug confession error: MalformedURLException in partially dynamic URL in search()!");
            return null;
        }

        urlConn.setConnectTimeout(CONN_TIMEOUT);
        urlConn.setRequestProperty("User-Agent", "Firefox/3.0"); // Trick google into thinking we're a proper browser. ;)

        BufferedReader gulesider = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

        System.out.println("OK");
        
        String curLine = " ";
        int status = 0;
        String output = "";
        while (status < 2){
            curLine = gulesider.readLine();
            if (curLine == null) 
                return null;
            String errorSearch = "noResult";
            int errorIndex = curLine.indexOf(errorSearch);
            
            if(errorIndex != -1)
                return null;

            if (status == 0) 
            {
                String resultSearch = "oppfnavn";
                int resultIndex = curLine.indexOf(resultSearch);
                output = gulesider.readLine().trim() + " , ";
                if (resultIndex != -1)
                    status = 1;
            }
            else {            
                String resultSearch = "Send SMS til nummer";
                int resultIndex = curLine.indexOf(resultSearch);
                if (resultIndex != -1)
                {
                    output += gulesider.readLine().trim();
                    return output;
                }
            }
        }
        

    }
}