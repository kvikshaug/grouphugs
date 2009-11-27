package grouphug.util;

import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsICharsetDetectionObserver;
import org.mozilla.intl.chardet.nsPSMDetector;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * Web contains useful methods often performed by modules, like fetching the contents of a website,
 * performing a search on Google and anything else that might be handy to have here.
 */
public class Web {

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
     * Fetch the title for the specified URL
     * @param urlString the url to find a title in
     * @return the parsed title
     * @throws java.io.IOException if I/O fails
     */
    public static String fetchTitle(String urlString) throws IOException {
        urlString = urlString.replace(" ", "%20");

        URL url = new URL(urlString);
        System.out.println("Web util opening: '" + urlString + "'...");
        URLConnection urlConn = url.openConnection();

        urlConn.setConnectTimeout(20000); // set to 20 seconds, we're GUESSING that that's what we want
        // Pretend we're using a proper browser and OS :)
        urlConn.setRequestProperty("User-Agent", "Opera/9.80 (X11; Linux i686; U; en) Presto/2.2.15 Version/10.01");

        // assume utf-8, we're still GUESSING
        BufferedReader input = new BufferedReader(new InputStreamReader(urlConn.getInputStream(), "UTF-8"));

        // check the content-type
        if(urlConn.getContentType().startsWith("image") ||
                urlConn.getContentType().startsWith("audio") ||
                urlConn.getContentType().startsWith("video")) {
            throw new IllegalArgumentException("URL of content-type " + urlConn.getContentType() + " isn't likely " +
                    "to contain an html title.");
        }

        String htmlLine;
        int titleIndex;
        while ((htmlLine = input.readLine()) != null) {
            if((titleIndex = htmlLine.indexOf("<title")) != -1 ||
                    (titleIndex = htmlLine.indexOf("<TITLE")) != -1 ||
                    (titleIndex = htmlLine.indexOf("<Title")) != -1) {
                String title;
                int startIndex = htmlLine.indexOf('>', titleIndex) + 1;
                int endIndex;
                if((endIndex = htmlLine.indexOf('<', startIndex)) != -1) {
                    // title is on one line
                    title = htmlLine.substring(startIndex, endIndex);
                } else {
                    // title spans multiple lines (booo *throws tomatoes at website*)
                    title = htmlLine.substring(startIndex);
                    htmlLine = input.readLine();
                    while((endIndex = htmlLine.indexOf('<')) == -1) {
                        title += htmlLine + "\n";
                        htmlLine = input.readLine();
                        if(htmlLine == null) {
                            throw new IOException("Unable to find end tag for title (this is highly unusual!)");
                        }
                    }
                    title += htmlLine.substring(0, endIndex);
                }
                input.close();
                return entitiesToChars(title.replaceAll("\\s+", " ").trim());
            }
        }
        throw new IOException("Unable to find title start tag.");
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

    /**
     * Tries to guess the character encoding of the document located at url.
     *
     * @param url the url to examine
     * @return the character encoding detected.
     */
    public static String guessEncoding(URL url) {
        CharsetDetectionObserver cdo = new CharsetDetectionObserver();
        return guessEncoding(url, cdo);
    }

    private static String guessEncoding(URL url, CharsetDetectionObserver cdo) {
        // Initialize the nsDetector() ;
        nsDetector det = new nsDetector(nsPSMDetector.ALL) ;

        // Set an observer...
        det.Init(cdo);

        BufferedInputStream imp = null;
        try {
            imp = new BufferedInputStream(url.openStream());
        } catch (IOException e) {
            System.err.println("[guessEncoding]: caught IOException while trying to guess encoding");
        }

        byte[] buf = new byte[1024];
        int len;
        boolean done = false ;
        boolean isAscii = true ;

        if (imp == null) { return cdo.getEncoding(); } /* will return UTF-8 */

        try {
            while((len = imp.read(buf, 0, buf.length)) != -1) {
                // Check if the stream is only ascii.
                if (isAscii)
                    isAscii = det.isAscii(buf,len);

                // DoIt if non-ascii and not done yet.
                if (!isAscii && !done)
                    done = det.DoIt(buf, len, false);
            }
        } catch (IOException e) {
            System.err.println("[guessEncoding]: caught IOException while trying to guess encoding");
        }
        det.DataEnd();

        return ((CharsetDetectionObserver)cdo).getEncoding();
    }

    /**
     * Used for the guessEncoding method.
     */
    static class CharsetDetectionObserver implements nsICharsetDetectionObserver {
        private String encoding = "UTF-8";

        /*
         * will be called when a matching encoding is found.
         */
        @Override
        public void Notify(String s) {
            this.encoding = s;
            System.out.println("[URLCatcher]: found encoding: " + s);
        }

        /**
         * @return returns the detected encoding, UTF-8 if no encoding was detected
         */
        public String getEncoding() {
            return this.encoding;
        }
    }
}
