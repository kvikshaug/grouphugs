package grouphug.util;

import static org.apache.commons.io.IOUtils.closeQuietly;
import org.apache.commons.io.IOUtils;

import org.jdom.Document;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * Web contains useful methods often performed by modules, like fetching the contents of a website,
 * performing a search on Google and anything else that might be handy to have here.
 */
public class Web {

    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final int DEFAULT_URLCONNECTION_TIMEOUT = 20000; // ms

    /**
     * Fetches a web page for you and returns a nicely formatted arraylist with each
     * line as its own entry when the whole thing has loaded.
     * @param url the url you want to look up.
     * @return an arraylist containing each line of the web site html
     * @throws java.io.IOException sometimes
     */
    public static ArrayList<String> fetchHtmlLines(URL url) throws IOException {
        BufferedReader input = prepareEncodedBufferedReader(url);
        ArrayList<String> lines = new ArrayList<String>();
        String htmlLine;
        while ((htmlLine = input.readLine()) != null) {
            lines.add(htmlLine);
        }
        closeQuietly(input);
        return lines;
    }

    /**
     * Fetches a web page for you and returns a long string containing the full html source
     * when the whole thing has loaded, including newline characters.
     * @param url the url you want to look up.
     * @return a long string containing the full html source of the specified url
     * @throws java.io.IOException sometimes
     */
    public static String fetchHtmlLine(URL url) throws IOException {
        InputStream in = prepareInputStream(url);
        String line = IOUtils.toString(in, CharEncoding.guessEncoding(url));
        closeQuietly(in);
        return line;
    }

    /**
     * Prepares a correctly encoded (if the encoding is guessed correctly) buffered reader
     * for the inputstream of the specified website.
     * This will return as soon as the connection is ready. Remember to close the reader!
     * @param url the url you want to look up.
     * @return the buffered reader for reading the input stream from the specified website
     * @throws java.io.IOException sometimes
     */
    public static BufferedReader prepareEncodedBufferedReader(URL url) throws IOException {
        return (BufferedReader)CharEncoding.openStreamWithGuessedEncoding(
                prepareInputStream(url), DEFAULT_ENCODING);
    }

    /**
     * Prepares an inputstream for the specified URL, faking a user-agent request property.
     * Remember to close the inputstream! 
     * @param url the url you want to look up
     * @return a ready-to-read inputstream for the URL-connection
     * @throws IOException if I/O fails
     */
    public static InputStream prepareInputStream(URL url) throws IOException {
        System.out.println("Web util opening: '" + url.toString() + "'...");
        //try { throw new Exception(); } catch(Exception e) { e.printStackTrace(); }
        URLConnection urlConn = url.openConnection();
        urlConn.setConnectTimeout(DEFAULT_URLCONNECTION_TIMEOUT);

        // Pretend we're using a proper browser and OS :)
        urlConn.setRequestProperty("User-Agent", "Opera/9.80 (X11; Linux i686; U; en) Presto/2.2.15 Version/10.01");
        // alternative:
        // "MSIE 4.01; Digital AlphaServer 1000A 4/233; Windows NT; Powered By 64-Bit Alpha Processor"
        // (just saying)

        return urlConn.getInputStream();
    }

    /**
     * Perform a search on google
     * @param query the query to search for
     * @return a list over all URLs google provided
     * @throws IOException if I/O fails
     */
    public static ArrayList<URL> googleSearch(String query) throws IOException {
        String googleHtml = fetchHtmlLine(new URL("http://www.google.com/search?q="+query.replace(' ', '+'))).replace("\n", "");

        String parseSearch = "<h3 class=r><a href=\"";
        int searchIndex = 0;

        ArrayList<URL> urls = new ArrayList<URL>();
        while((searchIndex = googleHtml.indexOf(parseSearch, searchIndex+1)) != -1) {
            urls.add(new URL(googleHtml.substring(searchIndex + parseSearch.length(), googleHtml.indexOf('"', searchIndex + parseSearch.length()))));
        }
        return urls;
    }

    /**
     * Presents the next weather forecast for a given location using yr.no's rss.
     * @param location the location to find the weather forecast for
     * @return the weather forecast or blank if location not found.
     * @throws IOException if I/O fails
     */
    public static String weather(String location) throws IOException {
        String searchHtml = fetchHtmlLine(new URL("http://www.yr.no/soek.aspx?sted="+location.replace(' ', '+'))).replace("\n", "");
        
        try {
            int searchIndex = searchHtml.indexOf("<a href=\"/sted/");
            searchHtml = searchHtml.substring(searchIndex+9,searchIndex+100);
            searchIndex = searchHtml.indexOf("\"");
            searchHtml = searchHtml.substring(0,searchIndex);

            // All locations should start with a slash.
            if (!searchHtml.startsWith("/"))
                    return "";
            
            String rssUrl = "http://www.yr.no" + searchHtml + "varsel.rss";
            rssUrl = rssUrl.replace("æ", "%C3%A6");
            rssUrl = rssUrl.replace("ø", "%C3%B8");
            rssUrl = rssUrl.replace("å", "%C3%85");
            searchHtml = fetchHtmlLine(new URL(rssUrl)).replace("\n", "");

            searchIndex = searchHtml.indexOf("<description>");
            searchHtml = searchHtml.substring(searchIndex+2);
            searchIndex = searchHtml.indexOf("<description>");
            searchHtml = searchHtml.substring(searchIndex+13);
            searchIndex = searchHtml.indexOf("</description>");

            return location + ": " + searchHtml.substring(0,searchIndex);
        } catch (java.lang.StringIndexOutOfBoundsException e) {
            return "";
        }
    }

