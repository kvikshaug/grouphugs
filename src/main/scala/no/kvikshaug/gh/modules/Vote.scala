package no.kvikshaug.gh.modules

import no.kvikshaug.gh.listeners.TriggerListener
import no.kvikshaug.gh.{Grouphug, ModuleHandler}

case class VoteItem(id: String, creator: String, text: String, multi: Boolean, options: List[VoteOption]) {
  def resultCount = options.foldLeft(0)((c, o) => c + o.voters.size)
}
case class VoteOption(id: String, text: String, var voters: List[String])

class Vote(val handler: ModuleHandler) extends TriggerListener {

  val bot = Grouphug.getInstance
  handler.addTriggerListener("startvote", this)
  handler.addTriggerListener("endvote", this)
  handler.addTriggerListener("showvote", this)
  handler.addTriggerListener("votes", this)
  handler.addTriggerListener("vote", this)
  handler.registerHelp("vote", """Have a channel vote.
!startvote Er du glad i meg?
!startvote -o ["Ost", "Kaviar", "Alle tre"] -q Ost, kaviar eller salami?
!vote <choice> <vote>
!showvote <vote>  -> Shows options and results for a vote
!votes            -> Lists all available votes
!endvote <vote>   -> Deletes a vote and corresponding results
Add -m to !startvote to allow multiple answers from one candidate""")
  println("Vote module loaded.")

  // TODO: db everywhere
  var items = List[VoteItem]()
  val random = new java.util.Random

  def onTrigger(channel: String, sender: String, login: String, hostname: String, message: String, trigger: String) = trigger match {
    case "startvote" => startVote(channel, nick, message)
    case "endvote"   => endVote(channel, message)
    case "showvote"  => showVote(channel, message)
    case "votes"     => listVotes(channel)
    case "vote"      => vote(channel, sender, message.split(' '))
  }

  def startVote(channel: String, nick: String, message: String) = {
    multi = message.contains("-m")
    items = VoteItem(randomId, nick, message, multi, VoteOption(:: items
    /*
    val matchCustom = Pattern.compile("-o [(\".*\", ?)+]").matcher(message)
    def parseLine(str: String) = {
      val mAll = Pattern.compile("-a (.+)").matcher(str)
      val mFor = Pattern.compile("-f (.+)").matcher(str)
      val mSeq = Pattern.compile("([0-9])-([0-9]) (.+)").matcher(str)
      val mOne = Pattern.compile("([0-9]) (.+)").matcher(str)
      //val mCur = Pattern.compile(".+").matcher(str)

      if(mAll matches) {
        (List(0, 1, 2, 3), true, mAll.group(1))
      } else if(mFor matches) {
        (List(0, 1, 2, 3), false, mFor.group(1))
      } else if(mSeq matches) {
        ((mSeq.group(1).toInt to mSeq.group(2).toInt toList), false, mSeq.group(3))
      } else if(mOne matches) {
        (List(mOne.group(1).toInt), false, mOne.group(2))
      } else /* if(mCur matches) *//* {
        (List(), true, str)
      }
      */
    }
  }

  def endVote(channel: String, id: String): Unit = {
    if(!(items.exists(_.id == id))) {
      bot.sendMessageChannel(channel, "I don't have a vote with ID \"" + message(0) + "\", try !votes")
      return
    }
    items = items.filterNot(_.id == id)
    bot.sendMessageChannel(channel, "Removed vote \"" + id + "\".")
  }

  def showVote(channel: String, id: String) = {
    var item = items.find(_.id == message(1))
    if(item isEmpty) {
      bot.sendMessageChannel(channel, "I don't have a vote with ID \"" + message(0) + "\", try !votes")
      return
    }
    item = item.get
    var multi = " not"
    if(item.multi) {
      multi = ""
    }
    bot.sendMessageChannel(channel, "Vote " + id + " by " + item.creator + " asking \"" + item.text + "\", multiple answers" + multi + " allowed:")
    item.options.foreach { o =>
      var voters = ""
      o.voters.foreach { v =>
        voters = voters + ", " + v
      }
      if(voters != "") {
        voters = voters.substring(2)
      } else {
        voters = "None"
      }
      bot.sendMessageChannel(channel, o.id + ": " + o.text + " - voters: " + voters)
    }
  }

  def listVotes(channel: String) = {
    if(items.size == 0) {
      bot.sendMessageChannel(channel, "No votes are currently being tracked.")
    } else {
      items foreach { item =>
        bot.sendMessageChannel(channel, "Vote " + item.id + " asking \"" + item.text + "\" with " + item.resultCount + " votes")
      }
    }
  }

  def vote(channel: String, nick: String, message: Array[String]): Unit = {
    val item = items.find(_.id == message(1))
    if(item isEmpty) {
      bot.sendMessageChannel(channel, "I don't have a vote with ID \"" + message(0) + "\", try !votes")
      return
    }
    if(!(item.get.multi) && hasVoted(nick, message(1))) {
      bot.sendMessageChannel(channel, "You've already voted in this vote.")
      return
    }
    val option = item.get.options.find(_.id == message(0))
    if(option isEmpty) {
      bot.sendMessageChannel(channel, "That vote doesn't have an option called \"" + message(0) + "\", try !showvote " + item.get.id)
      return
    }
    option.get.voters = nick :: option.get.voters
    // TODO db!
  }

  def hasVoted(nick: String, vote: String): Boolean = items.exists {
    i => i.options.exists {
      o => o.voters.exists {
        v => v == nick
      }
    }
  }

}

