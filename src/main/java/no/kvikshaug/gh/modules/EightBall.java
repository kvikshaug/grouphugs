package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.listeners.TriggerListener;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class EightBall implements TriggerListener {

    private static final String TRIGGER = "8ball";
    private static final String TRIGGER_HELP = "8ball";
    private Random random = new Random(System.currentTimeMillis());
    private static final List<String> answers = Arrays.asList("As I see it, yes.",
                "It is certain.", "It is decidedly so.", "Most likely.",
                "Outlook good.", "Signs point to yes.", "Without a doubt.",
                "Yes.", "Yes - definitely.", "You may rely on it.",
                "Reply hazy, try again.", "Ask again later.",
                "Better not tell you now.", "Cannot predict  now.",
                "Concentrate and ask again.", "Don't count on it.",
                "My reply is no.", "My sources say no.",
                "Outlook not so good.", "Very doubtful.");

    public EightBall(ModuleHandler moduleHandler) {
        moduleHandler.addTriggerListener(TRIGGER, this);
        moduleHandler.registerHelp(TRIGGER_HELP, "Seek advice from the magic 8-ball fortuneteller!\n" +
                    "  "+ Grouphug.MAIN_TRIGGER+TRIGGER+" <yes/no question>\n" +
                    "  "+Grouphug.MAIN_TRIGGER+TRIGGER+" am I a bad person?");
        System.out.println("8ball module loaded.");
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        Grouphug.getInstance().sendMessageChannel(channel, sender+": "+ answers.get(random.nextInt(answers.size())));
    }
}
