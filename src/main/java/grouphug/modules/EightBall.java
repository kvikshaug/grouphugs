package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.listeners.TriggerListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EightBall implements TriggerListener {

    private static final String TRIGGER = "8ball";
    private static final String TRIGGER_HELP = "8ball";
    private Random random = new Random(System.currentTimeMillis());
    private List<String> answerDb = new ArrayList<String>();

    public EightBall(ModuleHandler moduleHandler) {
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

        moduleHandler.addTriggerListener(TRIGGER, this);
        moduleHandler.registerHelp(TRIGGER_HELP, "Seek advice from the magic 8-ball fortuneteller!\n" +
                    "  "+ Grouphug.MAIN_TRIGGER+TRIGGER+" <yes/no question>\n" +
                    "  "+Grouphug.MAIN_TRIGGER+TRIGGER+" am I a bad person?");
        System.out.println("8ball module loaded.");
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message) {
        Grouphug.getInstance().sendMessage(sender+": "+answerDb.get(random.nextInt(answerDb.size())));
    }
}
