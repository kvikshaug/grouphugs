CREATE TABLE bofh (excuse TEXT, id INTEGER PRIMARY KEY);
CREATE TABLE factoid (author TEXT, id INTEGER PRIMARY KEY, reply TEXT, trigger TEXT, type TEXT, channel TEXT);
CREATE TABLE joinmsg (id INTEGER PRIMARY KEY, channel TEXT, added_by TEXT, date TEXT, msg TEXT, nick TEXT);
CREATE TABLE karma (id INTEGER PRIMARY KEY, name TEXT, value NUMERIC, channel TEXT);
CREATE TABLE quote (id integer primary key, channel text, sender text, date text, quote text);
CREATE TABLE remember (id INTEGER PRIMARY KEY, message TEXT, sender TEXT, tag TEXT, channel TEXT);
CREATE TABLE seen (date TEXT, id INTEGER PRIMARY KEY, lastwords TEXT, nick TEXT, channel TEXT);
CREATE TABLE tell (id integer primary key, from_nick text, to_nick text, date text, msg text, channel text);
CREATE TABLE timer (id INTEGER PRIMARY KEY, nick TEXT, time INTEGER, message TEXT, channel TEXT);
CREATE TABLE tracking (id INTEGER PRIMARY KEY, trackingId TEXT, owner TEXT, channel TEXT, status text, statusCode text);
CREATE TABLE uploads (id INTEGER PRIMARY KEY, keyword TEXT, nick TEXT, filename TEXT, date TEXT, channel TEXT);
CREATE TABLE vote_option_voters (id INTEGER PRIMARY KEY, optionId INTEGER, nick TEXT);
CREATE TABLE vote_options (id INTEGER PRIMARY KEY, voteId INTEGER, text TEXT);
CREATE TABLE votes (id INTEGER PRIMARY KEY, creator TEXT, text TEXT, multi BOOLEAN);
CREATE TABLE words (id INTEGER PRIMARY KEY, lines NUMERIC, nick TEXT, since TEXT, words NUMERIC, channel TEXT);

