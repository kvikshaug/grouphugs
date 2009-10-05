package grouphug.modules;

import grouphug.Grouphug;
import grouphug.GrouphugModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EightBall implements GrouphugModule {

    private static final String TRIGGER = "8ball";
    private static final String TRIGGER_HELP = "8ball";
    private Random random = new Random(System.currentTimeMillis());
    private List<String> answerDb = new ArrayList<String>();

    public EightBall() {
        answerDb.add("As I see it, yes.");
        answerDb.add("It is certain.");
        answerDb.add("It is decidedly so.");
        answerDb.add("Most likely.");
        answerDb.add("Outlook good.");
        answerDb.add("Signs point to yes.");
        answerDb.add("Without a doubt.");
        answerDb.add("Yes.");
        answerDb.add("Yes - definitely.");
        answerDb.add("You may rely on it.");
        answerDb.add("Reply hazy, try again.");
        answerDb.add("Ask again later.");
        answerDb.add("Better not tell you now.");
        answerDb.add("Cannot predict  now.");
        answerDb.add("Concentrate and ask again.");
        answerDb.add("Don't count on it.");
        answerDb.add("My reply is no.");
        answerDb.add("My sources say no.");
        answerDb.add("Outlook not so good.");
        answerDb.add("Very doubtful.");
    }

    public void trigger(String channel, String sender, String login, String hostname, String message) {
        if(message.startsWith(TRIGGER))
            Grouphug.getInstance().sendMessage(sender+": "+answerDb.get(random.nextInt(answerDb.size())), false);
    }

    public void specialTrigger(String channel, String sender, String login, String hostname, String message) {
        // no special action
    }

    public String helpMainTrigger(String channel, String sender, String login, String hostname, String message) {
        return TRIGGER_HELP;
    }

    public String helpSpecialTrigger(String channel, String sender, String login, String hostname, String message) {
        if(message.equals(TRIGGER_HELP)) {
            return "Seek advice from the magic 8-ball fortuneteller!\n" +
                    "  "+ Grouphug.MAIN_TRIGGER+TRIGGER+" <yes/no question>\n" +
                    "  "+Grouphug.MAIN_TRIGGER+TRIGGER+" am I a bad person?";
        }
        return null;
    }
}
