package no.kvikshaug.gh.util;

import java.io.*;

public class IO {
    private static final int BUFFER_SIZE = 8192;
    private static final char[] charBuffer = new char[BUFFER_SIZE];
    private static final byte[] byteBuffer = new byte[BUFFER_SIZE];

    public static synchronized void copy(InputStream in, OutputStream out) throws IOException {
        while (in.read(byteBuffer) != -1) {
            out.write(byteBuffer);
        }
    }

    public static synchronized void copy(Reader reader, Writer writer) throws IOException {
        while(reader.read(charBuffer) != -1) {
            writer.write(charBuffer);
        }
    }

    public static void closeQuietly(Closeable in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ioe) {
                // ignore
            }
        }
    }

    public static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        return out.toByteArray();
    }
}
