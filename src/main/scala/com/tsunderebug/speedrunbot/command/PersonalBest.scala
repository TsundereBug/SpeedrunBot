package com.tsunderebug.speedrunbot.command

import com.tsunderebug.speedrun4j.game.run.PlacedRun
import com.tsunderebug.speedrun4j.game.{Game, GameList, Leaderboard}
import com.tsunderebug.speedrunbot.FormatUtil
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.{EmbedBuilder, MessageBuilder}

object PersonalBest extends Command("pb", "Lists PBs", (_: MessageReceivedEvent) => true, (e: MessageReceivedEvent) => {
  e.getChannel.sendMessage("Command in rework for Speedrun.com sync")
  true
}
) {

  object Game extends Command("game", "Sorts and searches PBs by game name", (_: MessageReceivedEvent) => true, (e: MessageReceivedEvent) => {
    val full = e.getMessage.getContent.split("\\s+").drop(3).mkString(" ")
    val probable: Array[Game] = GameList.withName(full).getGames
    if (!full.contains(": ")) {
      if (probable.exists(_.getNames.values().contains(full))) {
        e.getChannel.sendMessage("Specify a category! One of:```\n" + probable.find(_.getNames.values().contains(full)).get.getCategories.getCategories.filter(_.getType != "per-level").foldLeft("")(_ + _.getName + "\n") + "```")
      } else {
        e.getChannel.sendMessage("Didn't find **" + full + "** as a game. Did you mean:```\n" + probable.foldLeft("")(_ + _.getNames.get("international") + "\n") + "```")
      }
    } else {
      val (beforeLast, afterLast) = full.splitAt(full.lastIndexOf(": "))
      val Array(game, category) = (beforeLast + ":" + afterLast).split(":: ", 2)
      val gl: Array[Game] = GameList.withName(game).getGames
      if (gl.exists(_.getNames.values().contains(game))) {
        val g = gl.find(_.getNames.values().contains(game)).get
        val cl = g.getCategories.getCategories.filter(_.getType != "per-level")
        if (cl.exists(_.getName == category)) {
          val c = cl.find(_.getName == category).get
          val mb = new MessageBuilder(e.getClient)
          mb.withContent("Leaderboard for **" + game + "**:")
          val eb = new EmbedBuilder
          eb.withColor(0x43b581)
          eb.setLenient(false)
          eb.appendDesc("```\n")
          val runs = Leaderboard.forCategory(c).getRuns
          runs.take(20).sortBy(_.getPlace).foreach((r: PlacedRun) => eb.appendDesc(FormatUtil.msToTime((r.getRun.getTimes.getPrimaryT * 1000).asInstanceOf[Long]) + " - " + r.getRun.getPlayers.map(_.getName).mkString(", ") + "\n"))
          eb.appendDesc("```")
          mb.withChannel(e.getChannel)
          mb.withEmbed(eb.build())
          mb.send()
        } else {
          e.getChannel.sendMessage("Not a category! One of:```\n" + cl.foldLeft("")(_ + _.getName + "\n") + "```")
        }
      } else if (probable.exists(_.getNames.values().contains(full))) {
        e.getChannel.sendMessage("Specify a category! One of:```\n" + probable.find(_.getNames.values().contains(full)).get.getCategories.getCategories.filter(_.getType != "per-level").foldLeft("")(_ + _.getName + "\n") + "```")
      } else {
        e.getChannel.sendMessage("Didn't find **" + full + "** as a game. Did you mean:```\n" + gl.foldLeft("")(_ + _.getNames.get("international") + "\n" + "```"))
      }
    }
    true
  })

}
