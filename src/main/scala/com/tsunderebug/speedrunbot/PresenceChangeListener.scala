package com.tsunderebug.speedrunbot

import com.tsunderebug.speedrunbot.data.{Database, TwitchIntegration}
import sx.blah.discord.api.events.IListener
import sx.blah.discord.handle.impl.events.user.PresenceUpdateEvent
import sx.blah.discord.handle.obj.IGuild

import scala.collection.JavaConverters._

object PresenceChangeListener extends IListener[PresenceUpdateEvent] {

  override def handle(e: PresenceUpdateEvent): Unit = {
    if (e.getNewPresence.getStreamingUrl.isPresent && !e.getOldPresence.getStreamingUrl.isPresent) {
      val url = e.getNewPresence.getStreamingUrl.get()
      val game = TwitchIntegration.game(url)
      for (i: IGuild <- e.getClient.getGuilds.asScala) {
        if (!Database.db.runnerRoles.contains(i.getStringID) || e.getUser.getRolesForGuild(i).asScala.map(_.getStringID).contains(Database.db.runnerRoles(i.getStringID))) {
          if (!Database.db.games.contains(i.getStringID) || Database.db.games(i.getStringID).contains(game)) {
            TwitchIntegration.showTwitchEmbed(e.getClient.getChannelByID(Database.db.streamChannels.getOrElse(i.getStringID, i.getGeneralChannel.getStringID).toLong), url)
          }
        }
      }
    }
  }

}
