package no.kvikshaug.gh.modules

import no.kvikshaug.gh.listeners.TriggerListener
import no.kvikshaug.gh.{Grouphug, ModuleHandler}
import no.kvikshaug.gh.util.SQL;

import no.kvikshaug.worm.Worm;

import java.util.regex._

case class VoteItem(creator: String, text: String, multi: Boolean,
                    options: List[VoteOption]) extends Worm {
  def resultCount = options.foldLeft(0)((c, o) => c + o.voters.size)
  override def equals(other: Any) = other match {
    case that: VoteItem => wormDbId.get == that.wormDbId.get
    case thatId: String => wormDbId.get.toString == thatId
    case _ => false
  }
}
case class VoteOption(var text: String, var voters: List[String]) extends Worm {
  override def equals(other: Any) = other match {
    case that: VoteOption => text.toLowerCase == that.text.toLowerCase
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

  // load all votes from db
  var items = List[VoteItem]()
  if(SQL.isAvailable) {
    items = Worm.get[VoteItem]
  } else {
    println("Warning: Vote module will start, but existing votes will not be loaded and new votes " +
      "will not be stored because SQL is unavailable.")
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
    if(message.contains(" -m ")) {
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
      val ni = VoteItem(nick, question, multi, options)
      items = ni :: items
      ni.insert
      val multiText = if(multi) " with multiple choices allowed" else ""
      bot.msg(channel, "Created vote " + items(0).wormDbId.get + multiText + ".")
      bot.msg(channel, "Type '!vote <choice> " + items(0).wormDbId.get + "' to vote")
    } catch {
      case e => bot.msg(channel, "Sorry, what? Maybe you should check !help vote."); e.printStackTrace
    }
  }

  def endVote(channel: String, id: String): Unit = {
    val item = items.find(_ == id)
    if(item.isEmpty) {
      bot.msg(channel, "I don't have a vote with ID \"" + id + "\", try !votes")
      return
    } else {
      items = items.filterNot(_ == item.get)
      item.get.delete
      bot.msg(channel, "Removed vote \"" + id + "\".")
    }
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
        bot.msg(channel, "Vote " + item.wormDbId.get + " by " + item.creator + " asking \"" +
          item.text + "\" with " + item.resultCount + " votes")
      }
    }
  }

  def vote(channel: String, nick: String, message: String): Unit = {
    val m = Pattern.compile("(.+) ([0-9]+)").matcher(message)
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
      bot.msg(channel, "That vote doesn't have an option called \"" + m.group(1) + "\", try !showvote " + item.get.wormDbId.get)
      return
    }
    if(option.get.voters.contains(nick)) {
      bot.msg(channel, "You've already voted for that option.")
      return
    }
    option.get.voters = nick :: option.get.voters
    item.get.update
    bot.msg(channel, nick + " voted for '" + option.get.text + "' in vote " + 
      item.get.wormDbId.get + ": " + item.get.text)
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

