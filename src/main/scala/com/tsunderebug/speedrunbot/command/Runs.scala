package com.tsunderebug.speedrunbot.command

import java.awt.Color
import java.io.{FileNotFoundException, IOException}

import com.tsunderebug.speedrun4j.game.run.{PlacedRun, Run}
import com.tsunderebug.speedrun4j.game.{Category, GameList, Leaderboard}
import com.tsunderebug.speedrun4j.user.User
import com.tsunderebug.speedrunbot.data.Database
import com.tsunderebug.speedrunbot.{FormatUtil, PermissionChecks}
import sx.blah.discord.api.internal.json.objects.EmbedObject
import sx.blah.discord.handle.obj.{IGuild, IRole, IUser, Permissions}
import sx.blah.discord.util.{EmbedBuilder, RequestBuffer}

import scala.collection.JavaConverters._
import scala.collection.{SortedMap, mutable}

object Runs {

  def place(p: Int): String = {
    p % 10 match {
      case 1 if (p % 100) / 10 != 1 => p + "st"
      case 2 if (p % 100) / 10 != 1 => p + "nd"
      case 3 if (p % 100) / 10 != 1 => p + "rd"
      case _ => p + "th"
    }
  }

  def runListEmbed(runs: Array[PlacedRun]): EmbedObject = runListEmbed(runs, order = false)

  def runListEmbed(runs: Array[PlacedRun], order: Boolean): EmbedObject = runListEmbed(runs, order, 0)

  def runListEmbed(runs: Array[PlacedRun], order: Boolean, page: Int): EmbedObject = {
    val r = (if (order) runs.sortBy(_.getPlace) else runs).slice(20 * page, 20 * page + 20)
    val eb = new EmbedBuilder
    if (r.isEmpty) {
      eb.appendDesc("No runs here!")
    } else {
      eb.withColor(runs(0).getRun.getPlayers()(0).getColor)
      eb.setLenient(false)
      eb.appendDescription("Page " + (page + 1) + " out of " + (runs.length / 20 + 1) + "\n")
      eb.appendDescription("```\n")
      r.foreach((r) => {
        eb.appendDescription((FormatUtil.msToTime((r.getRun.getTimes.getPrimaryT * 1000).asInstanceOf[Long]) + " - " + place(r.getPlace) + " place run of " + r.getRun.getCategory.getGame.getNames.get("international") + ": " + r.getRun.getCategory.getName + " (by " + r.getRun.getPlayers.map(_.getName).mkString(", ") + ")\n").replaceAll(" ", "\u00A0"))
      })
      eb.appendDesc("```")
    }
    eb.build()
  }

  object List extends Command("runs", "List your or mention user's runs if you are connected to SRCom.", (_) => true, (e) => {
    val u: String = if (e.getMessage.getContent.split("\\s+").length > 2) {
      if (e.getMessage.getMentions.isEmpty) {
        e.getMessage.getContent.split("\\s+")(2)
      } else {
        e.getMessage.getMentions.get(0).getStringID
      }
    } else {
      e.getAuthor.getStringID
    }
    if (u.matches("""\d+""") && !Database.db.srcomlinks.contains(u)) {
      e.getChannel.sendMessage((if (e.getAuthor.getStringID == u) "You have " else "<@!" + u + "> has ") + "not linked Speedrun.com with Discord. This is accomplished by running `-s runs link` in a PM.")
    } else {
      try {
        val sru: User = if (u.matches("""\d+""")) User.fromID(Database.db.srcomlinks(u)) else User.fromID(u)
        e.getChannel.sendMessage(sru.getNames.get("international") + "'s runs:", runListEmbed(sru.getPBs.getData, order = false, try {
          e.getMessage.getContent.split("\\s+")(3).toInt - 1
        } catch {
          case _: NumberFormatException | _: ArrayIndexOutOfBoundsException => 0
        }))
      } catch {
        case _: FileNotFoundException => e.getChannel.sendMessage("Not a valid Speedrun.com user!")
      }
    }
    true
  })

