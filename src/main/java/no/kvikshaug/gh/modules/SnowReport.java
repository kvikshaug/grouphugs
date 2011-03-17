package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.util.Web;
import no.kvikshaug.gh.listeners.TriggerListener;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.Element;
import org.jdom.xpath.XPath;

/**
 * Uses www.skiinfo.no to give a snow report per requested location.
 * A bit nasty, and pretty long, but works, and DOES use tagsoup and xpath :)
 */
public class SnowReport implements TriggerListener {

    private Grouphug bot;

    public SnowReport(ModuleHandler handler) {
        handler.addTriggerListener("snowreport", this);
        handler.addTriggerListener("snow", this);
        String helpText = "SnowReport - display snow depth, num. of open lifts and more for given location\n" +
                "!snow <location>";
        handler.registerHelp("snowreport", helpText);
        handler.registerHelp("snow", helpText);
        bot = Grouphug.getInstance();
        System.out.println("SnowReport module loaded.");
    }

    @Override
    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        StringBuilder finalReply = new StringBuilder();

        // try to fetch title and snow report
        try {
            List<URL> reportSites = Web.googleSearch(message + " site:skiinfo.no intitle:\"Snørapport fra\"");
            boolean found = false;
            for(URL snowReportSite : reportSites) {
                if(snowReportSite.toString().contains("/Snorapport/")) {
                    System.out.println(snowReportSite.toString());
                    Document snowReportDocument = Web.getJDOMDocument(snowReportSite);
                    finalReply.append(parseSnowReport(snowReportDocument));
                    finalReply.append("\n").append(snowReportSite.toString());
                    found = true;
                    break;
                }
            }
            if(!found) {
                bot.msg(channel, "Sorry, I couldn't find any location named '"+ message +"' on skiinfo.");
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            bot.sendAction(channel, "barfs up an IOException.");
        } catch (JDOMException e) {
            e.printStackTrace();
            bot.msg(channel, "Sorry, JDOM threw an exception. Probably not my fault.");
        } catch (UnableToParseException e) {
            e.printStackTrace();
            bot.msg(channel, "Sorry, I couldn't parse vital information for the report.");
        }

        bot.msg(channel, finalReply.toString());
    }

    private String parseSnowReport(Document document)
            throws JDOMException, UnableToParseException, IOException {
        // parse all the crap
        String title = parseTitle(document);
        String snowDepth = parseSnowDepth(document);
        String snowType = parseSnowType(document);
        String lastSnowFall = parseLastSnowFall(document);
        String lifts = parseLifts(document);
        String pists = parsePists(document);
        String conditions = parseConditions(document);
        String price = parsePrice(document);
        String lastUpdate = parseLastUpdate(document);

        // cleaning
        if("-".equals(snowType)) {
            snowType = "";
        } else {
            snowType = " (" + snowType + ")";
        }
        if("-".equals(price)) {
            price = "";
        } else {
            price = " - " + price;
        }
        if("?".equals(conditions)) {
            conditions = "";
        } else {
            conditions = " med \""+conditions+"\" forhold";
        }
        if("-".equals(lastSnowFall)) {
            lastSnowFall = "";
        } else {
            lastSnowFall = " - siste snøfall " + lastSnowFall;
        }

        // and put it all together
        return title + ": " + snowDepth + snowType + lastSnowFall + "\n" +
                lifts + " heiser, " + pists + " løyper"+conditions+price+"\n" +
                "Sist oppdatert: " + lastUpdate + ".";
    }

    private String parsePrice(Document document) throws JDOMException, IOException {
        // first we need to find a link to the lift price page on the current page (so we know it's the same location)
        XPath xpath = XPath.newInstance("//h:ul[@id='destMenu']/h:li/h:a");
        xpath.addNamespace("h", "http://www.w3.org/1999/xhtml");
        for(Object element : xpath.selectNodes(document)) {
            if(element instanceof Element) {
                if(((Element)element).getText().equals("Heiskortpriser")) {
                    Element priceElement = (Element) element;
                    return parsePriceSite(Web.getJDOMDocument(new URL("http://www.skiinfo.no/" + priceElement.getAttribute("href").getValue())));
                }
            }
        }
        return "-";
    }

    private String parsePriceSite(Document priceDocument) throws JDOMException {
        XPath xpath = XPath.newInstance("//h:div[@id='siId1']/h:div[@class='portlet table']/h:table/h:tr[4]/h:td[4]");
        xpath.addNamespace("h", "http://www.w3.org/1999/xhtml");
        Element element = (Element)xpath.selectSingleNode(priceDocument);

        if(element == null) {
            return "-";
        } else {
            String priceText = element.getText().replace(",00", "").trim();
            if("-".equals(priceText)) {
                return priceText;
            } else {
                return "Dagskort: " + priceText + "kr";
            }
        }
    }

