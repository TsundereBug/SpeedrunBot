package com.tsunderebug.speedrunbot.command

import java.io.IOException
import java.util

import com.tsunderebug.speedrun4j.game.run.PlacedRun
import com.tsunderebug.speedrun4j.user.User
import com.tsunderebug.speedrunbot.data.Database
import com.tsunderebug.speedrunbot.{FormatUtil, PermissionChecks}
import sx.blah.discord.api.internal.json.objects.EmbedObject
import sx.blah.discord.handle.obj.{IRole, Permissions}
import sx.blah.discord.util.{EmbedBuilder, RequestBuffer}

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
    val u = if (e.getMessage.getMentions.isEmpty) e.getAuthor else e.getMessage.getMentions.get(0)
    if (Database.db.srcomlinks.contains(u.getStringID)) {
      val su = User.fromID(Database.db.srcomlinks(u.getStringID))
      try {
        val p: Int = e.getMessage.getContent.split("\\s+")(if (e.getMessage.getMentions.isEmpty) 2 else 3).toInt
        e.getChannel.sendMessage(su.getNames.get("international") + "'s runs:", runListEmbed(su.getPBs.getData, order = false, p - 1))
      } catch {
        case _: NumberFormatException | _: ArrayIndexOutOfBoundsException =>
          e.getChannel.sendMessage(su.getNames.get("international") + "'s runs:", runListEmbed(su.getPBs.getData))
      }
    } else {
      if (u.equals(e.getAuthor)) {
        e.getChannel.sendMessage("You have not linked your Speedrun.com account to your Discord account! Do it by running `-s runs link` in a PM.")
      } else {
        e.getChannel.sendMessage(u.mention + " has not linked their Speedrun.com account to their Discord account. They can do it by running `-s runs link` in a PM.")
      }
    }
    true
  })

  object Link extends Command("link", "Link your Discord to your SRCom. **Only works in PMs.**", _.getChannel.isPrivate, (e) => {
    if (e.getMessage.getContent.split("\\s+").length == 4) {
      e.getChannel.sendMessage("Getting your user ID from your token... _SpeedrunBot does not store tokens. If you suspect someone is using your token in a malicious way, reset it at ..._")
      try {
        val u = User.fromApiKey(e.getMessage.getContent.split("\\s+").last)
        e.getChannel.sendMessage("Hello, " + u.getNames.get("international") + "!")
        Database.linkSrCoom(e.getAuthor, u.getId)
      } catch {
        case _: IOException => e.getChannel.sendMessage("That's not a valid API token! Get your token at <http://www.speedrun.com/api/auth>.")
      }
    } else {
      e.getChannel.sendMessage("Incorrect usage! Get your token at http://www.speedrun.com/api/auth and run `-s runs link [token]` in a PM.")
    }
    true
  })

  object Role extends Command("role", "Creates colored roles for SRCom users.", PermissionChecks.manageServer, (e) => {
    val l: IRole = if (e.getGuild.getRolesByName("Linked to Speedrun.com").isEmpty) {
      val lr = e.getGuild.createRole()
      lr.changeName("Linked to Speedrun.com")
      lr.changePermissions(util.EnumSet.noneOf(classOf[Permissions]))
      lr
    } else {
      e.getGuild.getRolesByName("Linked to Speedrun.com").get(0)
    }
    val red = e.getGuild.getRolesByName("SpeedrunRed")
    val coral = e.getGuild.getRolesByName("SpeedrunCoral")
    val orange = e.getGuild.getRolesByName("SpeedrunOrange")
    val yellow = e.getGuild.getRolesByName("SpeedrunYellow")
    val green = e.getGuild.getRolesByName("SpeedrunGreen")
    val mint = e.getGuild.getRolesByName("SpeedrunMint")
    val azure = e.getGuild.getRolesByName("SpeedrunAzure")
    val blue = e.getGuild.getRolesByName("SpeedrunBlue")
    val purple = e.getGuild.getRolesByName("SpeedrunPurple")
    val lavender = e.getGuild.getRolesByName("SpeedrunLavender")
    val pink = e.getGuild.getRolesByName("SpeedrunPink")
    val silver = e.getGuild.getRolesByName("SpeedrunSilver")
    val white = e.getGuild.getRolesByName("SpeedrunWhite")
    e.getGuild.getUsers.forEach((u) => {
      if (Database.db.srcomlinks.contains(u.getStringID)) {
        RequestBuffer.request(() => u.addRole(l))
      }
    })
    true
  })

}
