package grouphug;

/**
 * This class runs as its own thread, its purpose is to reclaim our main nick if
 * it's taken when we first connect.
 */
class NickPoller implements Runnable {

    // How long we wait between each time we try to recapture our nick
    private static int RETRY_TIME = 3 * 60 * 1000; // in ms - every 3 minutes, should be pretty reasonable?

    // How long we wait for the confirmation of a nickchange before we check if the change was successful
    private static int CONFIRMATION_WAIT_TIME = 2000; // in ms

    private static Grouphug bot;
    private static NickPoller pollerThread;

    private boolean run = true; // should be set to false when we want to stop this thread.
    public void stop() {
        run = false;
    }

    /**
     * Creates and starts a thread which attempts to recapture the most wanted nicks in the nicklist
     * If there already is a thread running; it is stopped
     * @param bot The bot
     */
    protected static void load(Grouphug bot) {
        NickPoller.bot = bot;

        if(pollerThread != null)
            pollerThread.run = false;

        pollerThread = new NickPoller();
        new Thread(pollerThread).start();
    }

    public void run() {

        // At startup, we should check if we already have the most wanted nick,
        // and don't need to start this algorithm at all
        // (Should this check be done before even starting the load() method ?)
        if(bot.getNick().equals(Grouphug.nicks.get(0))) {
            System.out.println("Nickpoller: Already have main nick, stopping thread.");
            run = false;
        }

        while(run) {

            // First we wait a pretty long time between each try - we don't want the server
            // to think we're spamming it with nickchanges
            try {
                Thread.sleep(RETRY_TIME);
            } catch(InterruptedException e) {
                // do nothing, just try again at once
            }

            // Figure out which nick in the nicklist we have
            int currentNick;
            if((currentNick = Grouphug.nicks.indexOf(bot.getNick())) == -1)
                currentNick = Grouphug.nicks.size();

            // Try to change the nick to one of the more wanted ones in the nicklist
            for(int newNick = 0; newNick < currentNick && newNick < Grouphug.nicks.size(); newNick++) {

                // First try a nickchange
                bot.changeNick(Grouphug.nicks.get(newNick));

                // Wait for confirmation from the server
                try {
                    Thread.sleep(CONFIRMATION_WAIT_TIME);
                } catch(InterruptedException e) {
                    // do nothing, just check immediately
                }

                // And check if the change was successful
                if(bot.getNick().equals(Grouphug.nicks.get(newNick))) {
                    if(newNick == 0) {
                        // Nice, we got the nick we wanted the most! stop the thread and exit :)
                        System.out.println("Nickpoller: Got main nick! Stopping thread.");
                        run = false;
                        break;
                    } else {
                        // Ok cool, we got a nick we wanted more than this one, but not the one we
                        // wanted the most, so just stop for now, but retry in RETRY_TIME ms.
                        break;
                    }
                }
                // If not, try again with the next nick in the list
            }
            // If we reach this point (and run is true), we weren't able to change to the
            // most wanted nick, so go back and try again in RETRY_TIME milliseconds.
        }
    }
}
