package com.tsunderebug.speedrunbot.command

import com.tsunderebug.speedrunbot.FormatUtil
import com.tsunderebug.speedrunbot.data.Database
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.{EmbedBuilder, MessageBuilder}

import scala.collection.mutable

object PersonalBest extends Command("pb", "Lists PBs", (_: MessageReceivedEvent) => true, (e: MessageReceivedEvent) => {
  val u = if (e.getMessage.getContent.split("\\s+").length <= 2 || e.getMessage.getMentions.isEmpty) {
    e.getAuthor
  } else {
    e.getMessage.getMentions.get(0)
  }
  val mb = new MessageBuilder(e.getClient)
  mb.withContent(u.mention() + "'s PBs:")
  val eb = new EmbedBuilder
  eb.withColor(0x7289DA)
  if (!Database.db.pbs.contains(u.getStringID) || Database.db.pbs(u.getStringID).isEmpty) {
    eb.withDescription("This user has no PBs in the bot!")
  } else {
    eb.setLenient(false)
    eb.appendDescription("```\n")
    for (g <- Database.db.pbs(u.getStringID).keys) {
      eb.appendDesc(g + ": " + FormatUtil.msToTime(Database.db.pbs(u.getStringID)(g).toLong) + "\n")
    }
    eb.appendDescription("```")
  }
  mb.withEmbed(eb.build())
  mb.withChannel(e.getChannel)
  mb.send()
  true
}
) {

  object Add extends Command("add", "Registers your PB (`-s pb add [game here] [time in HH:mm:ss.SSS]`)", (_: MessageReceivedEvent) => true, (e: MessageReceivedEvent) => {
    try {
      val game = e.getMessage.getContent.split("\\s+").drop(3).dropRight(1).mkString(" ")
      val time = e.getMessage.getContent.split("\\s+").takeRight(1).mkString(" ")
      val ms = FormatUtil.timeToMS(time)
      Database.addPB(e.getAuthor, game, ms.toString)
      e.getChannel.sendMessage("Added your PB for **" + game + "** to the database: **" + time + "**")
      true
    } catch {
      case _: Exception => false
    }
  })

  object Game extends Command("game", "Sorts and searches PBs by name", (_: MessageReceivedEvent) => true, (e: MessageReceivedEvent) => {
    val game = e.getMessage.getContent.split("\\s+").drop(3).mkString(" ")
    val users = Database.db.pbs.keys.filter(Database.db.pbs(_).contains(game))
    val mb = new MessageBuilder(e.getClient)
    mb.withContent("Leaderboard for **" + game + "**: _(Please PM Sock#4177 or TsundereBug#0641 if you suspect fake PBs)_")
    val eb = new EmbedBuilder
    eb.withColor(0x43b581)
    eb.setLenient(false)
    eb.appendDesc("```\n")
    val lb: mutable.Map[Long, String] = mutable.Map()
    Database.db.pbs.keys.foreach((s: String) => {
      val m = Database.db.pbs(s)
      m.keys.filter(game.==).foreach((g: String) => {
        val u = e.getClient.fetchUser(s.toLong)
        lb += (m(g).toLong -> (u.getName + "#" + u.getDiscriminator))
      })
    })
    lb.keySet.toList.sorted.foreach((t: Long) => {
      eb.appendDesc(FormatUtil.msToTime(t) + ": " + lb(t) + "\n")
    })
    eb.appendDesc("```")
    mb.withChannel(e.getChannel)
    mb.withEmbed(eb.build())
    mb.send()
    true
  })

}
