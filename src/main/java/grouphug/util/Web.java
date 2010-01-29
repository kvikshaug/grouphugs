package grouphug.util;

import static org.apache.commons.io.IOUtils.closeQuietly;
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

    /**
     * Fetches a web page for you and returns a nicely formatted arraylist when the whole
     * thing has loaded. This method has a default timeout value of 20 seconds.
     * @param urlString the url you want to look up.
     * @return an arraylist containing each line of the web site html
     * @throws java.io.IOException sometimes
     */
    public static ArrayList<String> fetchHtmlLines(String urlString) throws IOException {
        return fetchHtmlLines(urlString, 20000);
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
    public static ArrayList<String> fetchHtmlLines(String urlString, int timeout) throws IOException {
        BufferedReader input = prepareBufferedReader(urlString, timeout);

        ArrayList<String> lines = new ArrayList<String>();
        String htmlLine;
        while ((htmlLine = input.readLine()) != null) {
            lines.add(htmlLine);
        }
        input.close();
        return lines;
    }

    /**
     * Fetches a web page for you and returns a long string containing the full html source
     * when the whole thing has loaded. This method has a default timeout value of 20 seconds.
     * @param urlString the url you want to look up.
     * @return a string containing the entire html source
     * @throws java.io.IOException sometimes
     */
    public static String fetchHtmlLine(String urlString) throws IOException {
        return fetchHtmlLine(urlString, 20000);
    }

    /**
     * Fetches a web page for you and returns a long string containing the full html source
     * when the whole thing has loaded.
     * @param urlString the url you want to look up.
     * @param timeout an int that specifies the connect timeout value in milliseconds - if this time passes,
     * a SocketTimeoutException is raised.
     * @return an arraylist containing each line of the web site html
     * @throws java.io.IOException sometimes
     */
    public static String fetchHtmlLine(String urlString, int timeout) throws IOException {
        BufferedReader input = prepareBufferedReader(urlString, timeout);

        StringBuilder sb = new StringBuilder();
        String htmlLine;
        while ((htmlLine = input.readLine()) != null) {
            sb.append(htmlLine);
        }
        input.close();
        return sb.toString();
    }

    /**
     * Prepares a buffered reader for the inputstream of the specified website with a default
     * timeout value of 20 seconds.
     * This will return as soon as the connection is ready. Remember to close the reader!
     * @param urlString the url you want to look up.
     * @return the buffered reader for reading the input stream from the specified website
     * @throws java.io.IOException sometimes
     */
    public static BufferedReader prepareBufferedReader(String urlString) throws IOException {
        return prepareBufferedReader(urlString, 20000);
    }

    /**
     * Prepares a buffered reader for the inputstream of the specified website.
     * This will return as soon as the connection is ready. Remember to close the reader!
     * @param urlString the url you want to look up.
     * @param timeout an int that specifies the connect timeout value in milliseconds - if this time passes,
     * a SocketTimeoutException is raised.
     * @return the buffered reader for reading the input stream from the specified website
     * @throws java.io.IOException sometimes
     */
    public static BufferedReader prepareBufferedReader(String urlString, int timeout) throws IOException {
        urlString = urlString.replace(" ", "%20");

        URL url = new URL(urlString);
        System.out.println("Web util opening: '" + urlString + "'...");
        URLConnection urlConn = url.openConnection();

        urlConn.setConnectTimeout(timeout);
        // Pretend we're using a proper browser and OS :)
        urlConn.setRequestProperty("User-Agent", "Opera/9.80 (X11; Linux i686; U; en) Presto/2.2.15 Version/10.01");
        // alternative:
        // "MSIE 4.01; Digital AlphaServer 1000A 4/233; Windows NT; Powered By 64-Bit Alpha Processor"
        // (just saying)

        // TODO encoding should be specified dependent on what the site says it is! but we just assume utf-8 :)
        return new BufferedReader(new InputStreamReader(urlConn.getInputStream(), "UTF-8"));
    }

    /**
     * Perform a search on google
     * @param query the query to search for
     * @return a list over all URLs google provided
     * @throws IOException if I/O fails
     */
    public static ArrayList<URL> googleSearch(String query) throws IOException {
        String googleHtml = fetchHtmlLine("http://www.google.com/search?q="+query.replace(' ', '+'));

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
     * @param the location to find the weather forecast for
     * @return the next weather forecast from the rss.
     * @throws IOException if I/O fails
     */
    public static String weatherForecast(String location) throws IOException {
        String rssUrl = "http://www.yr.no" + location + "varsel.rss";
        rssUrl = rssUrl.replace("æ", "%C3%A6");
        rssUrl = rssUrl.replace("ø", "%C3%B8");
        rssUrl = rssUrl.replace("å", "%C3%85");
        String searchHtml = fetchHtmlLine(rssUrl);

        int searchIndex = searchHtml.indexOf("<description>");
        searchHtml = searchHtml.substring(searchIndex+2);
        searchIndex = searchHtml.indexOf("<description>");
        searchHtml = searchHtml.substring(searchIndex+13);
        searchIndex = searchHtml.indexOf("</description>");

        return searchHtml.substring(0,searchIndex);
    }

    /**
     * Searches for a valid yr.no's weather location.
     * @param the location to find.
     * @return A list with the results.
     * @throws IOException if I/O fails
     */
    public static ArrayList<String[]> weatherLocationSearch(String location) throws IOException {
        String searchHtml = fetchHtmlLine("http://www.yr.no/soek.aspx?sted="+location.replace(' ', '+'));
        ArrayList<String[]> results = new ArrayList<String[]>();

        if (searchHtml.indexOf("gav 0 treff") > -1)
            return results;

        String[] name = new String[3];
        int searchIndex = searchHtml.indexOf("<title>");

        if (searchHtml.substring(searchIndex + 7, searchIndex + 8).equals("S")) {
            // If we have multiple results.
            while (searchIndex > -1) {
                // Location
                searchIndex = searchHtml.indexOf("<a href=\"/sted/", searchIndex);
                name[0] = searchHtml.substring(searchIndex + 9, searchHtml.indexOf("\"", searchIndex + 9));

                // Altitude
                searchIndex = searchHtml.indexOf("<td>", searchIndex);
                name[1] = searchHtml.substring(searchIndex + 4, searchHtml.indexOf("</td>", searchIndex));

                // Description
                searchIndex = searchHtml.indexOf("<td>", searchIndex + 4);
                name[2] = searchHtml.substring(searchIndex + 4, searchHtml.indexOf("</td>", searchIndex));

                results.add(name);
                name = new String[3];

                // This is a fix for non-norwegian locations.
                searchIndex = searchHtml.indexOf("</td>", searchIndex);
                int c = 4;
                if (searchHtml.substring(searchIndex + 13, searchIndex + 19).equals("<td />"))
                    c--;
                // Let's skip ahead to the next result.
                for (int i = 0; i < c; i++)
                    searchIndex = searchHtml.indexOf("<a href=\"/sted/", searchIndex+1);
            }
        }
        else if (searchHtml.substring(searchIndex + 7, searchIndex + 8).equals("V")) {
            // If we only have one result.
            searchIndex = searchHtml.indexOf("href=\"/place/");
            name[0] = searchHtml.substring(searchIndex + 6, searchHtml.indexOf("\"", searchIndex + 6));

            searchIndex = searchHtml.indexOf("over havet");
            name[1] = searchHtml.substring(searchIndex + 28, searchHtml.indexOf(" ", searchIndex + 28));

            searchIndex = searchHtml.indexOf("Kategori");
            name[2] = searchHtml.substring(searchIndex + 25, searchHtml.indexOf("</li>", searchIndex + 28));

            name[2] = name[2].trim();
            results.add(name);
        }

        return results;
    }

    /**
     * Fetch the title for the specified URL
     * @param urlString the url to find a title in
     * @return the parsed title
     * @throws java.io.IOException if I/O fails
     * @throws org.jdom.JDOMException if parsing HTML fails
     */
    public static String fetchTitle(String urlString) throws IOException, JDOMException {
        URL url = new URL(urlString);
        return fetchTitle(url);
    }

    /**
     * Fetch the title for the specified URL
     * @param url the url to find a title in
     * @return the parsed title
     * @throws java.io.IOException if I/O fails
     * @throws org.jdom.JDOMException if parsing HTML fails
     */
    public static String fetchTitle(URL url) throws JDOMException, IOException {
        Reader r = null;
        try {
            r = CharEncoding.getReaderWithEncoding(url, DEFAULT_ENCODING);
        } catch (IOException ioe) {
            closeQuietly(r);
        }

        String title = null;

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
