package no.kvikshaug.gh.listeners;

public interface JoinListener {

    /**
     * This method will be called every time someone joins our channel, including us
     * @param channel The channel which somebody joined.
     * @param sender The nick of the user who joined the channel.
     * @param login The login of the user who joined the channel.
     * @param hostname The hostname of the user who joined the channel.
     */
    public void onJoin(String channel, String sender, String login, String hostname);

}
