package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.listeners.JoinListener;
import no.kvikshaug.gh.listeners.NickChangeListener;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.jibble.pircbot.User;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Operator implements JoinListener, NickChangeListener {

    private List<UserMask> ops = new ArrayList<UserMask>();
    private Grouphug bot;

    public Operator(ModuleHandler handler) {
        handler.addJoinListener(this);
        handler.addNickChangeListener(this);
        bot = Grouphug.getInstance();

        try {
	    	File xmlDocument = new File("props.xml");
	    	SAXBuilder saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
			Document jdomDocument = saxBuilder.build(xmlDocument);
			
			Element operatorNode = (Element)(XPath.selectSingleNode(jdomDocument,
			        "/Channels//Channel//Modules//Operator"));
						
			List<Element> operatorNicks = operatorNode.getChildren();
			
			for(Element nick: operatorNicks){
				ops.add(new UserMask((String)nick.getValue(), null, null));
			}
			
    	} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        
//        // add the users to be opped
//        ops.add(new UserMask("murr4y", null, null));
//        ops.add(new UserMask("icc", null, null));
//        ops.add(new UserMask("sunn", null, null));
//        ops.add(new UserMask("Huuligan", null, null));
//        ops.add(new UserMask("Ehtirno", null, null));
//        ops.add(new UserMask("Twst", null, null));
//        ops.add(new UserMask("Krashk", null, null));
//        ops.add(new UserMask("Blaster", null, null));
//        ops.add(new UserMask("dakh", null, null));

        System.out.println("Operator module loaded.");
    }

    @Override
    public void onJoin(String channel, String sender, String login, String hostname) {
        // first check if it's me who's joining
        if(sender.equals(bot.getNick())) {
            return;
        }

        opIfInList(sender, login, hostname);
    }

    @Override
    public void onNickChange(String oldNick, String login, String hostname, String newNick) {
        // first check if it's me who's changing nick (can happen)
        // hmm, could getNick() return the old nick if it for some reason isn't updated? just in case:
        if(oldNick.equals(bot.getNick())) {
            return;
        }
        if(newNick.equals(bot.getNick())) {
            return;
        }

        opIfInList(newNick, login, hostname);
    }

    private void opIfInList(String nick, String login, String hostname) {
        // all right, this looks a bit complicated but is really very easy:
        // first check if the joining user is one to be oped
        for(UserMask op : ops) {
            // if this is a user to be oped...
            if(op.is(nick)) {
                // then op him, but first check if I have op status to op the user
                // for every user in the channel
                for(User user : bot.getUsers(Grouphug.CHANNEL)) {
                    // if i (the bot) am that user...
                    if(user.getNick().equals(bot.getNick())) {
                        // ...then check if i (the bot) has op
                        if(user.isOp()) {
                            // i have, so op the user
                            bot.op(Grouphug.CHANNEL, nick);
                            return;
                        } else {
                            // i don't have op, but don't complain, just ignore it
                            return;
                        }
                    }
                }
                // if we reach this spot, then i'm not in the user list!
                bot.sendMessage("Uhm, I'm not in the user list so I can't check if I have op status in order to op others. " +
                        "This might be a pircbot bug?");
            }
        }
        // reaching here, the user is NOT to be oped
    }

    private static class UserMask {
        private String nick;
        private Pattern loginPattern;
        private Pattern hostnamePattern;

        private UserMask(String nick, Pattern loginPattern, Pattern hostnamePattern) {
            this.nick = nick;
            this.loginPattern = loginPattern;
            this.hostnamePattern = hostnamePattern;
        }

        public boolean is(String nick) {
            // TODO compare more than nick!
            return this.nick.equals(nick);
        }
    }
}
