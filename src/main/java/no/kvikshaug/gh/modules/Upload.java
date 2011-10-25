package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.Config;
import no.kvikshaug.gh.exceptions.PreferenceNotSetException;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.SQL;
import no.kvikshaug.gh.util.Web;

import no.kvikshaug.worm.Worm;
import no.kvikshaug.worm.JWorm;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public class Upload implements TriggerListener {

    private static final String TRIGGER_HELP = "upload";
    private static final String TRIGGER = "upload";
    private static final String TRIGGER_KEYWORD = "keyword";

    private Grouphug bot;

    public Upload(ModuleHandler moduleHandler) {
        if(SQL.isAvailable()) {
            bot = Grouphug.getInstance();
            // moduleHandler.addMessageListener(this);
            moduleHandler.addTriggerListener(TRIGGER, this);
            moduleHandler.addTriggerListener(TRIGGER_KEYWORD, this);
            moduleHandler.registerHelp(TRIGGER_HELP, " A module to upload pictures and other things to gh\n" +
                    " To use the module type " +Grouphug.MAIN_TRIGGER + TRIGGER + " <url> <keyword>\n" +
                    " To search for a keyword or image name, use: "+ Grouphug.MAIN_TRIGGER + TRIGGER_KEYWORD+" <searchphrase>");
        } else {
            System.err.println("Upload module disabled: SQL is unavailable.");
        }
    }


    /*public void onMessage(String channel, String sender, String login, String hostname, String message) {
        TODO always upload pictures when posted
    }*/

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        // Verify that settings in Config are specified
        try {
            if("".equals(Config.uploadDirs().get(channel)) || "".equals(Config.publicUrls().get(channel))) {
                // Also throw an exception if the value for the specified channel is empty
                throw new PreferenceNotSetException("Missing Upload module elements in properties file");
            }
            if(trigger.equals(TRIGGER)) {
                insert(channel, message, sender);
            } else if(trigger.equals(TRIGGER_KEYWORD)) {
                showUploads(channel, message);
            }
        } catch(PreferenceNotSetException e) {
            bot.msg(channel, "My owner hasn't specified where to save upload " +
                    "images for this channel, and/or the URL where they can be accessed.");
            return;
        }
    }

    private void showUploads(String channel, String keyword) throws PreferenceNotSetException {
        if(keyword.length() <= 1) {
            bot.msg(channel, "Please use at least 2 search characters.");
            return;
        }

        List<UploadItem> items = JWorm.getWith(UploadItem.class, "where `channel`='" +
          SQL.sanitize(channel) + "' and (keyword like '%" + SQL.sanitize(keyword) +
          "%' or filename like '%" + SQL.sanitize(keyword) + "%')");

        if(items.size() == 0) {
            bot.msg(channel, "No results for '" + keyword + "'.");
        } else {
            StringBuilder allRows = new StringBuilder();
            for(UploadItem i : items) {
                allRows.append(Config.publicUrls().get(channel)).append(i.getFileName()).append(" ('")
                        .append(i.getKeyword()).append("' by ").append(i.getNick()).append(")\n");
            }
            //Prints the URL(s) associated with the keyword
            bot.msg(channel, allRows.toString(), true);
        }
    }

    private void insert(String channel, String message, String sender) throws PreferenceNotSetException {
        //Split the message into URL and keyword, URL first
        String[] parts = message.split(" ");
        int lastSlashIndex = parts[0].lastIndexOf('/');
        if(lastSlashIndex == -1) {
            bot.msg(channel, "That's not a valid URL now, is it?");
            return;
        }
        if(parts.length <= 1) {
            bot.msg(channel, "Please provide both a valid URL and keyword.");
            return;
        }
        int nextQuestionmarkIndex = parts[0].indexOf('?', lastSlashIndex);
        String filename;
        if(nextQuestionmarkIndex != -1) {
            filename = parts[0].substring(lastSlashIndex+1, nextQuestionmarkIndex);
        } else {
            filename = parts[0].substring(lastSlashIndex+1);
        }

        // if the file exists, add a number to the front of it
        if(new File(Config.uploadDirs().get(channel) + filename).exists()) {
            int number = 1;
            while(new File(Config.uploadDirs().get(channel) + number + filename).exists()) {
                number++;
            }
            filename = number + filename;
        }

        try {
            Web.downloadFile(parts[0], filename, Config.uploadDirs().get(channel));
            UploadItem i = new UploadItem(parts[1], sender, filename, new Date().getTime(), channel);
            i.insert();
            bot.msg(channel, "Saved to " + Config.publicUrls().get(channel) + filename);
        } catch (IOException e) {
            System.err.println("Failed to copy the file to the local filesystem.");
            bot.msg(channel, "Why am I expected to be able to upload anything?");
            e.printStackTrace();
        }
    }

    public static class UploadItem extends Worm {
        private String keyword;
        private String nick;
        private String fileName;
        private long date;
        private String channel;

        public UploadItem(String keyword, String nick, String fileName, long date, String channel) {
            this.keyword = keyword;
            this.nick = nick;
            this.fileName = fileName;
            this.date = date;
            this.channel = channel;
        }

        public String getKeyword() {
            return this.keyword;
        }

        public String getNick() {
            return this.nick;
        }

        public String getFileName() {
            return this.fileName;
        }

        public long getDate() {
            return this.date;
        }

        public String getChannel() {
            return this.channel;
        }
    }
}
