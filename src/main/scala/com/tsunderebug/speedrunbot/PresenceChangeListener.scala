package com.tsunderebug.speedrunbot

import java.util.concurrent.TimeUnit

import com.tsunderebug.speedrunbot.data.{Database, TwitchIntegration}
import sx.blah.discord.api.events.IListener
import sx.blah.discord.handle.impl.events.user.PresenceUpdateEvent
import sx.blah.discord.handle.obj.{IGuild, IUser}

import scala.collection.JavaConverters._
import scala.collection.mutable

object PresenceChangeListener extends IListener[PresenceUpdateEvent] {

  val u = mutable.Map[IUser, Long]()

  override def handle(e: PresenceUpdateEvent): Unit = {
    if (e.getNewPresence.getStreamingUrl.isPresent && !e.getOldPresence.getStreamingUrl.isPresent) {
      if (!u.contains(e.getUser) || System.currentTimeMillis() - u(e.getUser) > TimeUnit.MINUTES.toMillis(5l)) {
        u += (e.getUser -> System.currentTimeMillis())
        val url = e.getNewPresence.getStreamingUrl.get()
        val game = TwitchIntegration.game(url)
        for (i: IGuild <- e.getClient.getGuilds.asScala) {
          if (i.getUsers.contains(e.getUser) && (!Database.db.runnerRoles.contains(i.getStringID) || e.getUser.getRolesForGuild(i).asScala.map(_.getStringID).contains(Database.db.runnerRoles(i.getStringID)))) {
            if (!Database.db.games.contains(i.getStringID) || Database.db.games(i.getStringID).isEmpty || Database.db.games(i.getStringID).exists(_.toLowerCase.contains(game.toLowerCase))) {
              TwitchIntegration.showTwitchEmbed(e.getClient.getChannelByID(Database.db.streamChannels.getOrElse(i.getStringID, i.getGeneralChannel.getStringID).toLong), url)
            }
          }
        }
      }
    }
  }

}
