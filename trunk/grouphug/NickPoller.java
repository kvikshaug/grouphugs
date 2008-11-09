package grouphug;

/**
 * This class runs as its own thread, its purpose is to reclaim our main nick if
 * it's taken when we first connect.
 */
class NickPoller implements Runnable {

    // How long we wait between each time we try to recapture our nick
    private static int RETRY_TIME = 2 * 60 * 1000; // in ms - every 2nd minute, should be pretty reasonable?

    // How long we wait for the confirmation of a nickchange before we check if the change was successful 
    private static int CONFIRMATION_WAIT_TIME = 2000; // in ms

    private static Grouphug bot;
    public static boolean run; // should be set to false when we want to stop this thread.

    /**
     * Creates and starts a thread which attempts
     * @param bot The bot
     */
    protected static void load(Grouphug bot) {
        // Create and start a thread
        NickPoller.bot = bot;
        run = true;
        new Thread(new NickPoller()).start();
    }

    public void run() {

        // At startup, we should check if we already have the most wanted nick,
        // and don't need to start this algorithm at all
        if(bot.getNick().equals(Grouphug.nicks.get(0))) {
            System.out.println("Nickpoller: Already have highest prioritized nick, stopping thread.");
            run = false;
        } else {
            System.out.println("Nickpoller: Will try to reclaim highest prioritized nick every "+(RETRY_TIME / 60 / 1000)+" minutes.");
        }

        while(run) {

            // First we wait a pretty long time between each try - we don't want the server
            // to think we're spamming it with nickchanges
            try {
                Thread.sleep(RETRY_TIME);
            } catch(InterruptedException e) {
                // do nothing, just try again at once
            }

            System.out.println("Trying to change nick from "+bot.getNick()+":");

            // Figure out which nick in the nicklist we have
            int currentNick;
            if((currentNick = Grouphug.nicks.indexOf(bot.getNick())) == -1)
                currentNick = Grouphug.nicks.size();

            // Try to change the nick to one of the more wanted ones in the nicklist
            for(int newNick = 0; newNick < currentNick && newNick < Grouphug.nicks.size(); newNick++) {

                System.out.print("  -> "+Grouphug.nicks.get(newNick)+" ... ");

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
                    System.out.println("got it!");
                    if(newNick == 0) {
                        // Nice, we got the nick we wanted the most! stop the thread and exit :)
                        run = false;
                        break;
                    } else {
                        // Ok cool, we got a nick we wanted more than this one, but not the one we
                        // wanted the most, so just stop for now, but retry in RETRY_TIME ms.
                        break;
                    }
                }
                System.out.println("failed, nick is probably taken.");
                // If not, try again with the next nick in the list
            }
            // If we reach this point (and run is true), we weren't able to change to the
            // most wanted nick, so go back and try again in RETRY_TIME milliseconds.
        }
    }
}
