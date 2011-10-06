CREATE TABLE BofhExcuse (`id` INTEGER PRIMARY KEY, `excuse` TEXT);
CREATE TABLE FactoidItem (`id` INTEGER PRIMARY KEY, `message` TEXT, `trigger` TEXT, `reply` TEXT, `author` TEXT, `channel` TEXT);
CREATE TABLE JoinMessageItem (`id` INTEGER PRIMARY KEY, `nick` TEXT, `message` TEXT, `author` TEXT, `date` NUMERIC, `channel` TEXT);
CREATE TABLE KarmaItem (`id` INTEGER PRIMARY KEY, `name` TEXT, `karma` NUMERIC, `channel` TEXT);
CREATE TABLE QuoteItem (`id` INTEGER PRIMARY KEY, `sender` TEXT, `quote` TEXT, `date` NUMERIC, `channel` TEXT);
CREATE TABLE RememberItem (`id` INTEGER PRIMARY KEY, `message` TEXT, `sender` TEXT, `tag` TEXT, `channel` TEXT);
CREATE TABLE SeenItem (`id` INTEGER PRIMARY KEY, `nick` TEXT, `lastWords` TEXT, `date` NUMERIC, `channel` TEXT);
CREATE TABLE TellItem (`id` INTEGER PRIMARY KEY, `from` TEXT, `to` TEXT, `message` TEXT, `date` NUMERIC, `channel` TEXT);
CREATE TABLE Sleeper (`id` INTEGER PRIMARY KEY, `nick` TEXT, `time` NUMERIC, `message` TEXT, `channel` TEXT);
CREATE TABLE Package (`id` INTEGER PRIMARY KEY, `trackingId` TEXT, `owner` TEXT, `status` TEXT, `statusCode` TEXT, `channel` TEXT);
CREATE TABLE UploadItem (`id` INTEGER PRIMARY KEY, `keyword` TEXT, `nick` TEXT, `fileName` TEXT, `date` NUMERIC, `channel` TEXT);
CREATE TABLE Words (`id` INTEGER PRIMARY KEY, `words` NUMERIC, `lines` NUMERIC, `nick` TEXT, `since` NUMERIC, `channel` TEXT);
CREATE TABLE VoteItem (`id` INTEGER PRIMARY KEY, `creator` TEXT, `text` TEXT, `multi` TEXT);
CREATE TABLE VoteItemVoteOption (`id` INTEGER PRIMARY KEY, `order` NUMERIC, `voteItem` INTEGER, `options` INTEGER);
CREATE TABLE VoteOption (`id` INTEGER PRIMARY KEY, `text` TEXT);
CREATE TABLE VoteOptionString (`id` INTEGER PRIMARY KEY, `order` NUMERIC, `voteOption` INTEGER, `voters` TEXT);

