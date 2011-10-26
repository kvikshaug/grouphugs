package no.kvikshaug.gh.listeners;

public interface PartListener {

    /**
     * This method is called whenever someone (possibly us) parts a channel which we are on.
     * @param channel - The channel which somebody parted from.
     * @param sender - The nick of the user who parted from the channel.
     * @param login - The login of the user who parted from the channel.
     * @param hostname - The hostname of the user who parted from the channel.
     */
    public void onPart(String channel, String sender, String login, String hostname);

}
