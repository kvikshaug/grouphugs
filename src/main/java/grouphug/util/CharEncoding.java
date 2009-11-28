package grouphug.util;

import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsICharsetDetectionObserver;
import org.mozilla.intl.chardet.nsPSMDetector;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 *
 */
public class CharEncoding {
    /**
     * Assume this encoding if we're unable to guess an encoding.
     */
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
     * Try to guess the character encoding of the document located at url
     * @param url the url to examine
     * @param defaultEncoding the encoding to assume if guessing is
     * unsuccessful. If null, don't make any assumptions, return null on
     * unsuccessful guess
     * @return the name of the encoding we guessed
     * @throws IOException if we're unable to open the document at url, or if
     * reading the document fails at any point in time
     */
    private static String guessEncoding(URL url, String defaultEncoding) throws IOException {
        return guessEncoding(url.openStream(), defaultEncoding);
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
     * Try to guess the character encoding of the bytes in the stream is
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
         * @param encoding the CharsetDetectionObserver assumes encoding if no
         * encoding can be found.
         */
        CharsetDetectionObserver(String encoding) {
            this.encoding = encoding;
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
