package grouphug;

/**
 * This class runs as its own thread, and flushes the logfile regularly
 */
class LogFlusher implements Runnable {

    // we flush every second - remember that if the buffer is empty,
    // calling flush() does nothing, so this isn't really that much
    private static final int SLEEP_TIME = 1000;

    private Grouphug bot;

    LogFlusher(Grouphug bot) {
        this.bot = bot;
    }

    private boolean run = true; // should be set to false when we want to stop this thread.
    protected void stop() {
        run = false;
    }
    protected void start() {
        new Thread(this).start();
    }

    public void run() {
        while(run) {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                // interrupted, ok care, doesn't matter if we flush a little early once in a while
            }

            System.out.flush();
            System.err.flush();
        }
    }
}