    private String parsePists(Document document) throws JDOMException {
        // we'll need to fetch all tr's, and find the one with "Åpne nedfarter/totalt"
        XPath xpath = XPath.newInstance("//h:div[@class='portlet table alpineSummary']/h:table/h:tr");
        xpath.addNamespace("h", "http://www.w3.org/1999/xhtml");
        for(Object object : xpath.selectNodes(document)) {
            if(!(object instanceof Element)) {
                continue;
            }
            Element element = (Element)object;
            boolean aboutToFindRelevantRow = false;
            for(Object childObject : element.getChildren()) {
                if(!(childObject instanceof Element)) {
                    continue;
                }
                Element childElement = (Element)childObject;
                if(aboutToFindRelevantRow) {
                    return childElement.getText().trim();
                }
                if(childElement.getText().trim().equals("Åpne nedfarter/totalt:")) {
                    aboutToFindRelevantRow = true;
                }
            }
        }
        // didn't find the appropriate row for some reason
        return "?/?";
    }

    private String parseLifts(Document document) throws JDOMException {
        // we'll need to fetch all tr's, and find the one with "Åpne heiser"
        XPath xpath = XPath.newInstance("//h:div[@class='portlet table alpineSummary']/h:table/h:tr");
        xpath.addNamespace("h", "http://www.w3.org/1999/xhtml");
        for(Object object : xpath.selectNodes(document)) {
            if(!(object instanceof Element)) {
                continue;
            }
            Element element = (Element)object;
            boolean aboutToFindRelevantRow = false;
            for(Object childObject : element.getChildren()) {
                if(!(childObject instanceof Element)) {
                    continue;
                }
                Element childElement = (Element)childObject;
                if(aboutToFindRelevantRow) {
                    return childElement.getText().trim();
                }
                if(childElement.getText().trim().equals("Åpne heiser:")) {
                    aboutToFindRelevantRow = true;
                }
            }
        }
        // didn't find the appropriate row for some reason
        return "?/?";
    }

    private String parseConditions(Document document) throws JDOMException {
        XPath xpath = XPath.newInstance("//h:div[@class='portlet table alpineSummary']/h:table/h:tr[3]/h:td[2]/h:a");
        xpath.addNamespace("h", "http://www.w3.org/1999/xhtml");
        Element element = (Element)xpath.selectSingleNode(document);
        if(element == null) {
            return "?";
        } else {
            return element.getText().trim();
        }
    }

    private String parseLastUpdate(Document document) throws JDOMException {
        XPath xpath = XPath.newInstance("//h:span[@class='bold updated colorBlack noTextTransform']");
        xpath.addNamespace("h", "http://www.w3.org/1999/xhtml");
        Element element = (Element)xpath.selectSingleNode(document);
        if(element == null) {
            return "?";
        } else {
            return element.getText().replace("Sist oppdatert:", "").trim();
        }
    }

    private String parseTitle(Document document) throws JDOMException, UnableToParseException {
        // assume that the title is the first h1 element
        XPath xpath = XPath.newInstance("//h:h1[1]");
        xpath.addNamespace("h", "http://www.w3.org/1999/xhtml");
        Element header = (Element)xpath.selectSingleNode(document);
        if(header == null) {
            throw new UnableToParseException("Could not find first header element.");
        } else {
            return header.getText().replace(" - Snørapport", "").trim();
        }
    }


    private String parseLastSnowFall(Document document) throws JDOMException {
        XPath xpath = XPath.newInstance("//h:div[@id='siId1']/h:div[@class='portlet table']/h:table/h:tr[2]/h:td[2]");
        xpath.addNamespace("h", "http://www.w3.org/1999/xhtml");
        Element element = (Element)xpath.selectSingleNode(document);
        if(element == null) {
            return "-";
        } else {
            return element.getText().trim();
        }
    }