  object Game extends Command("game", "Lists runs for a Game: Category", (_) => true, (e) => {
    val gc: String = try {
      e.getMessage.getContent.split("\\s+").takeRight(1)(0).toInt
      e.getMessage.getContent.split("\\s+").drop(3).dropRight(1).mkString(" ")
    } catch {
      case _: NumberFormatException => e.getMessage.getContent.split("\\s+").drop(3).mkString(" ")
    }
    val p: Int = try {
      e.getMessage.getContent.split("\\s+").takeRight(1)(0).toInt - 1
    } catch {
      case _: NumberFormatException => 0
    }
    gameCat(gc) match {
      case Some(c) =>
        e.getChannel.sendMessage("Runs for **" + c.getGame.getNames.get("international") + ": " + c.getName + "**:", runListEmbed(Leaderboard.forCategory(c).getRuns, order = true, p))
      case None =>
        e.getChannel.sendMessage("Couldn't find **Game: Category** by **" + gc + "**.")
        val gl: GameList = GameList.withName(gc.split(": ")(0))
        val go: Option[com.tsunderebug.speedrun4j.game.Game] = gl.getGames.find(_.getNames.get("international") == gc.split(": ")(0))
        if (go.isDefined) {
          e.getChannel.sendMessage("Valid categories for **" + go.get.getNames.get("international") + "** are:\n```\n" + go.get.getCategories.getCategories.map(_.getName).mkString("\n") + "\n```")
        } else {
          e.getChannel.sendMessage("Games you might be looking for:\n```\n" + gl.getGames.map(_.getNames.get("international")).mkString("\n") + "\n```")
        }
    }
    true
  })

  def splitApply2(str: String, sep: String, fun: ((String, String)) => Boolean): Option[(String, String)] = {
    val ss = str.split(sep).toList
    ss.indices.drop(1) map ss.splitAt map ((t) => (t._1.mkString(sep), t._2.mkString(sep))) find { case (prefix, tail) =>
      fun(prefix, tail)
    }
  }

  def gameCat(gc: String): Option[Category] = {
    splitApply2(gc, ": ", (t) => {
      val g = t._1
      val c = t._2
      val gl: GameList = GameList.withName(g)
      gl.getGames.count(_.getNames.get("international") == g) == 1 && gl.getGames()(0).getCategories.getCategories.filter((c) => c.getType != "per-level").exists(_.getName == c)
    }) match {
      case None => None
      case Some((g, c)) => GameList.withName(g).getGames.find(_.getNames.values().contains(g)) match {
        case Some(g) => g.getCategories.getCategories.filter((c) => c.getType != "per-level").find(_.getName == c)
        case None => None
      }
    }
  }

  object Link extends Command("link", "Link your Discord to your SRCom. **Only works in PMs.**", _.getChannel.isPrivate, (e) => {
    if (e.getMessage.getContent.split("\\s+").length == 4) {
      e.getChannel.sendMessage("Getting your user ID from your token... _SpeedrunBot does not store tokens. If you suspect someone is using your token in a malicious way, reset it by going to your settings and to API Key and clicking \"Generate new Token\"._")
      try {
        val u = User.fromApiKey(e.getMessage.getContent.split("\\s+").last)
        e.getChannel.sendMessage("Hello, " + u.getNames.get("international") + "!")
        Database.linkSrCoom(e.getAuthor, u.getId)
      } catch {
        case _: IOException => e.getChannel.sendMessage("That's not a valid API token! Get your token at <https://www.speedrun.com/api/auth>.")
      }
    } else {
      e.getChannel.sendMessage("Incorrect usage! Get your token at https://www.speedrun.com/api/auth and run `-s runs link [token]` in a PM.")
    }
    true
  })

  object ForceLink extends Command("force", "Only usable by owner.", PermissionChecks.isGlobal, (e) => {
    val d = e.getMessage.getContent.split("\\s+")(4)
    val s = e.getMessage.getContent.split("\\s+")(5)
    val u: IUser = e.getClient.fetchUser(d.toLong)
    val sru: User = User.fromID(s)
    Database.linkSrCoom(u, sru.getId)
    val eb = new EmbedBuilder
    eb.withColor(sru.getNameStyle.getColor)
    eb.appendDescription("Linked " + u.mention() + " with " + sru.getId + " (" + sru.getNames.get("international") + ")")
    e.getChannel.sendMessage(eb.build())
    true
  })

  def setupRole(g: IGuild, name: String, color: Color): IRole = {
    val l = g.getRolesByName(name)
    if (l.isEmpty) {
      val r: IRole = RequestBuffer.request(() => g.createRole()).get()
      RequestBuffer.request(() => r.changeName(name)).get()
      RequestBuffer.request(() => r.changeColor(color)).get()
      r
    } else {
      val r = l.get(0)
      RequestBuffer.request(() => r.changeColor(color)).get()
      r
    }
  }

