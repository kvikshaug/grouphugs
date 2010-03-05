package no.kvikshaug.gh.listeners;

public interface NickChangeListener {
    /**
     * Every time someone changes their nick, registered listeners will be notified via this method
     * @param oldNick The old nick
     * @param login The login of the user
     * @param hostname The hostname of the user
     * @param newNick The new nick
     */
    public void onNickChange(String oldNick, String login, String hostname, String newNick);
}