    private String parseSnowDepth(Document document) throws JDOMException, UnableToParseException {

        // first snow depth - we take all 4 values and calculate highest and lowest
        Integer snowdepth1 = null, snowdepth2 = null, snowdepth3 = null, snowdepth4 = null;

        // now parse the values from the document
        try {
            snowdepth1 = getSnowDepth(document, 2, 2);
        } catch(NullPointerException e) {
            System.out.println("Note: SnowReport unable to find first snowdepth element.");
        } catch(NumberFormatException e) {
            System.out.println("Note: SnowReport unable to parse first snowdepth element.");
        }

        try {
            snowdepth2 = getSnowDepth(document, 2, 3);
        } catch(NullPointerException e) {
            System.out.println("Note: SnowReport unable to find second snowdepth element.");
        } catch(NumberFormatException e) {
            System.out.println("Note: SnowReport unable to parse second snowdepth element.");
        }

        try {
            snowdepth3 = getSnowDepth(document, 3, 2);
        } catch(NullPointerException e) {
            System.out.println("Note: SnowReport unable to find third snowdepth element.");
        } catch(NumberFormatException e) {
            System.out.println("Note: SnowReport unable to parse third snowdepth element.");
        }

        try {
            snowdepth4 = getSnowDepth(document, 3, 3);
        } catch(NullPointerException e) {
            System.out.println("Note: SnowReport unable to find fourth snowdepth element.");
        } catch(NumberFormatException e) {
            System.out.println("Note: SnowReport unable to parse fourth snowdepth element.");
        }

        if(snowdepth1 == null && snowdepth2 == null && snowdepth3 == null && snowdepth4 == null) {
            return "-";
        }

        // now find the highest value
        int highest = -1;
        if(snowdepth1 != null) {
            highest = snowdepth1;
        } else if(snowdepth2 != null) {
            highest = snowdepth2;
        } else if(snowdepth3 != null) {
            highest = snowdepth3;
        } else if(snowdepth4 != null) {
            // if here, 4 is the only one, so we can just return that
            return snowdepth4 + "cm";
        }

        // just in case; should never happen
        if(highest == -1) {
            throw new UnableToParseException("'highest' is uninitialized, but shouldn't be able to be so! " +
                    "Please review code logic.");
        }

        if(snowdepth2 != null && snowdepth2 > highest) {
            highest = snowdepth2;
        }
        if(snowdepth3 != null && snowdepth3 > highest) {
            highest = snowdepth3;
        }
        if(snowdepth4 != null && snowdepth4 > highest) {
            highest = snowdepth4;
        }

        // now find the lowest value
        int lowest = -1;
        if(snowdepth1 != null) {
            lowest = snowdepth1;
        } else if(snowdepth2 != null) {
            lowest = snowdepth2;
        } else if(snowdepth3 != null) {
            lowest = snowdepth3;
        } else if(snowdepth4 != null) {
            // if we reach here, then something is very wrong
            throw new UnableToParseException("Equal code comparison yielded different results. " +
                    "snowdepth 1-3 is null and 4 is not, please review code logic.");
        }

        // just in case; should never happen
        if(lowest == -1) {
            throw new UnableToParseException("'highest' is uninitialized, but shouldn't be able to be so! " +
                    "Please review code logic.");
        }

        if(snowdepth2 != null && snowdepth2 < lowest) {
            lowest = snowdepth2;
        }
        if(snowdepth3 != null && snowdepth3 < lowest) {
            lowest = snowdepth3;
        }
        if(snowdepth4 != null && snowdepth4 < lowest) {
            lowest = snowdepth4;
        }

        // now, if they're the same size, we don't need to show both
        if(lowest == highest) {
            return highest + "cm";
        } else {
            return lowest + "-" + highest + "cm";
        }
    }

    private Integer getSnowDepth(Document document, int column, int row) throws JDOMException {
        String snowDepthString;
        XPath xpath = XPath.newInstance("//h:div[@class='portlet table snowreportBasic snowreport']/h:table/h:tr[" + column + "]/h:td[" +row + "]");
        xpath.addNamespace("h", "http://www.w3.org/1999/xhtml");
        snowDepthString = ((Element)xpath.selectSingleNode(document)).getText();
        // important: the following space character is NOT a standard space character.
        // it seems to be converted from &nbsp; by xpath, and it is not removed by string's trim() method.
        // therefore the char was copy-pasted and manually removed here
        snowDepthString = snowDepthString.replace(" ", "");
        snowDepthString = snowDepthString.replace("cm", "").trim();
        if("-".equals(snowDepthString)) {
            return null;
        } else {
            return Integer.parseInt(snowDepthString);
        }
    }

    private String parseSnowType(Document document) throws JDOMException {
        // note: checks only top value
        XPath xpath = XPath.newInstance("//h:div[@class='portlet table snowreportBasic snowreport']/h:table/h:tr[2]/h:td[4]");
        xpath.addNamespace("h", "http://www.w3.org/1999/xhtml");
        return ((Element)xpath.selectSingleNode(document)).getText().trim();
    }

    private class UnableToParseException extends Throwable {
        public UnableToParseException(String message) {
            super(message);
        }
    }
}
