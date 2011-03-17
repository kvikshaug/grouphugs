package no.kvikshaug.gh.modules

import no.kvikshaug.gh.listeners.TriggerListener
import no.kvikshaug.gh.{Grouphug, ModuleHandler}
import no.kvikshaug.gh.exceptions.SQLUnavailableException;
import no.kvikshaug.gh.util.SQLHandler;

import java.util.regex._
import java.sql.SQLException

import scalaj.collection.Imports._
import scala.collection.immutable.List

case class VoteItem(id: String, creator: String, text: String, multi: Boolean, options: List[VoteOption]) {
  def resultCount = options.foldLeft(0)((c, o) => c + o.voters.size)
  override def equals(other: Any) = other match {
    case that: VoteItem => id == that.id
    case thatId: String => id == thatId
    case _ => false
  }
}
case class VoteOption(var text: String, var voters: List[String]) {
  override def equals(other: Any) = other match {
    case that: VoteOption => text == that.text
    case thatText: String => text.toLowerCase == thatText.toLowerCase
    case _ => false
  }
}

class Vote(val handler: ModuleHandler) extends TriggerListener {

  val bot = Grouphug.getInstance
  handler.addTriggerListener("startvote", this)
  handler.addTriggerListener("endvote", this)
  handler.addTriggerListener("showvote", this)
  handler.addTriggerListener("votes", this)
  handler.addTriggerListener("vote", this)
  handler.registerHelp("vote", """Have a channel vote.
!startvote Er du glad i meg?
!startvote -o ost, kaviar, alle tre -q Ost, kaviar eller salami?
!vote <choice> <vote>  -> Make your vote (choice is case insensitive)
!showvote <vote>       -> Shows options and results for a vote
!votes                 -> Lists all available votes
!endvote <vote>        -> Deletes a vote and corresponding results
Add -m to !startvote to allow multiple choices from one candidate""")

  var sqlHandler: SQLHandler = null
  val dbVotes = "votes"
  val dbVoteOptions = "vote_options"
  val dbVoteOptionVoters = "vote_option_voters"
  var items = List[VoteItem]()

  // load all votes from db. this is so ugly i can't even look at it.
  try {
    sqlHandler = SQLHandler.getSQLHandler
    // votes
    items = sqlHandler.select(
      "select id, creator, text, multi from " + dbVotes + ";"
    ).asScala.map { voteRow =>
      // vote options
      val optionList = sqlHandler.select(
        "select id, text from " + dbVoteOptions + " where voteId='" + voteRow(0).toString + "';"
      ).asScala.map { voteOptionRow =>
        // vote option voters
        val voterList = sqlHandler.select(
          "select nick from " + dbVoteOptionVoters + " where optionId='" + voteOptionRow(0).toString + "';"
        ).asScala.map { voteOptionVoterRow =>
          voteOptionVoterRow(0).asInstanceOf[String]
        }.toList;
        VoteOption(voteOptionRow(1).asInstanceOf[String], voterList)
      }.toList;
    VoteItem(voteRow(0).toString, voteRow(1).asInstanceOf[String],
             voteRow(2).asInstanceOf[String], voteRow(3).asInstanceOf[String].toBoolean,
             optionList)
    }.toList
  } catch {
    case e: SQLUnavailableException => println("Vote module warning: Existing votes couldn't be loaded because SQL is unavailable.")
    case e: SQLException => println("Vote module warning: Existing votes couldn't be loaded because of an SQLException.")
      e.printStackTrace();
  }
  println("Vote module loaded.")

  val random = new java.util.Random
  val idDigits = 4
  def randomId = {
    def generate = random.nextInt(9000) + 1000 // remember to change idDigits if changing these!
    var id = generate
    // might be inefficient if there are a lot of votes, but there probably won't be
    while(items.contains(id)) {
      id = generate
    }
    id
  }

  def onTrigger(channel: String, sender: String, login: String, hostname: String, message: String, trigger: String) = trigger match {
    case "startvote" => startVote(channel, sender, message)
    case "endvote"   => endVote(channel, message)
    case "showvote"  => showVote(channel, message)
    case "votes"     => listVotes(channel)
    case "vote"      => vote(channel, sender, message)
  }

