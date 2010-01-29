package grouphug.util;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.toByteArray;
import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsICharsetDetectionObserver;
import org.mozilla.intl.chardet.nsPSMDetector;

import java.io.*;
import java.net.URL;

/**
 * General utility class for handling different character encodings.
 *
 * CharEncoding uses the jchardet library for most of the hard bits. jchardet,
 * in turn, is based on the Mozilla universal character detection library.
 */
public class CharEncoding {
    public static final int BYTE_ARR_SIZE = 1024;

    /**
     * Try to guess the character encoding of the document located at url
     * @param url the url to examine
     * @return the name of the encoding we guessed
     * @throws IOException if we're unable to open the document at url, or if
     * reading the document fails at any point in time
     */
    public static String guessEncoding(URL url) throws IOException {
        return guessEncoding(url, null);
    }


    /**
     * Try to guess the character encoding of the document located at url, and
     * return a Reader for the stream is using said encoding
     * @param url the url to examine
     * @return a Reader using the guessed encoding, if guessing is
     * unsuccessful, a Reader using the system default encoding will be
     * returned
     * @throws IOException if we're unable to read is for whatever reason
     */
    public static Reader getReaderWithEncoding(URL url) throws IOException {
        return getReaderWithEncoding(url, null);
    }

    /**
     * Try to guess the character encoding of the document located at url, and
     * return a Reader for the stream is using said encoding
     * @param url the url to examine
     * @param defaultEncoding the character encoding to assume if guessing is
     * unsuccessful. If null, don't make any assumptions, return a Reader with
     * the system default encoding
     * @return a Reader using the guessed encoding, or defaultEncoding if
     * guessing is unsuccessful. if defaultEncoding is null and guessing is
     * unsuccessful, a Reader using the system default encoding will be
     * returned
     * @throws IOException if we're unable to read is for whatever reason
     */
    public static Reader getReaderWithEncoding(URL url, String defaultEncoding) throws IOException {
        return openStreamWithGuessedEncoding(Web.prepareInputStream(url), defaultEncoding);
    }

    /**
     * Try to guess the character encoding of the document located at url
     * @param url the url to examine
     * @param defaultEncoding the encoding to assume if guessing is
     * unsuccessful. If null, don't make any assumptions, return null on
     * unsuccessful guess
     * @return the name of the encoding we guessed
     * @throws IOException if we're unable to open the document at url, or if
     * reading the document fails at any point in time
     */
    public static String guessEncoding(URL url, String defaultEncoding) throws IOException {
        return guessEncoding(Web.prepareInputStream(url), defaultEncoding);
    }


    /**
     * Try to guess the character encoding with which s is encoded
     * @param s the String to examine
     * @return the name of the encoding we guessed
     * @throws IOException if reading the String as a stream fails
     */
    public static String guessEncoding(String s) throws IOException {
        return guessEncoding(s, null);
    }

    /**
     * Try to guess the character encoding with which s is encoded
     * @param s the String to examine
     * @param defaultEncoding the character encoding to assume if guessing is
     * unsuccessful. If null, don't make any assumptions, return null on
     * unsuccessful guess
     * @return the name of the encoding we guessed
     * @throws IOException if reading the String as a stream fails
     */
    public static String guessEncoding(String s, String defaultEncoding) throws IOException {
        BufferedInputStream is = new BufferedInputStream(
                new ByteArrayInputStream(s.getBytes()));

        return guessEncoding(is, defaultEncoding);
    }

    /**
     * Try to guess the character encoding of the bytes in the stream is, and
     * return a Reader for the stream is using said encoding
     * @param is the stream of bytes to examine
     * @return a Reader using the guessed encoding, or the system default
     * encoding if guessing is unsuccessful
     * @throws IOException if we're unable to read is for whatever reason
     */
    public static Reader openStreamWithGuessedEncoding(InputStream is) throws IOException {
        return openStreamWithGuessedEncoding(is, null);
    }

