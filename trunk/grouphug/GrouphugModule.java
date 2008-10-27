package grouphug;

interface GrouphugModule {

    /**
     * This method is called by the bot when someone sends a chat line that starts with the trigger command.
     * It is then up to each module to parse the line and, if applicable, respond.
     *
     * @param channel - The channel to which the message was sent.
     * @param sender - The nick of the person who sent the message.
     * @param login - The login of the person who sent the message.
     * @param hostname - The hostname of the person who sent the message.
     * @param message - The actual message sent to the channel.
     */
    public abstract void trigger(String channel, String sender, String login, String hostname, String message);

    /**
     * This method is similar to the <code>trigger</code> method, but is called when the chat line contains
     * no specific trigger command. The module may still choose to parse this, e.g. the karma module fetching
     * up sentences ending with ++/--, but be careful as an important part of this bot is not to bother anyone
     * unless specifically requested.
     *
     * @param channel - The channel to which the message was sent.
     * @param sender - The nick of the person who sent the message.
     * @param login - The login of the person who sent the message.
     * @param hostname - The hostname of the person who sent the message.
     * @param message - The actual message sent to the channel.
     */
    public abstract void specialTrigger(String channel, String sender, String login, String hostname, String message);

    /**
     * This is called when the user is believed to ask for trigger specifics for all modules, e.g. with
     * the command "!help", and is supposed to output the name of the module, what it does, and how it's
     * triggered. Example output:
     *
     * SuperModule 1.1 - Does Super Magic Upon Request
     *  - Trigger: !super
     *
     * NOTE: This is to be sent in notice! 
     *
     * @param channel - The channel to which the message was sent.
     * @param sender - The nick of the person who sent the message.
     * @param login - The login of the person who sent the message.
     * @param hostname - The hostname of the person who sent the message.
     * @param message - The actual message sent to the channel.
     */
    public abstract void helpTrigger(String channel, String sender, String login, String hostname, String message);
}
