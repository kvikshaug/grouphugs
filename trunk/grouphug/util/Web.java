package grouphug.util;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

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
            System.err.println("No site found at:" + url);
            return null;
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace(System.err); //TODO proper IOException handling
        }
        finally {
            try { if(input != null) input.close(); } catch (IOException ioeIsIgnored) { /*IDEA code inspection nags*/ }
        }
        
        return html.toString();
    }

}
