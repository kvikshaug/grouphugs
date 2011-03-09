package no.kvikshaug.gh.modules;

import java.sql.SQLException;
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
import no.kvikshaug.gh.util.SQLHandler;

public class Quote implements TriggerListener, MessageListener {

	private Grouphug bot;
	private SQLHandler sqlHandler;
	private HashMap<String, String> quotes; //Hashmap with sender->currentquote

	private static final String QUOTE_DB = "quote";

	public Quote(ModuleHandler handler) {
		bot = Grouphug.getInstance();
		try {
			sqlHandler = SQLHandler.getSQLHandler();
		} catch (SQLUnavailableException ex) {
			System.err.println("Quote module startup error: SQL is unavailable!");
		}
		handler.addTriggerListener("startquote", this);
		handler.addTriggerListener("stopquote", this);
		handler.addTriggerListener("randomquote", this);
		handler.addMessageListener(this);
		handler.registerHelp("quote", "Saves lines you say between !startquote and !stopquote as quotes.\n" +
		"Restarts recording if sending a startquote before sending a stopquote");
		quotes = new HashMap<String, String>();
		System.out.println("Pre module loaded.");
	}

	public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
		if (trigger.equals("startquote")){
			bot.sendMessageChannel(channel, "Starting recording of quote");
			quotes.put(sender, "");
		} else if (trigger.equals("stopquote")){
			bot.sendMessageChannel(channel, "Stopped recording of quote: ");
			String quote = quotes.get(sender);
			quote = quote.substring(0, quote.length()-1); //remove last \n
			saveQuote(channel, sender, quote);
			bot.sendMessageChannel(channel, quote);
			quotes.remove(sender);
		} else if (trigger.equals("randomquote")){
			sayRandomQuote(channel);
		}
	}

	@Override
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
		try {
			Object[] row = sqlHandler.selectSingle("SELECT id, sender, date, quote FROM "+QUOTE_DB+" WHERE channel="+channel+" ORDER BY RANDOM() LIMIT 1");
			if (row != null) {
				bot.sendMessageChannel(channel, (String)row[3]);
			} 
		} catch (SQLUnavailableException e) {
			System.err.println("Quote failed: SQL is unavailable!");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void saveQuote(String channel, String sender, String message){
		List<String> params = new ArrayList<String>();
		params.add(channel);
		params.add(sender);
		params.add(SQL.dateToSQLDateTime(new Date()));
		params.add(message);
		try {
			sqlHandler.insert("INSERT INTO " + QUOTE_DB + " (channel, sender, date, quote) VALUES (?, ?, ?, ?);", params);
		} catch (SQLException e) {
			System.err.println(" > SQL Exception: " + e.getMessage() + "\n" + e.getCause());
			bot.sendMessageChannel(channel, "Sorry, unable to update Quote DB, an SQL error occured.");
		}
	}
}
