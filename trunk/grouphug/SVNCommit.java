package grouphug;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * SVNOutput: A module that parses and outputs information about an SVN commit.
 *
 * Upon creation, this object creates and starts a thread. This threads job is to listen for
 * connections by another program I've made for this. The specified program will run when svn
 * recieves a commit. It then connects to this program, sends "SVN_DATA", recieves "OK" and
 * then sends data about the svn commit in the following syntax:
 *
 * ------------------------------------------------------------------------
 * r40 | username | 2008-09-07 19:52:28 +0200 (Sun, 07 Sep 2008) | 1 line
 * Changed paths:
 *    M /trunk/grouphug/Modified.java
 *    D /trunk/grouphug/Deleted.java
 *    A /trunk/grouphug/Added.java
 *
 * This is the comment provided with the commit.
 * ------------------------------------------------------------------------
 *
 * When all data is sent, the client sends "KTHXBYE" and closes the connection.
 * This class parses the provided data and outputs it on the gh channel.
 */

class SVNCommit implements Runnable {

    public static boolean run; // should be set to false when we want to stop this thread.
    private static final int MAX_FAILS = 3; // Max no. of fails allowed by the listener before aborting

    private static Grouphug bot;

    protected static void load(Grouphug bot) {
        // Create and start a thread
        SVNCommit.bot = bot;
        run = true;
        new Thread(new SVNCommit()).start();
    }

    public void run() {
        int fails = 0;
        ServerSocket serverSocket;
        Socket clientSocket;
        PrintWriter out;
        BufferedReader in;

        while(run) {

            if(fails >= MAX_FAILS) {
                System.err.println("SVNOutput listener failed more than 3 times, aborting.");
                break;
            }

            try {
                serverSocket = new ServerSocket(4445);
                System.out.println("SVNOutput listening at 4445.");

                clientSocket = serverSocket.accept();
                System.out.println("SVNOutput Accepted connection.");

                // If we get here, the connection has been accepted, so reset the fail counter
                fails = 0;

                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String inputLine;
                ArrayList<String> input = new ArrayList<String>();

                System.out.println("Reading line");

                inputLine = in.readLine();
                if(inputLine == null || !inputLine.equals("SVN_DATA"))
                    throw new NullPointerException("Wrong data recieved by client.");

                System.out.println("Read line, sending OK");

                out.println("OK");

                while ((inputLine = in.readLine()) != null) {
                    if(inputLine.equals("KTHXBYE"))
                        break;
                    input.add(inputLine);
                }

                out.close();
                in.close();
                serverSocket.close();
                clientSocket.close();

                SVNCommitItem data;
                try {
                    data = parse(input);
                    bot.sendMessage(data.toString(), false);
                } catch(Exception e) {
                    System.err.println("Error: Unable to parse input from SVNOutput listener!");
                    e.printStackTrace();
                }

            } catch (IOException e) {
                fails++;
                System.err.println("Error: Caught IOException when starting SVNOutput listener; incrementing fails to "+fails);
                try {
                    Thread.sleep(5000); // sleep for 5 seconds before retrying
                } catch(InterruptedException ex) {
                    // do nothing; just continue
                }
            } catch(NullPointerException e) {
                System.err.println("Nullpointer exception catched in the SVNOutput listener, restarting socket.");
            } finally {
                out = null;
                in = null;
                serverSocket = null;
                clientSocket = null;
            }
        }
    }

    private SVNCommitItem parse(ArrayList<String> input) throws Exception {
        if(!input.get(0).startsWith("---"))
            throw new Exception("Unable to parse input - unexpected data at line 1.");
        if(!input.get(1).startsWith("r"))
            throw new Exception("Unable to parse input - unexpected data at line 2.");

        // Find the first and second separator
        int sep1 = input.get(1).indexOf(" | ");
        int sep2 = input.get(1).indexOf(" | ", sep1+1);

        // Revision is between 2nd char and 1st seperator
        int revision = Integer.parseInt(input.get(1).substring(1, sep1));

        // Username is between 1st and 2nd separator
        String username = input.get(1).substring(sep1+3, sep2);

        // Initiate the files container
        ArrayList<SVNCommitItemFile> files = new ArrayList<SVNCommitItemFile>();

        // Now start at line 3 and continue until the current line is empty. All of these lines are files
        int line;
        for(line = 3; !(input.get(line).trim()).equals(""); line++) {
            // The modification char is at index 3
            char modChar = input.get(line).charAt(3);

            // The filename starts at index 5
            String filename = input.get(line).substring(5);

            // Create the new fileobject and add it to the filelist
            files.add(new SVNCommitItemFile(modChar, filename));
        }

        // The comment will now start on line "line" + 1.
        line++;

        // However, the comment might be several lines, so we track down the last line.
        int endLine = line;
        while(!input.get(endLine).startsWith("---"))
            endLine++;

        // Now, concatenate all the comment-lines
        String comment = "";
        for(; line < endLine; line++) {
            comment += input.get(line)+"\n";
        }

        // We have all the data we wanted, so create and return the new commit-data
        return new SVNCommitItem(revision, username, files, comment);
    }
}
