package grouphug.listeners;

/**
 */
public interface TriggerListener {

    /**
     * This method is called when the modules trigger string is triggered
     * when a user writes something to the channel
     * @param channel channel of the event
     * @param sender users nick
     * @param login users login
     * @param hostname users hostname
     * @param message the message sent from the user, excluding the trigger character, trigger string and
     * any leading or trailing whitespace. So a '!trigger hi all'-message will be sent to the module as
     * 'hi all'.
     * @param trigger the trigger string that led to this module being triggered, this is useful if the module
     * has registered more than one trigger
     */
    public abstract void onTrigger(String channel, String sender, String login, String hostname, String message,
                                   String trigger);

}
