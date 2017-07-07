package com.tsunderebug.speedrunbot.command

import java.io.FileNotFoundException

import com.tsunderebug.speedrun4j.game.run.PlacedRun
import com.tsunderebug.speedrun4j.game.{Game, GameList, Leaderboard}
import com.tsunderebug.speedrun4j.user.User
import com.tsunderebug.speedrunbot.FormatUtil
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.MessageBuilder

object PersonalBest extends Command("pb", "Lists PBs", (_: MessageReceivedEvent) => true, (e: MessageReceivedEvent) => {
  val name = e.getMessage.getContent.split("\\s+")(2)
  try {
    val page = if (e.getMessage.getContent.split("\\s+").length >= 4) e.getMessage.getContent.split("\\s+")(3).toInt else 1
    val u = User.fromID(name)
    val pbs = u.getPBs
    val pbd = pbs.getData.filter(_.getRun.getCategory.getType != "per-level")
    val mb = new MessageBuilder(e.getClient)
    mb.appendContent("PBs for user " + u.getNames.get("international") + ": _(" + ((page - 1) * 20 + 1) + "-" + Math.min(page * 20, pbd.length) + " out of " + pbd.length + ")_```\n")
    pbd.slice((page - 1) * 20, page * 20).foreach((r: PlacedRun) => {
      val c = r.getRun.getCategory
      val g = c.getGame
      val p = r.getPlace
      val t = FormatUtil.msToTime((r.getRun.getTimes.getPrimaryT * 1000).asInstanceOf[Long])
      mb.appendContent(g.getNames.get("international") + ": " + c.getName + " - " + p + (p % 10 match {
        case 1 if p % 100 != 11 => "st"
        case 2 if p % 100 != 12 => "nd"
        case 3 if p % 100 != 13 => "rd"
        case _ => "th"
      }) + " with " + t + "\n")
    })
    mb.appendContent("```")
    mb.withChannel(e.getChannel)
    mb.send()
  } catch {
    case _: FileNotFoundException => e.getChannel.sendMessage("Not a valid speedrun.com user!")
    case _: NumberFormatException => e.getChannel.sendMessage("Not a valid page number!")
  }
  true
}
) {

  def runs(game: String, category: String): Array[PlacedRun] = {
    val g = GameList.withName(game).getGames.find(_.getNames.values().contains(game)).get
    val c = g.getCategories.getCategories.filter(_.getType != "per-level").find(_.getName == category).get
    Leaderboard.forCategory(c).getRuns
  }

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
      try {
        val pr = runs(game, category)
      } catch {
        case _: NoSuchElementException => {
          e.getChannel.sendMessage("Didn't find **" + game + "**, **" + category + "**. Run `-s pb [game]` to find categories for your game. _Note: Categories with a `: ` in them are not supported yet._")
        }
      }
    }
    true
  })

  object WorldRecord extends Command("wr", "Get information about the World Record run of a Game: Category", (_: MessageReceivedEvent) => true, (e: MessageReceivedEvent) => {

    true
  })

}