  val redc = new Color(0xee4444)
  val coralc = new Color(0xe77471)
  val orangec = new Color(0xef8241)
  val yellowc = new Color(0xf0c03e)
  val greenc = new Color(0x8ac951)
  val mintc = new Color(0x09b876)
  val azurec = new Color(0x44bbee)
  val bluec = new Color(0x6666ee)
  val purplec = new Color(0xa010a0)
  val lavenderc = new Color(0xc279e5)
  val pinkc = new Color(0xf772c5)
  val fuchsiac = new Color(0xff3091)
  val silverc = new Color(0xb8b8b8)
  val whitec = Color.WHITE
  val colors = Seq(redc, coralc, orangec, yellowc, greenc, mintc, azurec, bluec, purplec, lavenderc, pinkc, fuchsiac, silverc, whitec)

  object Role extends Command("role", "Creates colored roles for SRCom users.", PermissionChecks.manageServer, (e) => {
    val g = e.getGuild
    if (!e.getClient.getOurUser.getPermissionsForGuild(g).contains(Permissions.MANAGE_ROLES)) {
      e.getChannel.sendMessage("I don't have the Manage Roles permission!")
    } else {
      val l = setupRole(g, "Linked to Speedrun.com", Color.BLACK)
      val rm: SortedMap[Color, IRole] = SortedMap(colors.zip(Seq(
        setupRole(g, "SpeedrunRed", redc),
        setupRole(g, "SpeedrunCoral", coralc),
        setupRole(g, "SpeedrunOrange", orangec),
        setupRole(g, "SpeedrunYellow", yellowc),
        setupRole(g, "SpeedrunGreen", greenc),
        setupRole(g, "SpeedrunMint", mintc),
        setupRole(g, "SpeedrunAzure", azurec),
        setupRole(g, "SpeedrunBlue", bluec),
        setupRole(g, "SpeedrunPurple", purplec),
        setupRole(g, "SpeedrunLavender", lavenderc),
        setupRole(g, "SpeedrunPink", pinkc),
        setupRole(g, "SpeedrunFuchsia", fuchsiac),
        setupRole(g, "SpeedrunSilver", silverc),
        setupRole(g, "SpeedrunWhite", whitec)
      )).toArray: _*)(colors.indexOf(_) - colors.indexOf(_))
      if (e.getMessage.getContent.split("\\s+").length > 3) {
        val rl: java.util.List[IRole] = RequestBuffer.request(() => g.getRolesByName(e.getMessage.getContent.split("\\s+")(3))).get()
        if (!rl.isEmpty) {
          val r = rl.get(0)
          val eb = new EmbedBuilder
          eb.withColor(r.getColor)
          eb.appendDesc("<@&" + r.getStringID + ">")
          e.getChannel.sendMessage("Placing Speedrun roles over", eb.build())
          val rh = g.getRoles
          rh.removeAll(rm.values.asJavaCollection)
          val i = rh.indexOf(r) + 1
          rm.values.foreach(rh.add(i, _))
          g.reorderRoles(rh.asScala.toArray: _*)
        }
      }
      e.getGuild.getUsers.forEach((u) => {
        if (Database.db.srcomlinks.contains(u.getStringID)) {
          RequestBuffer.request(() => u.addRole(l))
          val uc = User.fromID(Database.db.srcomlinks(u.getStringID)).getNameStyle.getColor
          val c = colors.minBy((c) => Math.sqrt(Math.pow(Math.abs(c.getRed - uc.getRed), 2) + Math.pow(Math.abs(c.getGreen - uc.getGreen), 2) + Math.pow(Math.abs(c.getBlue - uc.getBlue), 2)))
          RequestBuffer.request(() => u.addRole(rm(c)))
        }
      })
    }
    true
  })

