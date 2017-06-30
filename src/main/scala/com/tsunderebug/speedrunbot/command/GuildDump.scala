package com.tsunderebug.speedrunbot.command

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.tsunderebug.speedrunbot.PermissionChecks
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.{IGuild, Permissions}
import sx.blah.discord.util.MissingPermissionsException

import scala.collection.JavaConverters._

object GuildDump extends Command("dumpguilds", "Only usable by owner", PermissionChecks.isGlobal, call = (e: MessageReceivedEvent) => {
  val os = new ByteArrayOutputStream()
  e.getClient.getGuilds.forEach((g: IGuild) => {
    val o = g.getChannels.asScala.find(_.getModifiedPermissions(e.getClient.getOurUser).contains(Permissions.CREATE_INVITE))
    val s = g.getName + "\t" + (if (o.isDefined) {
      o.get.createInvite(0, 0, false, false).getCode + "\n"
    } else {
      try {
        g.getExtendedInvites.asScala.map(_.getCode).mkString(",")
      } catch {
        case _: MissingPermissionsException => "noperms"
      }
    })
    os.write(s.getBytes)
  })
  val i = new ByteArrayInputStream(os.toByteArray)
  e.getChannel.sendFile("", i, "dump.txt")
  true
}) {

}
