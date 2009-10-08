package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.listeners.TriggerListener;
import grouphug.util.Web;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class Slang implements TriggerListener {

    private static final String TRIGGER_HELP = "slang";
    private static final String TRIGGER_MAIN = "slang";
    private static final String TRIGGER_EXAMPLE = "-ex";

    private static int slangCount = 0;

    public Slang(ModuleHandler moduleHandler) {
        moduleHandler.addTriggerListener(TRIGGER_MAIN, this);
        moduleHandler.registerHelp(TRIGGER_HELP, "Slang: Define an expression in slang terms.\n" +
                   "  " + Grouphug.MAIN_TRIGGER + TRIGGER_MAIN + "<expr>\n" +
                   "  " + Grouphug.MAIN_TRIGGER + TRIGGER_MAIN + "<expr> <number>\n" +
                   "  " + Grouphug.MAIN_TRIGGER + TRIGGER_MAIN + Slang.TRIGGER_EXAMPLE + "<expr>");
        System.out.println("Slang module loaded.");
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message) {
        boolean includeExample;
        if(message.contains(TRIGGER_EXAMPLE)) {
            includeExample = true;
            message = message.replace("-ex", "").trim();
        } else {
            includeExample = false;
        }

        // Check if the line ends with a number - in which case a specified slangitem is to be extracted
        int number = -1;
        try {
            number = Integer.parseInt(message.substring(message.length() - 1, message.length()));
            message = message.substring(0, message.length()-1);
            number = Integer.parseInt(message.substring(message.length() - 2, message.length()));
            message = message.substring(0, message.length()-1);
        } catch(NumberFormatException ex) {
            // do nothing - if the number hasn't been set, we know it didn't work
        }

        message = message.trim();

        if(number <= 0)
            number = 1;

        SlangItem si;
        try {
            si = parseXML(getSlangXML(message), number);
        } catch(IOException e) {
            Grouphug.getInstance().sendMessage("Sorry, the intartubes seems to be clogged up (IOException)", false);
            System.err.println(e);
            return;
        } catch(Exception e) {
            // TODO small hack (better than the previous one): A general Exception is only thrown
            // TODO by us when no slang was found
            Grouphug.getInstance().sendMessage("No slang found for "+message+".", false);
            return;
        }

        String reply;
        if(includeExample)
            reply = si.getExample();
        else
            reply = si.getWord()+" ("+si.getNumber()+" of "+Slang.slangCount+"): "+ si.getDefinition();

        reply = Web.entitiesToChars(reply);

        Grouphug.getInstance().sendMessage(reply, true);
    }

    private String getSlangXML(String query) throws IOException {
        query = query.replace("&", "&amp;");
        URL u=new URL("http://api.urbandictionary.com/soap");
        URLConnection conn=u.openConnection();
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setAllowUserInteraction(false);
        conn.setRequestProperty("METHOD","POST");
        conn.setRequestProperty("Content-Type", "text/xml");
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(conn.getOutputStream()));
        dos.writeBytes("<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> <soapenv:Body> <ns1:lookup soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:ns1=\"urn:UrbanSearch\"> <key xsi:type=\"xsd:string\">3f9269732de32464b34b35fd26157cb5</key> <term xsi:type=\"xsd:string\">" + query + "</term> </ns1:lookup> </soapenv:Body> </soapenv:Envelope>" + "\r\n");
        dos.flush();
        dos.close();
        InputStream is=conn.getInputStream();
        byte[] buffer = new byte[1024];
        int bytesRead = 0;
        while(true) {
            int byteReadThisTurn = is.read(buffer, bytesRead, buffer.length - bytesRead);
            if(byteReadThisTurn < 0) break;
            bytesRead += byteReadThisTurn;
            if(bytesRead >= buffer.length - 256) {
                byte[] newBuffer = new byte[buffer.length * 2];
                System.arraycopy(buffer, 0, newBuffer, 0, bytesRead);
                buffer = newBuffer;
            }
        }
        is.close();
        if(buffer.length == bytesRead) {
            return new String(buffer);
        } else {
            byte[] response = new byte[bytesRead];
            System.arraycopy(buffer, 0, response, 0, bytesRead);
            return new String(buffer).replace("&amp;", "&");
        }
    }

    // TODO isn't this able to fail somewhere? throw exception?
    private SlangItem parseXML(String xml, int number) {

        String wordXMLStart = "<word xsi:type=\"xsd:string\">", wordXMLEnd = "</word>";
        String definitionXMLStart = "<definition xsi:type=\"xsd:string\">", definitionXMLEnd = "</definition>";
        String exampleXMLStart = "<example xsi:type=\"xsd:string\">", exampleXMLEnd = "</example>";

        // first, find how many slangs there are
        int slangCount = 0;
        int index = 0;
        while(xml.indexOf("</example>", index) != -1) {
            index = xml.indexOf("</example>", index) + 1;
            slangCount++;
        }
        Slang.slangCount = slangCount;

        if(number > slangCount)
            number = slangCount;

        // TODO hack, because number decrements, we copy it for use further down...
        int orgNumber = number;

        // Search through the definitions for the number we want, "</example>" defining the end scope of one definition
        index = 0;
        while(number > 1) {
            index = (xml.indexOf("</example>", index) + 10);
            number--;
        }

        String word = xml.substring(xml.indexOf(wordXMLStart, index) + wordXMLStart.length(), xml.indexOf(wordXMLEnd, index));
        String definition = xml.substring(xml.indexOf(definitionXMLStart, index) + definitionXMLStart.length(), xml.indexOf(definitionXMLEnd, index));
        String example = xml.substring(xml.indexOf(exampleXMLStart, index) + exampleXMLStart.length(), xml.indexOf(exampleXMLEnd, index));

        return new SlangItem(orgNumber, word, definition, example);
    }

    private static class SlangItem {

        private int number;
        private String word;
        private String definition;
        private String example;

        public int getNumber() {
            return number;
        }

        public String getWord() {
            return word;
        }

        public String getDefinition() {
            return definition;
        }

        public String getExample() {
            return example;
        }

        public SlangItem(int number, String word, String definition, String example) {
            this.number = number;
            this.word = word;
            this.definition = definition;
            this.example = example;
        }
    }
}