    /**
     * Try to guess the character encoding of the bytes in the stream is, and
     * return a Reader for the stream is using said encoding
     * @param is the stream of bytes to examine
     * @param defaultEncoding the character encoding to assume if guessing is
     * unsuccessful. If null, don't make any assumptions, return a Reader with
     * the system default encoding
     * @return a Reader using the guessed encoding, or defaultEncoding if
     * guessing is unsuccessful. if defaultEncoding is null and guessing is
     * unsuccessful, a Reader using the system default encoding will be
     * returned
     * @throws IOException if we're unable to read is for whatever reason
     */
    public static Reader openStreamWithGuessedEncoding(InputStream is, String defaultEncoding) throws IOException {
        byte[] buffer = toByteArray(is);
        closeQuietly(is);

        InputStream guessStream = new BufferedInputStream(new ByteArrayInputStream(buffer));
        InputStream readerStream = new BufferedInputStream(new ByteArrayInputStream(buffer));

        String encoding = guessEncoding(guessStream, defaultEncoding);

        if (encoding != null) {
            return new BufferedReader(new InputStreamReader(readerStream, encoding));
        } else {
            if (defaultEncoding != null) {
                return new BufferedReader(new InputStreamReader(readerStream, defaultEncoding));
            } else {
                return new BufferedReader(new InputStreamReader(readerStream));
            }
        }
    }

    /**
     * Try to guess the character encoding of the bytes in the stream is
     * 
     * NOTE that this method reads bytes from and closes is, making it
     * useless after it returns. So if you need the data in is, you have to
     * buffer it. Take a look at openStreamWithGuessedEncoding() if you want
     * an example of how to do that.
     *
     * @param is the stream of bytes to examine
     * @return the name of the encoding we guessed
     * @throws IOException if we're unable to read is for whatever reason
     */
    public static String guessEncoding(InputStream is) throws IOException {
        return guessEncoding(is, null);
    }

    /**
     * Try to guess the character encoding of the bytes in the stream is
     *
     * NOTE that this method reads bytes from and closes is, making it
     * useless after it returns. So if you need the data in is, you have to
     * buffer it. Take a look at openStreamWithGuessedEncoding() if you want
     * an example of how to do that.
     *
     * @param is the stream of bytes to examine
     * @param defaultEncoding the encoding to assume if we're unable to guess
     * an encoding.
     * @return the name of the encoding we guessed
     * @throws IOException if we're unable to read is for whatever reason
     */
    public static String guessEncoding(InputStream is, String defaultEncoding) throws IOException {
        nsDetector det = new nsDetector(nsPSMDetector.ALL);
        CharsetDetectionObserver cdo =  new CharsetDetectionObserver(defaultEncoding);
        det.Init(cdo);

        BufferedInputStream bis = new BufferedInputStream(is);

        byte[] buf = new byte[BYTE_ARR_SIZE];
        int len;
        boolean isAscii = true;
        boolean done = false;

        while ((len = bis.read(buf, 0, buf.length)) != -1) {
                if (isAscii)
                    isAscii = det.isAscii(buf,len);

                if (!isAscii && !done)
                    done = det.DoIt(buf, len, false);
        }
        det.DataEnd();

        closeQuietly(is);

        System.out.println("CharEncoding guesses '"+cdo.getEncoding()+"'.");
        return cdo.getEncoding();
    }

    /**
     * Used for the guessEncoding methods.
     */
    static class CharsetDetectionObserver implements nsICharsetDetectionObserver {
        private String encoding;

        CharsetDetectionObserver() {
            this.encoding = null;
        }

        /**
         * @param defaultEncoding the CharsetDetectionObserver assumes defaultEncoding if no
         * defaultEncoding can be found.
         */
        CharsetDetectionObserver(String defaultEncoding) {
            this.encoding = defaultEncoding;
        }

        /*
         * This method is automatically called by the nsDetector when a
         * character encoding is found.
         */
        @Override
        public void Notify(String s) {
            this.encoding = s;
        }

        /**
         * @return returns the detected encoding, or DEFAULT_ENCODING if no
         * encoding was detected.
         */
        public String getEncoding() {
            return this.encoding;
        }
    }
}