  def startVote(channel: String, nick: String, message: String): Unit = {
    var multi = false
    var content = message
    if(message.contains("-m")) {
      multi = true
      content = message.replace("-m", "").trim
    }
    val customOptions = Pattern.compile(" *-o (.*) *-q (.*)").matcher(content)
    var question = ""
    var options = List[VoteOption]()
    try {
      if(customOptions matches) {
        options = customOptions.group(1).split(",").toList.map(VoteOption(_, List[String]()))
        question = customOptions.group(2).trim
      } else {
        options = VoteOption("ja", List[String]()) :: VoteOption("nei", List[String]()) :: Nil
        question = content
      }
      options = (options.map{x => x.text = x.text.trim; x}).filterNot(_.text isEmpty)
      if((options isEmpty) || (question isEmpty)) {
        bot.msg(channel, "Please provide both a question and options.")
        return
      }
      items = VoteItem(randomId.toString, nick, question, multi, options) :: items
      val multiText = if(multi) " with multiple choices allowed" else ""
      bot.msg(channel, "Created vote " + items(0).id + multiText + ".")
      bot.msg(channel, "Type '!vote <choice> " + items(0).id + "' to vote")
      sqlHandler.insert("insert into votes (id, creator, text, multi) values ('" + items(0).id + "', '" + nick + "', '" + question + "', '" + multi.toString + "')")
      options foreach { o =>
        sqlHandler.insert("insert into " + dbVoteOptions + " (voteId, text) values ('" + items(0).id + "', '" + o.text + "');")
      }
      // db
    } catch {
      case e: SQLException => bot.msg(channel, "Failed to insert the vote into SQL."); e.printStackTrace
      case e => bot.msg(channel, "Sorry, what? Maybe you should check !help vote."); e.printStackTrace
    }
  }

  def endVote(channel: String, id: String): Unit = {
    if(!(items.contains(id))) {
      bot.msg(channel, "I don't have a vote with ID \"" + id + "\", try !votes")
      return
    }
    items = items.filterNot(_ == id)
    sqlHandler.select("select id from " + dbVoteOptions + " where voteId='" + id + "';").asScala.foreach { row =>
      sqlHandler.delete("delete from " + dbVoteOptionVoters + " where optionId='" + row(0).toString + "';")
    }
    sqlHandler.delete("delete from " + dbVoteOptions + " where voteId='" + id + "';")
    sqlHandler.delete("delete from " + dbVotes + " where id='" + id + "';")
    bot.msg(channel, "Removed vote \"" + id + "\".")
  }

  def showVote(channel: String, id: String): Unit = {
    var item = items.find(_ == id)
    if(item isEmpty) {
      bot.msg(channel, "I don't have a vote with ID \"" + id + "\", try !votes")
      return
    }
    val multi = if(item.get.multi) ", multiple choices allowed" else ""
    bot.msg(channel, "Vote " + id + " by " + item.get.creator + multi + ": " + item.get.text)
    outputSortedList(channel, item.get)
  }

  def listVotes(channel: String) = {
    if(items.size == 0) {
      bot.msg(channel, "No votes are currently being tracked.")
    } else {
      items foreach { item =>
        bot.msg(channel, "Vote " + item.id + " by " + item.creator + " asking \"" + item.text + "\" with " + item.resultCount + " votes")
      }
    }
  }

  def vote(channel: String, nick: String, message: String): Unit = {
    val m = Pattern.compile("(.+) ([0-9]{" + idDigits + "})").matcher(message)
    if(!(m matches) || m.groupCount < 2) {
      bot.msg(channel, "Sorry, what? Maybe you should try !help vote")
      return
    }
    val item = items.find(_ == m.group(2))
    if(item isEmpty) {
      bot.msg(channel, "I don't have a vote with ID \"" + m.group(2) + "\", try !votes")
      return
    }
    if(!(item.get.multi) && hasVoted(nick, item.get)) {
      bot.msg(channel, "You've already voted in that vote.")
      return
    }
    val option = item.get.options.find(_ == m.group(1))
    if(option isEmpty) {
      bot.msg(channel, "That vote doesn't have an option called \"" + m.group(1) + "\", try !showvote " + item.get.id)
      return
    }
    if(option.get.voters.contains(nick)) {
      bot.msg(channel, "You've already voted for that option.")
      return
    }
    try {
      option.get.voters = nick :: option.get.voters
      val optionId = (sqlHandler.selectSingle("select id from " + dbVoteOptions + " where text='" + option.get.text + "' and voteId='" + m.group(2) + "';"))(0).toString
      sqlHandler.insert("insert into " + dbVoteOptionVoters + " (optionId, nick) values ('" + optionId + "', '" + nick + "');")
      bot.msg(channel, nick + " voted for '" + option.get.text + "' in vote " + item.get.id + ": " + item.get.text)
      outputSortedList(channel, item.get)
    } catch {
      case e: SQLException => bot.msg(channel, "Sorry, I failed to update votecount in SQL!"); e.printStackTrace
    }
  }

  def hasVoted(nick: String, vote: VoteItem): Boolean = vote.options.exists {
    o => o.voters.exists {
      v => v == nick
    }
  }

  def outputSortedList(channel: String, item: VoteItem) = {
    item.options.sortWith((a,b) => a.voters.size > b.voters.size).foreach { o =>
      var voters = ""
      o.voters.foreach { v =>
        voters = voters + ", " + v
      }
      if(voters != "") {
        voters = " (" + voters.substring(2) + ")"
      }
      val plural = if(voters.size != 1) "s" else ""
      bot.msg(channel, o.voters.size + " vote" + plural + " for " + o.text + voters)
    }
  }
}