  def runInfoEmbed(r: PlacedRun): EmbedObject = {
    val eb = new EmbedBuilder
    eb.withColor(r.getRun.getPlayers()(0).getColor)
    eb.withAuthorName(r.getRun.getPlayers.map(_.getName).mkString(", ") + "'s run of " + r.getRun.getCategory.getGame.getNames.get("international") + ": " + r.getRun.getCategory.getName)
    eb.appendField("Place", place(r.getPlace), true)
    eb.appendField("Time", FormatUtil.msToTime((r.getRun.getTimes.getPrimaryT * 1000).asInstanceOf[Long]), true)
    eb.appendField("Runners", r.getRun.getPlayers.map(_.getName).mkString(", "), true)
    eb.withAuthorIcon(r.getPlace match {
      case 1 if r.getRun.getCategory.getGame.getAssets.getTrophyFirst != null => r.getRun.getCategory.getGame.getAssets.getTrophyFirst.getUri
      case 2 if r.getRun.getCategory.getGame.getAssets.getTrophySecond != null => r.getRun.getCategory.getGame.getAssets.getTrophySecond.getUri
      case 3 if r.getRun.getCategory.getGame.getAssets.getTrophyThird != null => r.getRun.getCategory.getGame.getAssets.getTrophyThird.getUri
      case 4 if r.getRun.getCategory.getGame.getAssets.getTrophyFourth != null => r.getRun.getCategory.getGame.getAssets.getTrophyFourth.getUri
      case _ => ""
    })
    eb.withAuthorUrl(r.getRun.getWeblink)
    try {
      eb.withTimestamp(r.getRun.getStatus.getVerifyDate.getTime)
      eb.withFooterText("Run verified on")
    } catch {
      case _: NullPointerException =>
    }
    eb.build()
  }

  def runInfoEmbed(r: Run): EmbedObject = {
    val eb = new EmbedBuilder
    eb.withColor(r.getPlayers()(0).getColor)
    eb.withAuthorName(r.getPlayers.map(_.getName).mkString(", ") + "'s run of " + r.getCategory.getGame.getNames.get("international") + ": " + r.getCategory.getName)
    eb.appendField("Time", FormatUtil.msToTime((r.getTimes.getPrimaryT * 1000).asInstanceOf[Long]), true)
    eb.appendField("Runners", r.getPlayers.map(_.getName).mkString(", "), true)
    eb.withAuthorUrl(r.getWeblink)
    eb.build()
  }

  object WorldRecord extends Command("wr", "Show info about the world record run for a Game: Category", (_) => true, (e) => {
    val gc = e.getMessage.getContent.split("\\s+").drop(3).mkString(" ")
    gameCat(gc) match {
      case Some(c) =>
        e.getChannel.sendMessage("World record run for **" + c.getGame.getNames.get("international") + ": " + c.getName + "**", runInfoEmbed(Leaderboard.forCategory(c).getRuns.sortBy(_.getPlace).apply(0)))
      case None =>
        e.getChannel.sendMessage("Couldn't find **Game: Category** by **" + gc + "**.")
        val gl: GameList = GameList.withName(gc.split(": ")(0))
        val go: Option[com.tsunderebug.speedrun4j.game.Game] = gl.getGames.find(_.getNames.get("international") == gc.split(": ")(0))
        if (go.isDefined) {
          e.getChannel.sendMessage("Valid categories for **" + go.get.getNames.get("international") + "** are:\n```\n" + go.get.getCategories.getCategories.map(_.getName).mkString("\n") + "\n```")
        } else {
          e.getChannel.sendMessage("Games you might be looking for:\n```\n" + gl.getGames.map(_.getNames.get("international")).mkString("\n") + "\n```")
        }
    }
    true
  })

  object PersonalBest extends Command("pb", "Show info about a user's PB for Game: Category", (_) => true, (e) => {
    try {
      val u: User = if (e.getMessage.getMentions.isEmpty) {
        User.fromID(e.getMessage.getContent.split("\\s+")(3))
      } else {
        val i = e.getMessage.getMentions.get(0).getStringID
        if (Database.db.srcomlinks.contains(i)) {
          User.fromID(Database.db.srcomlinks(i))
        } else {
          null
        }
      }
      val gc: String = e.getMessage.getContent.split("\\s+").drop(4).mkString(" ")
      if (u != null) {
        gameCat(gc) match {
          case Some(c) =>
            e.getChannel.sendMessage(u.getNames.get("international") + "'s run of " + c.getGame.getNames.get("international") + ": " + c.getName, runInfoEmbed(u.getPBs.getData.filter(_.getRun.getCategory.getId == c.getId)(0)))
          case None =>
            e.getChannel.sendMessage("Couldn't find **Game: Category** by **" + gc + "**.")
            val gl: GameList = GameList.withName(gc.split(": ")(0))
            val go: Option[com.tsunderebug.speedrun4j.game.Game] = gl.getGames.find(_.getNames.get("international") == gc.split(": ")(0))
            if (go.isDefined) {
              e.getChannel.sendMessage("Valid categories for **" + go.get.getNames.get("international") + "** are:\n```\n" + go.get.getCategories.getCategories.map(_.getName).mkString("\n") + "\n```")
            } else {
              e.getChannel.sendMessage("Games you might be looking for:\n```\n" + gl.getGames.map(_.getNames.get("international")).mkString("\n") + "\n```")
            }
        }
      } else {
        e.getChannel.sendMessage(e.getMessage.getMentions.get(0).mention() + " hasn't linked their Speedrun.com account to their Discord account. They can run -s runs for instructions.")
      }
    } catch {
      case _: ArrayIndexOutOfBoundsException | _: IndexOutOfBoundsException =>
        e.getChannel.sendMessage("Incorrect syntax! Usage: `-s runs pb [user] [game: category]`")
    }
    true
  })

