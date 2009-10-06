package grouphug.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Utilities for interacting with web pages etc.
 *
 * Feel free to add useful methods here.
 *
 */
public class Web
{

    /**
     * Fetches a web page for you and returns a nicely formatted arraylist when the whole
     * thing has loaded. This method has a default timeout value of 20 seconds.
     * @param urlString the url you want to look up.
     * @return an arraylist containing each line of the web site html
     * @throws java.io.IOException sometimes
     */
    public static ArrayList<String> fetchHtmlList(String urlString) throws IOException {
        return fetchHtmlList(urlString, 20000);
    }

    /**
     * Fetches a web page for you and returns a nicely formatted arraylist when the whole
     * thing has loaded.
     * @param urlString the url you want to look up.
     * @param timeout an int that specifies the connect timeout value in milliseconds - if this time passes,
     * a SocketTimeoutException is raised.
     * @return an arraylist containing each line of the web site html
     * @throws java.io.IOException sometimes
     */
    public static ArrayList<String> fetchHtmlList(String urlString, int timeout) throws IOException {
        urlString = urlString.replace(" ", "%20");

        URL url = new URL(urlString);
        System.out.println("Opening: " + urlString + " ...");
        URLConnection urlConn = url.openConnection();

        urlConn.setConnectTimeout(timeout);
        urlConn.setRequestProperty("User-Agent", "Firefox/3.0"); // Pretend we're a proper browser :)

        BufferedReader input = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

        ArrayList<String> lines = new ArrayList<String>();
        String htmlLine;
        while ((htmlLine = input.readLine()) != null) {
            lines.add(htmlLine);
        }
        input.close();
        return lines;
    }


    /**
     * Tries to fetch a HTML page from a URL.
     * @param url the url we want to look up.
     * @return the HTML of the page located at url stored in a String. null if something unexpected happens, like having
     *         to catch a IOException or a MalformedURLException.
     */
    public static String fetchHTML(String url)
    {
        URL realURL;
        try
        {
            realURL = new URL(url);
        }
        catch (MalformedURLException mURLe)
        {
            System.err.println("The URL passed to fetchHTML was not well-formed: " + mURLe);
            mURLe.printStackTrace(System.err);

            return null;
        }

        StringBuilder html = new StringBuilder();
        BufferedReader input = null;
        try
        {
            input = new BufferedReader(new InputStreamReader(realURL.openStream()));

            String htmlLine;
            while ((htmlLine = input.readLine()) != null) {
                html.append(htmlLine);
            }
        }
        catch (UnknownHostException uhe)
        {
            //System.err.println("No site found at:" + url);
            return null; // fail silently
        }
        catch (IOException ioe)
        {
            //ioe.printStackTrace(System.err); //TODO proper IOException handling
            return null; // fail silently
        }
        finally {
            try { if(input != null) input.close(); } catch (IOException ioeIsIgnored) { /*IDEA code inspection nags*/ }
        }

        return html.toString();
    }

    /**
     * Find all URIs with a specific URI scheme in a String
     *
     * @param uriScheme the URI scheme to look for. (http://, git:// svn://, etc.)
     * @param string the String to look for URIs in.
     * @return An ArrayList containing any URIs found.
     */
    public static ArrayList<String> findURIs(String uriScheme, String string)
    {
        ArrayList<String> uris = new ArrayList<String>();

        int index = 0;
        do
        {
            index = string.indexOf(uriScheme, index); // find the start index of a URL

            if (index == -1) // if indexOf returned -1, we didn't find any urls
                break;

            int endIndex = string.indexOf(" ", index); // find the end index of a URL (look for a space character)
            if (endIndex == -1)             // if indexOf returned -1, we didnt find a space character, so we set the
                endIndex = string.length(); // end of the URL to the end of the string

            uris.add(string.substring(index, endIndex));

            index = endIndex; // start at the end of the URL we just added
        }
        while (true);

        return uris;
    }

    /**
     * Convert HTML entities to their respective characters
     * @param str The unconverted string
     * @return The converted string
     */
    public static String entitiesToChars(String str) {
        str = str.replace("&amp;", "&");
        str = str.replace("&nbsp;", " ");
        str = str.replace("&#8216;", "'");
        str = str.replace("&#8217;", "'");
        str = str.replace("&#8220;", "\"");
        str = str.replace("&#8221;", "\"");
        str = str.replace("&#8230;", "...");
        str = str.replace("&#8212;", " - ");
        str = str.replace("&mdash;", " - ");
        str = str.replace("&quot;", "\"");
        str = str.replace("&apos;", "'");
        str = str.replace("&lt;", "<");
        str = str.replace("&gt;", ">");
        str = str.replace("&#34;", "\"");
        str = str.replace("&#39;", "'");
        str = str.replace("&laquo;", "«");
        str = str.replace("&lsaquo;", "‹");
        str = str.replace("&raquo;", "»");
        str = str.replace("&rsaquo;", "›");
        str = str.replace("&aelig;", "æ");
        str = str.replace("&Aelig;", "Æ");
        str = str.replace("&aring;", "å");
        str = str.replace("&Aring;", "Å");
        str = str.replace("&oslash;", "ø");
        str = str.replace("&Oslash;", "Ø");
        str = str.replace("&#228;", "ä");
        return str;
    }
}
