package grouphug;

public interface GrouphugModule {

    // TODO why is "protected" not allowed here?
    public abstract void trigger(Grouphug bot, String channel, String sender, String login, String hostname, String message);

}
