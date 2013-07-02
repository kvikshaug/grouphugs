package no.kvikshaug.gh.util;

import no.kvikshaug.gh.Config;
import no.kvikshaug.gh.exceptions.NoTitleException;
import no.kvikshaug.gh.exceptions.PreferenceNotSetException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import static java.net.URLEncoder.encode;
import static no.kvikshaug.gh.util.IO.closeQuietly;
import static no.kvikshaug.gh.util.IO.copy;

/**
 * Web contains useful methods often performed by modules, like fetching the contents of a website,
 * performing a search on Google and anything else that might be handy to have here.
 */
public class Web {

    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final int DEFAULT_URLCONNECTION_TIMEOUT = 20000; // ms

    /**
     * Returns a JDOM document parsed via tagsoup
     *
     * @param url the url to return a document for
     * @return a JDOM document parsed via tagsoup
     * @throws IOException   if IO fails
     * @throws JDOMException when errors occur in parsing
     */
    public static Document getJDOMDocument(URL url) throws IOException, JDOMException {
        Reader r = prepareEncodedBufferedReader(url);

        // build a JDOM tree from the SAX stream provided by tagsoup
        SAXBuilder builder = new SAXBuilder("org.ccil.cowan.tagsoup.Parser");
        Document document = builder.build(r);
        closeQuietly(r);
        return document;
    }

    /**
     * Prepares a correctly encoded (if the encoding is guessed correctly) buffered reader
     * for the inputstream of the specified website.
     * This will return as soon as the connection is ready. Remember to close the reader!
     *
     * @param url the url you want to look up.
     * @return the buffered reader for reading the input stream from the specified website
     * @throws java.io.IOException sometimes
     */
    public static BufferedReader prepareEncodedBufferedReader(URL url) throws IOException {
        return (BufferedReader) CharEncoding.openStreamWithGuessedEncoding(
                prepareInputStream(url), DEFAULT_ENCODING);
    }

    /**
     * Prepares an inputstream for the specified URL, faking a user-agent request property.
     * Remember to close the inputstream!
     *
     * @param url the url you want to look up
     * @return a ready-to-read inputstream for the URL-connection
     * @throws IOException if I/O fails
     */
    public static InputStream prepareInputStream(URL url) throws IOException {
        System.out.println("Web util opening: '" + url.toString() + "'...");
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
        urlConn.setConnectTimeout(DEFAULT_URLCONNECTION_TIMEOUT);

        // HACK to avoid redirection on Spotify links
        if (url.getHost().equals("open.spotify.com")) {
            urlConn.setInstanceFollowRedirects(false);
        }

        // Pretend we are an old shitty browser so that google calculator uses the old layout
        urlConn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0)");
        // alternative:
        // "MSIE 4.01; Digital AlphaServer 1000A 4/233; Windows NT; Powered By 64-Bit Alpha Processor"
        // (just saying)

        return urlConn.getInputStream();
    }

    /**
     * Perform a search on google
     *
     * @param query the query to search for
     * @return a list over all URLs google provided
     * @throws IOException            if I/O fails
     * @throws org.jdom.JDOMException when errors occur in parsing
     */
    public static List<URL> googleSearch(String query) throws IOException, JDOMException {
        Document doc = getJDOMDocument(new URL("http://www.google.com/search?q=" + query.replace(' ', '+')));

        // find all a.l elements, which are result links
        XPath links = XPath.newInstance("//h:a[@class='l']");
        links.addNamespace("h", "http://www.w3.org/1999/xhtml");

        List<URL> urls = new ArrayList<URL>();
        for (Object element : links.selectNodes(doc)) {
            urls.add(new URL(((Element) element).getAttribute("href").getValue()));
        }
        return urls;
    }

    /**
     * Fetch the title for the specified URL.
     * Note that two new connections will be opened to the specified URL, one
     * to guess its encoding and another to read in the title.
     *
     * @param url the url to find a title in
     * @return the parsed title
     * @throws java.io.IOException    if I/O fails
     * @throws org.jdom.JDOMException if parsing HTML fails
     * @throws no.kvikshaug.gh.exceptions.NoTitleException
     *                                if there is no title to be found in the DOM
     */
    public static String fetchTitle(URL url) throws JDOMException, IOException, NoTitleException {
        String title;
        Document doc = getJDOMDocument(url);

        // find the <title> element using XPath
        XPath titlePath = XPath.newInstance("/h:html/h:head/h:title");
        titlePath.addNamespace("h", "http://www.w3.org/1999/xhtml");

        Element titleElement = (Element) titlePath.selectSingleNode(doc);
        if (titleElement == null) {
            throw new NoTitleException("No title element in DOM");
        }

        title = titleElement.getText();

        // remove all unnecessary whitespace
        title = title.replaceAll("\\s+", " ").trim();

        if (title.equals("")) {
            throw new NoTitleException("Title tag was empty or contained only whitespace");
        }

        return title;
    }

    /**
     * Find all URIs with a specific URI scheme in a String
     *
     * @param uriScheme the URI scheme to look for. (http://, git:// svn://, etc.)
     * @param string    the String to look for URIs in.
     * @return A List containing any URIs found.
     */
    public static List<String> findURIs(String uriScheme, String string) {
        List<String> uris = new ArrayList<String>();

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
     *
     * @param remoteFile     The full path to the remote file
     * @param localFileName  The name of the locally saved file (a number will be appended if it exists)
     * @param destinationDir The directory of which the local file will be saved
     * @throws IOException if an I/O error occurs
     */
    public static void downloadFile(String remoteFile, String localFileName, String destinationDir) throws IOException {
        URLConnection connection = new URL(remoteFile).openConnection();
        BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(destinationDir + "/" + localFileName));
        InputStream is = connection.getInputStream();
        int bytesRead, bytesWritten = 0;
        byte[] buf = new byte[1024];
        while ((bytesRead = is.read(buf)) != -1) {
            os.write(buf, 0, bytesRead);
            bytesWritten += bytesRead;
        }
        System.out.println("Downloaded file '" + remoteFile + "' to '" + destinationDir + '/' + localFileName + "' (" +
                bytesWritten + " bytes).");
        is.close();
        os.close();
    }


    public static String getBitlyURL(String url) {
        String bitlyUser;
        String apiKey;
        try {
            bitlyUser = Config.bitlyUser();
            apiKey = Config.bitlyApiKey();
        } catch (PreferenceNotSetException pnse) {
            return url;
        }
        try {
            String urlString = "http://api.bitly.com/v3/shorten?" +
                    "login=" + bitlyUser + "&apiKey=" + apiKey +
                    "&longUrl=" + encode(url, "UTF-8") +
                    "&format=txt";
            URL bitly = new URL(urlString);

            InputStream is = bitly.openStream();
            StringWriter writer = new StringWriter();
            copy(new InputStreamReader(is, "UTF-8"), writer);
            is.close();
            return writer.toString(); // short url
        } catch (MalformedURLException murle) {
            System.out.println("Malformed url in bitly-method");
            return url;
        } catch (IOException ioe) {
            System.out.println("IOException in bitly-method");
            return url;
        }
    }


    /**
     * Convert HTML entities to their respective characters
     *
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
