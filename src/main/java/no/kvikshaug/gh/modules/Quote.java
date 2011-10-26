package no.kvikshaug.gh.modules;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.exceptions.SQLUnavailableException;
import no.kvikshaug.gh.listeners.MessageListener;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.SQL;

import no.kvikshaug.worm.Worm;
import no.kvikshaug.worm.JWorm;

public class Quote implements TriggerListener, MessageListener {

	private Grouphug bot;
	private HashMap<String, String> quotes; //Hashmap with sender->currentquote

	public Quote(ModuleHandler handler) {
		bot = Grouphug.getInstance();
		if(SQL.isAvailable()) {
			handler.addTriggerListener("startquote", this);
			handler.addTriggerListener("stopquote", this);
			handler.addTriggerListener("randomquote", this);
			handler.addMessageListener(this);
			handler.registerHelp("quote", "Saves lines you say between !startquote and !stopquote as quotes.\n" +
			"Restarts recording if sending a startquote before sending a stopquote");
			quotes = new HashMap<String, String>();
		} else {
			System.err.println("Quote module startup error: SQL is unavailable!");
		}
	}

	public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
		if (trigger.equals("startquote")){
			bot.msg(channel, "Starting recording of quote");
			quotes.put(sender, "");
		} else if (trigger.equals("stopquote")){
			bot.msg(channel, "Stopped recording of quote");
			String quote = quotes.get(sender);
			quote = quote.substring(0, quote.length()-1); //remove last \n
			saveQuote(channel, sender, quote);
			quotes.remove(sender);
		} else if (trigger.equals("randomquote")){
			sayRandomQuote(channel);
		}
	}

	public void onMessage(String channel, String sender, String login,
			String hostname, String message) {
		if (message.equals("!startquote") || message.equals("!stopquote")){
			return;
		}
		if (quotes.containsKey(sender)){ //Currently recording a quote
			quotes.put(sender, quotes.get(sender)+ message + "\n");
		}
	}

	private void sayRandomQuote(String channel){
		List<QuoteItem> quotes = JWorm.getWith(QuoteItem.class, "where channel='" +
		  channel + "' order by random() limit 1");
		if(quotes.size() == 0) {
			bot.msg(channel, "SQL managed to randomly pick a quote that doesn't exist! Maybe " +
			  "there are no quotes stored?");
		} else {
			bot.msg(channel, quotes.get(0).getQuote());
		}
	}

	private void saveQuote(String channel, String sender, String message){
		QuoteItem newItem = new QuoteItem(sender, message, new Date().getTime(), channel);
		newItem.insert();
	}

	public static class QuoteItem extends Worm {
		private String sender;
		private String quote;
		private Long date;
		private String channel;

		public QuoteItem(String sender, String quote, Long date, String channel) {
			this.sender = sender;
			this.quote = quote;
			this.date = date;
			this.channel = channel;
		}

		public String getSender() {
			return this.sender;
		}

		public String getQuote() {
			return this.quote;
		}

		public Long getDate() {
			return this.date;
		}

		public String getChannel() {
			return this.channel;
		}
	}
}
