package com.tsunderebug.speedrunbot.command

import com.tsunderebug.speedrunbot.PermissionChecks
import com.tsunderebug.speedrunbot.data.Database
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.EmbedBuilder

object StreamAlert extends Command("stream", "Use subcommands to configure alert settings.",
  (_: MessageReceivedEvent) => true,
  (_: MessageReceivedEvent) => {
    true // TODO pick a random stream following role and channel
  }
) {

  object Channel extends Command("channel", "Send alerts to a specific channel.", PermissionChecks.manageServer,
    (e: MessageReceivedEvent) => {
      if (e.getMessage.getChannelMentions.isEmpty) {
        e.getChannel.sendMessage("Stream alerts are sent to " + e.getClient.getChannelByID(Database.db.streamChannels(e.getGuild.getStringID).toLong).mention())
      } else {
        Database.setStreamChannel(e.getGuild, e.getMessage.getChannelMentions.get(0))
        e.getChannel.sendMessage("Updated stream alert channel to " + e.getMessage.getChannelMentions.get(0))
      }
      true
    }
  )

  object Role extends Command("role", "Filter alerts to a specific role.", PermissionChecks.manageServer,
    (e: MessageReceivedEvent) => {
      if (e.getMessage.getContent.split("\\s+").length <= 3) {
        val r = e.getClient.getRoleByID(Database.db.runnerRoles(e.getGuild.getStringID).toLong)
        val eb = new EmbedBuilder
        eb.appendDesc("<@&" + r.getLongID + ">")
        e.getChannel.sendMessage("Stream alerts are only shown for", eb.build())
      } else {
        val r = e.getGuild.getRolesByName(e.getMessage.getContent.split("\\s+").drop(3).mkString(" ")).get(0)
        Database.setStreamRole(e.getGuild, r)
        val eb = new EmbedBuilder
        eb.appendDesc("<@&" + r.getLongID + ">")
        e.getChannel.sendMessage("Updated stream alert filter role to", eb.build())
      }
      true
    }
  )

  object Game extends Command("game", "Add or remove a game filter to alerts.", PermissionChecks.manageServer,
    (e: MessageReceivedEvent) => {
      val game = e.getMessage.getContent.split("\\s+").drop(3).mkString(" ")
      if (e.getMessage.getContent.split("\\s+").length <= 3) {
        e.getChannel.sendMessage("You have whitelisted " + (if (!Database.db.games.contains(e.getGuild.getStringID) || Database.db.games(e.getGuild.getStringID).isEmpty) "all games." else Database.db.games(e.getGuild.getStringID).mkString(", ")))
      } else {
        if (!Database.db.games.contains(e.getGuild.getStringID)) {
          Database.addGame(e.getGuild, game)
          Database.db = Database(Database.db.streamChannels, Database.db.runnerRoles, Database.db.games + (e.getGuild.getStringID -> Database.db.games(e.getGuild.getStringID).filter(game.!=)), Database.db.srcomlinks)
        }
        if (Database.db.games(e.getGuild.getStringID).contains(game)) {
          Database.db = Database(Database.db.streamChannels, Database.db.runnerRoles, Database.db.games + (e.getGuild.getStringID -> Database.db.games(e.getGuild.getStringID).filter(game.!=)), Database.db.srcomlinks)
          e.getChannel.sendMessage("Removed " + game + " from the whitelist.")
        } else {
          Database.addGame(e.getGuild, game)
          e.getChannel.sendMessage("Added " + game + " to the whitelist.")
        }
      }
      true
    }
  )

}
