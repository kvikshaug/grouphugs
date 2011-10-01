package no.kvikshaug.gh.util;

import java.io.*;

public class IO {
    private static final int BUFFER_SIZE = 8192;

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] byteBuffer = new byte[BUFFER_SIZE];
        int bytes = 0;
        while ((bytes = in.read(byteBuffer)) != -1) {
            out.write(byteBuffer, 0, bytes);
        }
        out.flush();
    }

    public static void copy(Reader reader, Writer writer) throws IOException {
        char[] charBuffer = new char[BUFFER_SIZE];
        int chars = 0;
        while((chars = reader.read(charBuffer)) != -1) {
            writer.write(charBuffer, 0, chars);
        }
        writer.flush();
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
