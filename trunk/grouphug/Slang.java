package grouphug;

import java.net.URL;
import java.net.URLConnection;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;

public class Slang implements GrouphugModule {

    private static final String TRIGGER_MAIN = "slang ";
    private static final String TRIGGER_EXAMPLE = "-ex ";

    private static int slangCount = 0;

    public Slang() {
    }

    public void trigger(Grouphug bot, String channel, String sender, String login, String hostname, String message) {
        if(!message.startsWith(TRIGGER_MAIN))
            return;

        boolean includeExample;
        String text;
        if(message.startsWith(TRIGGER_MAIN + TRIGGER_EXAMPLE)) {
            includeExample = true;
            text = message.substring(TRIGGER_MAIN.length() + TRIGGER_EXAMPLE.length());
        } else {
            includeExample = false;
            text = message.substring(TRIGGER_MAIN.length());
        }

        // Check if the line ends with a number - in which case a specified slangitem is to be extracted
        int number = -1;
        try {
            number = Integer.parseInt(message.substring(message.length() - 1, message.length()));
            text = text.substring(0, text.length()-1);
            number = Integer.parseInt(message.substring(message.length() - 2, message.length()));
            text = text.substring(0, text.length()-1);
        } catch(NumberFormatException ex) {
            // do nothing - on intent; we check number later, if < 0 then bogus
        }

        text = text.trim();

        if(number <= 0)
            number = 1;

        SlangItem si = getSlang(text, number);

        String reply;
        if(includeExample)
            reply = si.getExample(); // TODO: if bug here, put \n in start of line (was removed because seemed unnecessary)
        else
            reply = si.getWord()+" ("+si.getNumber()+" of "+Slang.slangCount+"): "+ si.getDefinition();

        reply = reply.replace("&quot;", "\"");
        reply = reply.replace("&apos;", "'");

        bot.sendMessage(reply);
    }

    // TODO: should not return null, but throw an exception, upon failure 
    private SlangItem getSlang(String query, int number) {
        try {
            System.out.print("Connecting via soap to UD... ");
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
            System.out.println("OK");
            if(buffer.length == bytesRead) {
                return parseXML(new String(buffer), number);
            } else {
                byte[] response = new byte[bytesRead];
                System.arraycopy(buffer, 0, response, 0, bytesRead);
                return parseXML(new String(buffer), number);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
}