  object Updates extends Command("updates", "Use subcommands to configure run update settings", (_) => true, (_) => true)

  object GameUpdates extends Command("game", "Add or remove a game to check updates for", (e) => PermissionChecks.manageServer(e) || e.getMessage.getContent.split("\\s+").drop(4).isEmpty, (e) => {
    val g = e.getMessage.getContent.split("\\s+").drop(4).mkString(" ")
    val gl = GameList.withName(g)
    val go = gl.getGames.find(_.getNames.values().contains(g))
    if (go.isDefined) {
      e.getChannel.sendMessage("Toggling " + go.get.getNames.get("international") + " (" + go.get.getId + ") for Verification notifications.")
      Database.toggleGame(go.get.getId, e.getGuild)
      val gm = Database.db.gameUpdates
      val gs: mutable.Set[String] = mutable.Set()
      gm.foreach((t) => {
        if (t._2(e.getGuild.getStringID)) gs += t._1
      })
      e.getChannel.sendMessage("You now receive updates for " + gs.map(com.tsunderebug.speedrun4j.game.Game.fromID(_).getNames.get("international")).mkString(", "))
    } else if (g == "") {
      val gm = Database.db.gameUpdates
      val gs: mutable.Set[String] = mutable.Set()
      gm.foreach((t) => {
        if (t._2(e.getGuild.getStringID)) gs += t._1
      })
      e.getChannel.sendMessage("You receive updates for " + gs.map(com.tsunderebug.speedrun4j.game.Game.fromID(_).getNames.get("international")).mkString(", "))
    } else {
      e.getChannel.sendMessage("Couldn't find a game by **" + g + "**. Did you mean any of\n```\n" + gl.getGames.map(_.getNames.get("international")).mkString("\n") + "\n```")
    }
    true
  })

  object UpdateChannel extends Command("channel", "Set the channel updates go to", (e) => PermissionChecks.manageServer(e) || e.getMessage.getChannelMentions.isEmpty, (e) => {
    if (e.getMessage.getChannelMentions.isEmpty) {
      e.getChannel.sendMessage("Run verification updates are sent to <#" + Database.db.runUpdatesChannel.getOrElse(e.getGuild.getStringID, e.getGuild.getGeneralChannel.getStringID) + ">")
    } else {
      val ch = e.getMessage.getChannelMentions.get(0)
      Database.setRunUpdatesChannel(e.getGuild, ch)
      e.getChannel.sendMessage("Run verification updates now go to " + ch.mention())
    }
    true
  })

  object NewRunChannel extends Command("mod", "Set the channel new, unverified runs go to", (e) => PermissionChecks.manageServer(e), (e) => {
    if (e.getMessage.getChannelMentions.isEmpty) {
      if (Database.db.modVerificationChannel.contains(e.getGuild.getStringID)) {
        e.getChannel.sendMessage("Run verification list is in <#" + Database.db.modVerificationChannel(e.getGuild.getStringID) + ">")
      } else {
        e.getChannel.sendMessage("Your run verification updates are disabled. Either you have not enabled them or your channel was not empty.")
      }
    } else {
      val ch = e.getMessage.getChannelMentions.get(0)
      if (ch.getMessageHistory(1).isEmpty) {
        Database.setModVerificationChannel(e.getGuild, ch)
        e.getChannel.sendMessage("Set verification updates to go to " + ch.mention() + ". If this channel does not remain empty (someone else chats in it) updates will be turned off.")
      } else {
        e.getChannel.sendMessage("That channel isn't empty! Verification updates require an empty channel to function properly.")
      }
    }
    true
  })

}
