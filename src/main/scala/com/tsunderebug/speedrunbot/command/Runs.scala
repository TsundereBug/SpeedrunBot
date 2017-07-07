package com.tsunderebug.speedrunbot.command

import java.io.FileNotFoundException

import com.tsunderebug.speedrun4j.game.run.PlacedRun
import com.tsunderebug.speedrun4j.game.{Game, GameList, Leaderboard}
import com.tsunderebug.speedrun4j.user.User
import com.tsunderebug.speedrunbot.FormatUtil
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.util.{EmbedBuilder, MessageBuilder}

object Runs extends Command("runs", "Lists runs", (_: MessageReceivedEvent) => true, (e: MessageReceivedEvent) => {
  def oname(num: Int): String = {
    num + (num % 10 match {
      case 1 if num % 100 != 11 => "st"
      case 2 if num % 100 != 12 => "nd"
      case 3 if num % 100 != 13 => "rd"
      case _ => "th"
    })
  }
  val name = e.getMessage.getContent.split("\\s+")(2)
  try {
    val u = User.fromID(name)
    val pbs = u.getPBs
    val pbd = pbs.getData.filter(_.getRun.getCategory.getType != "per-level")
    if (e.getMessage.getContent.contains(" - ")) {
      val gamec = e.getMessage.getContent.substring(e.getMessage.getContent.indexOf(" - ") + 3)
      val (beforeLast, afterLast) = gamec.splitAt(gamec.lastIndexOf(": "))
      val Array(game, category) = (beforeLast + ":" + afterLast).split(":: ", 2)
      val upb = pbd.filter((r: PlacedRun) => r.getRun.getCategory.getGame.getNames.containsValue(game) && r.getRun.getCategory.getName == category)
      if (upb.isEmpty) {
        e.getChannel.sendMessage("Didn't find any runs of **" + game + ": " + category + "** by **" + name + "**.")
      } else if (upb.length > 1) {
        e.getChannel.sendMessage("Found multiple runs of **" + game + ": " + category + "** by **" + name + "**. _There is no way to select one at the moment._")
      } else {
        def showRunDetails(c: IChannel, run: PlacedRun): Unit = {
          val eb = new EmbedBuilder
          eb.withColor(run.getRun.getPlayers.head.getColor)
          eb.withAuthorName(run.getRun.getPlayers.map(_.getName).mkString(", ") + "'s " + oname(run.getPlace) + " place run of " + run.getRun.getCategory.getGame.getNames.get("international") + ": " + run.getRun.getCategory.getName + " with " + FormatUtil.msToTime((run.getRun.getTimes.getPrimaryT * 1000).asInstanceOf[Int]))
          eb.appendField("Notes:", run.getRun.getComment, false)
          eb.withThumbnail(run.getRun.getCategory.getGame.getAssets.getCoverLarge.getUri)
          eb.appendDescription("**Video(s):** " + run.getRun.getVideos.getLinks.map(_.getUri).mkString("<", ">, <", ">"))
          run.getPlace match {
            case 1 => eb.withAuthorIcon(run.getRun.getCategory.getGame.getAssets.getTrophyFirst.getUri)
            case 2 => eb.withAuthorIcon(run.getRun.getCategory.getGame.getAssets.getTrophySecond.getUri)
            case 3 => eb.withAuthorIcon(run.getRun.getCategory.getGame.getAssets.getTrophyThird.getUri)
            case 4 => eb.withAuthorIcon(run.getRun.getCategory.getGame.getAssets.getTrophyFourth.getUri)
            case _ => eb.withAuthorIcon(run.getRun.getCategory.getGame.getAssets.getIcon.getUri)
          }
          c.sendMessage(eb.build())
        }

        showRunDetails(e.getChannel, upb.head)
      }
    } else {
      val page = if (e.getMessage.getContent.split("\\s+").length >= 4) e.getMessage.getContent.split("\\s+")(3).toInt else 1
      val mb = new MessageBuilder(e.getClient)
      mb.appendContent("PBs for user " + u.getNames.get("international") + ": _(" + ((page - 1) * 20 + 1) + "-" + Math.min(page * 20, pbd.length) + " out of " + pbd.length + ")_```\n")
      pbd.slice((page - 1) * 20, page * 20).foreach((r: PlacedRun) => {
        val c = r.getRun.getCategory
        val g = c.getGame
        val p = r.getPlace
        val t = FormatUtil.msToTime((r.getRun.getTimes.getPrimaryT * 1000).asInstanceOf[Long])
        mb.appendContent(g.getNames.get("international") + ": " + c.getName + " - " + oname(num = p) + " with " + t + "\n")
      })
      mb.appendContent("```")
      mb.withChannel(e.getChannel)
      mb.send()
    }
  } catch {
    case _: FileNotFoundException => e.getChannel.sendMessage("Not a valid speedrun.com user!")
    case _: NumberFormatException => e.getChannel.sendMessage("Not a valid page number!")
  }
  true
}
) {

  def name(num: Int): String = {
    num + (num % 10 match {
      case 1 if num % 100 != 11 => "st"
      case 2 if num % 100 != 12 => "nd"
      case 3 if num % 100 != 13 => "rd"
      case _ => "th"
    })
  }

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
        val mb = new MessageBuilder(e.getClient)
        mb.withChannel(e.getChannel)
        mb.appendContent("Runs for **" + game + ": " + category + "**:\n```" + pr.sortBy(_.getPlace).take(20).map((r) => FormatUtil.msToTime((r.getRun.getTimes.getPrimaryT * 1000).asInstanceOf[Int]) + " - " + r.getRun.getPlayers.map(_.getName).mkString(", ")).mkString("\n") + "```")
        mb.send()
      } catch {
        case _: NoSuchElementException =>
          e.getChannel.sendMessage("Didn't find **" + game + "**, **" + category + "**. Run `-s pb [game]` to find categories for your game. _Note: Categories with a `: ` in them are not supported yet._")
      }
    }
    true
  })

  def showRunDetails(c: IChannel, run: PlacedRun): Unit = {
    val eb = new EmbedBuilder
    eb.withColor(run.getRun.getPlayers.head.getColor)
    eb.withAuthorName(run.getRun.getPlayers.map(_.getName).mkString(", ") + "'s " + name(run.getPlace) + " place run of " + run.getRun.getCategory.getGame.getNames.get("international") + ": " + run.getRun.getCategory.getName + " with " + FormatUtil.msToTime((run.getRun.getTimes.getPrimaryT * 1000).asInstanceOf[Int]))
    eb.appendField("Notes:", run.getRun.getComment, false)
    eb.withThumbnail(run.getRun.getCategory.getGame.getAssets.getCoverLarge.getUri)
    eb.appendDescription("**Video(s):** " + run.getRun.getVideos.getLinks.map(_.getUri).mkString("<", ">, <", ">"))
    run.getPlace match {
      case 1 => eb.withAuthorIcon(run.getRun.getCategory.getGame.getAssets.getTrophyFirst.getUri)
      case 2 => eb.withAuthorIcon(run.getRun.getCategory.getGame.getAssets.getTrophySecond.getUri)
      case 3 => eb.withAuthorIcon(run.getRun.getCategory.getGame.getAssets.getTrophyThird.getUri)
      case 4 => eb.withAuthorIcon(run.getRun.getCategory.getGame.getAssets.getTrophyFourth.getUri)
    }
    c.sendMessage(eb.build())
  }

  object WorldRecord extends Command("wr", "Get information about the World Record run of a Game: Category", (_: MessageReceivedEvent) => true, (e: MessageReceivedEvent) => {
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
        showRunDetails(e.getChannel, pr.minBy(_.getPlace))
      } catch {
        case _: NoSuchElementException =>
          e.getChannel.sendMessage("Didn't find **" + game + "**, **" + category + "**. Run `-s pb [game]` to find categories for your game. _Note: Categories with a `: ` in them are not supported yet._")
      }
    }
    true
  })

}
