package com.tsunderebug.speedrunbot.command

import java.awt.Color
import java.io.IOException

import com.tsunderebug.speedrun4j.game.run.PlacedRun
import com.tsunderebug.speedrun4j.user.User
import com.tsunderebug.speedrunbot.data.Database
import com.tsunderebug.speedrunbot.{FormatUtil, PermissionChecks}
import sx.blah.discord.api.internal.json.objects.EmbedObject
import sx.blah.discord.handle.obj.{IGuild, IRole}
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

  val redc = Color.RED
  val coralc = new Color(0xff5544)
  val orangec = Color.ORANGE
  val yellowc = Color.YELLOW
  val greenc = Color.GREEN
  val mintc = new Color(0x008000)
  val azurec = new Color(0x4455ff)
  val bluec = Color.BLUE
  val purplec = new Color(0x600090)
  val lavenderc = new Color(0xaa66ff)
  val fuchsiac = Color.MAGENTA
  val pinkc = Color.PINK
  val silverc = Color.GRAY
  val whitec = Color.WHITE
  val colors = Seq(redc, coralc, orangec, yellowc, greenc, mintc, azurec, bluec, purplec, lavenderc, fuchsiac, pinkc, silverc, whitec)

  object Role extends Command("role", "Creates colored roles for SRCom users.", PermissionChecks.manageServer, (e) => {
    val g = e.getGuild
    val l = setupRole(g, "Linked to Speedrun.com", Color.BLACK)
    val red = setupRole(g, "SpeedrunRed", redc)
    val coral = setupRole(g, "SpeedrunCoral", coralc)
    val orange = setupRole(g, "SpeedrunOrange", orangec)
    val yellow = setupRole(g, "SpeedrunYellow", yellowc)
    val green = setupRole(g, "SpeedrunGreen", greenc)
    val mint = setupRole(g, "SpeedrunMint", mintc)
    val azure = setupRole(g, "SpeedrunAzure", azurec)
    val blue = setupRole(g, "SpeedrunBlue", bluec)
    val purple = setupRole(g, "SpeedrunPurple", purplec)
    val lavender = setupRole(g, "SpeedrunLavender", lavenderc)
    val fuchsia = setupRole(g, "SpeedrunFuchsia", fuchsiac)
    val pink = setupRole(g, "SpeedrunPink", pinkc)
    val silver = setupRole(g, "SpeedrunSilver", silverc)
    val white = setupRole(g, "SpeedrunWhite", whitec)
    e.getGuild.getUsers.forEach((u) => {
      if (Database.db.srcomlinks.contains(u.getStringID)) {
        RequestBuffer.request(() => u.addRole(l))
        val uc = User.fromID(Database.db.srcomlinks(u.getStringID)).getNameStyle.getColor
        colors.minBy((c) => Math.sqrt(Math.pow(Math.abs(c.getRed - uc.getRed), 2) + Math.pow(Math.abs(c.getGreen - uc.getGreen), 2) + Math.pow(Math.abs(c.getBlue - uc.getBlue), 2))) match {
          case `redc` => RequestBuffer.request(() => u.addRole(red)).get()
          case `coralc` => RequestBuffer.request(() => u.addRole(coral)).get()
          case `orangec` => RequestBuffer.request(() => u.addRole(orange)).get()
          case `yellowc` => RequestBuffer.request(() => u.addRole(yellow)).get()
          case `greenc` => RequestBuffer.request(() => u.addRole(green)).get()
          case `mintc` => RequestBuffer.request(() => u.addRole(mint)).get()
          case `azurec` => RequestBuffer.request(() => u.addRole(azure)).get()
          case `bluec` => RequestBuffer.request(() => u.addRole(blue)).get()
          case `purplec` => RequestBuffer.request(() => u.addRole(purple)).get()
          case `lavenderc` => RequestBuffer.request(() => u.addRole(lavender)).get()
          case `fuchsiac` => RequestBuffer.request(() => u.addRole(fuchsia)).get()
          case `pinkc` => RequestBuffer.request(() => u.addRole(pink)).get()
          case `silverc` => RequestBuffer.request(() => u.addRole(silver)).get()
          case `whitec` => RequestBuffer.request(() => u.addRole(white)).get()
        }
      }
    })
    true
  })

}
