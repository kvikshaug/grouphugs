package no.kvikshaug.gh.modules

import no.kvikshaug.gh.listeners.TriggerListener
import no.kvikshaug.gh.{Grouphug, ModuleHandler}

import java.util.regex._

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
Add -m to !startvote to allow multiple answers from one candidate""")
  println("Vote module loaded.")

  // TODO: db everywhere
  var items = List[VoteItem]()
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
        bot.sendMessageChannel(channel, "Please provide both a question and options.")
        return
      }
      items = VoteItem(randomId.toString, nick, question, multi, options) :: items
      var multiText = ""
      if(multi) {
        multiText = " with multiple answers allowed"
      }
      bot.sendMessageChannel(channel, "Created vote " + items(0).id + multiText + ".")
      bot.sendMessageChannel(channel, "Type '!vote <choice> " + items(0).id + "' to vote")
      // db
    } catch {
      case e => bot.sendMessageChannel(channel, "Sorry, what? Maybe you should check !help vote."); e.printStackTrace
    }
  }

  def endVote(channel: String, id: String): Unit = {
    if(!(items.contains(id))) {
      bot.sendMessageChannel(channel, "I don't have a vote with ID \"" + id + "\", try !votes")
      return
    }
    items = items.filterNot(_ == id)
    bot.sendMessageChannel(channel, "Removed vote \"" + id + "\".")
  }

  def showVote(channel: String, id: String): Unit = {
    var item = items.find(_ == id)
    if(item isEmpty) {
      bot.sendMessageChannel(channel, "I don't have a vote with ID \"" + id + "\", try !votes")
      return
    }
    var multi = ""
    if(item.get.multi) {
      multi = ", multiple answers allowed"
    }
    bot.sendMessageChannel(channel, "Vote " + id + " by " + item.get.creator + multi + ": " + item.get.text)
    outputSortedList(channel, item.get)
  }

  def listVotes(channel: String) = {
    if(items.size == 0) {
      bot.sendMessageChannel(channel, "No votes are currently being tracked.")
    } else {
      items foreach { item =>
        bot.sendMessageChannel(channel, "Vote " + item.id + " by " + item.creator + " asking \"" + item.text + "\" with " + item.resultCount + " votes")
      }
    }
  }

  def vote(channel: String, nick: String, message: String): Unit = {
    val m = Pattern.compile("(.+) ([0-9]{" + idDigits + "})").matcher(message)
    if(!(m matches) || m.groupCount < 2) {
      bot.sendMessageChannel(channel, "Sorry, what? Maybe you should try !help vote")
      return
    }
    val item = items.find(_ == m.group(2))
    if(item isEmpty) {
      bot.sendMessageChannel(channel, "I don't have a vote with ID \"" + m.group(2) + "\", try !votes")
      return
    }
    if(!(item.get.multi) && hasVoted(nick, item.get)) {
      bot.sendMessageChannel(channel, "You've already voted in that vote.")
      return
    }
    val option = item.get.options.find(_ == m.group(1))
    if(option isEmpty) {
      bot.sendMessageChannel(channel, "That vote doesn't have an option called \"" + m.group(1) + "\", try !showvote " + item.get.id)
      return
    }
    if(option.get.voters.contains(nick)) {
      bot.sendMessageChannel(channel, "You've already voted for that option.")
      return
    }
    // TODO db!
    option.get.voters = nick :: option.get.voters
    bot.sendMessageChannel(channel, nick + " voted for '" + option.get.text + "' in vote " + item.get.id + ": " + item.get.text)
    outputSortedList(channel, item.get)
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
      bot.sendMessageChannel(channel, o.voters.size + " votes for " + o.text + voters)
    }
  }
}