    /**
     * Fetch the title for the specified URL.
     * Note that two new connections will be opened to the specified URL, one
     * to guess its encoding and another to read in the title.
     * @param url the url to find a title in
     * @return the parsed title
     * @throws java.io.IOException if I/O fails
     * @throws org.jdom.JDOMException if parsing HTML fails
     */
    public static String fetchTitle(URL url) throws JDOMException, IOException {
        Reader r = prepareEncodedBufferedReader(url);
        String title;

        // build a JDOM tree from the SAX stream provided by tagsoup
        SAXBuilder builder = new SAXBuilder("org.ccil.cowan.tagsoup.Parser");
        Document doc = builder.build(r);

        // find the <title> element using XPath
        XPath titlePath = XPath.newInstance("/h:html/h:head/h:title");
        titlePath.addNamespace("h","http://www.w3.org/1999/xhtml");

        title = ((Element)titlePath.selectSingleNode(doc)).getText();

        closeQuietly(r);

        // do some tidying up
        if (title != null) {
            // strip trailing whitespace
            title = title.trim();

            // strip newlines
            title = title.replaceAll("\r\n", " ");
            title = title.replaceAll("\n", " ");

            // strip tabs
            title = title.replaceAll("\t", " ");
        }

        return title;
    }

    /**
     * Find all URIs with a specific URI scheme in a String
     *
     * @param uriScheme the URI scheme to look for. (http://, git:// svn://, etc.)
     * @param string the String to look for URIs in.
     * @return An ArrayList containing any URIs found.
     */
    public static ArrayList<String> findURIs(String uriScheme, String string) {
        ArrayList<String> uris = new ArrayList<String>();

        int index = 0;
        do {
            index = string.indexOf(uriScheme, index); // find the start index of a URL

            if (index == -1) // if indexOf returned -1, we didn't find any urls
                break;

            int endIndex = string.indexOf(" ", index); // find the end index of a URL (look for a space character)
            if (endIndex == -1)             // if indexOf returned -1, we didnt find a space character, so we set the
                endIndex = string.length(); // end of the URL to the end of the string

            uris.add(string.substring(index, endIndex));

            index = endIndex; // start at the end of the URL we just added
        } while (true);

        return uris;
    }

    /**
     * Save a remote file on the local filesystem
     * @param remoteFile The full path to the remote file
     * @param localFileName The name of the locally saved file (a number will be appended if it exists)
     * @param destinationDir The directory of which the local file will be saved
     * @throws IOException if an I/O error occurs
     */
    public static void downloadFile(String remoteFile, String localFileName, String destinationDir) throws IOException {
        URLConnection connection = new URL(remoteFile).openConnection();
        BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(destinationDir + "/" + localFileName));
        InputStream is = connection.getInputStream();
        int bytesRead, bytesWritten = 0;
        byte[] buf = new byte[1024];
        while((bytesRead = is.read(buf)) != -1) {
            os.write(buf, 0, bytesRead);
            bytesWritten += bytesRead;
        }
        System.out.println("Downloaded file '"+remoteFile+"' to '"+destinationDir+'/'+localFileName+"' ("+
                bytesWritten+" bytes).");
        is.close();
        os.close();
    }


    /**
     * Convert HTML entities to their respective characters
     * @param str The unconverted string
     * @return The converted string
     */
    public static String entitiesToChars(String str) {
        return str.replace("&amp;", "&")
                .replace("&nbsp;", " ")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&mdash;", " - ")
                .replace("&laquo;", "«")
                .replace("&lsaquo;", "‹")
                .replace("&raquo;", "»")
                .replace("&rsaquo;", "›")
                .replace("&rsquo;", "'")
                .replace("&aelig;", "æ")
                .replace("&Aelig;", "Æ")
                .replace("&oslash;", "ø")
                .replace("&Oslash;", "Ø")
                .replace("&aring;", "å")
                .replace("&Aring;", "Å")
                .replace("&#x22;", "\"")
                .replace("&#x27;", "'")
                .replace("&#34;", "\"")
                .replace("&#39;", "'")
                .replace("&#039;", "'")
                .replace("&#228;", "ä")
                .replace("&#8212;", " - ")
                .replace("&#8216;", "'")
                .replace("&#8217;", "'")
                .replace("&#8220;", "\"")
                .replace("&#8221;", "\"")
                .replace("&#8230;", "...");
    }
}
