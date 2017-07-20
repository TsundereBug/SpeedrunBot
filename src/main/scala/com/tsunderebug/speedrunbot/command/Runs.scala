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

import scala.collection.JavaConverters._
import scala.collection.SortedMap

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
    true
  })

}
