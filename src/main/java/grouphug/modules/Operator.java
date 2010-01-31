package grouphug.modules;

import grouphug.listeners.JoinListener;
import grouphug.ModuleHandler;
import grouphug.Grouphug;

import java.util.regex.Pattern;
import java.util.ArrayList;

import org.jibble.pircbot.User;

public class Operator implements JoinListener {

    private ArrayList<UserMask> ops = new ArrayList<UserMask>();
    private Grouphug bot;

    public Operator(ModuleHandler handler) {
        handler.addJoinListener(this);
        bot = Grouphug.getInstance();

        // add the users to be opped
        ops.add(new UserMask("murr4y", null, null));
        ops.add(new UserMask("icc", null, null));
        ops.add(new UserMask("sunn", null, null));
        ops.add(new UserMask("Huuligan", null, null));
        ops.add(new UserMask("Ehtirno", null, null));
        ops.add(new UserMask("Twst", null, null));
        ops.add(new UserMask("Krashk", null, null));
        ops.add(new UserMask("Blaster", null, null));

        System.out.println("Operator module loaded.");
    }

    @Override
    public void onJoin(String channel, String sender, String login, String hostname) {
        // first check if it's me who's joining
        if(sender.equals(bot.getNick())) {
            return;
        }

        // all right, this looks a bit complicated but is really very easy:
        // for every user in the channel
        for(User user : bot.getUsers(Grouphug.CHANNEL)) {
            // if i (the bot) am that user...
            if(user.getNick().equals(bot.getNick())) {
                // ...then check if i (the bot) has op
                if(user.isOp()) {
                    // i have op, so check if the joining user is one to be oped
                    for(UserMask op : ops) {
                        // this is a user to be oped, so op him/her
                        if(op.is(sender)) {
                            bot.op(Grouphug.CHANNEL, sender);
                            break;
                        }
                    }
                    // reaching here, the user is NOT to be oped
                    return;
                } else {
                    // i don't have op, so complain
                    bot.sendMessage("Why haven't anyone oped me, so I could have oped " + sender + " just now?");
                    return;
                }
            }
        }
        // if we reach this spot, then i'm not in the user list!
        bot.sendMessage("Uhm, I'm not in the user list so I can't check if I have op status. " +
                "This might be a pircbot bug?");
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
