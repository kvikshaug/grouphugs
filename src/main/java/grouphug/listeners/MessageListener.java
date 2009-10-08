package grouphug.listeners;

public interface MessageListener {

    /**
     * This method is called every time anyone says anything to the channel
     * @param channel channel of the event
     * @param sender users nick
     * @param login users login
     * @param hostname users hostname
     * @param message the users complete message
     */
    public abstract void onMessage(String channel, String sender, String login, String hostname, String message);
}
