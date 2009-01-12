package grouphug.util;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
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

}
