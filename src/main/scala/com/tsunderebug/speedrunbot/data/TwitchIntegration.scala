package com.tsunderebug.speedrunbot.data

import com.tsunderebug.speedrunbot.Main
import com.urgrue.twitch.api.handlers.{ChannelResponseHandler, StreamResponseHandler}
import com.urgrue.twitch.api.models.Channel
import com.urgrue.twitch.api.{TwitchApiClient, models}
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.util.{EmbedBuilder, MessageBuilder}

object TwitchIntegration {

  val twitch = new TwitchApiClient()
  twitch.setClientId(Main.config("twitchkey"))

  def game(url: String): String = {
    channel(url) match {
      case Some(c) => c.getGame
      case None => stream(url) match {
        case Some(s) => s.getGame
        case None => "Unknown"
      }
    }
  }

  def channel(url: String): Option[Channel] = {
    val name = url.drop(url.lastIndexOf("/"))
    var c: Option[Channel] = null
    twitch.channels().get(name, new ChannelResponseHandler {

      override def onSuccess(channel: Channel): Unit = c = Some(channel)

      override def onFailure(i: Int, s: String, s1: String): Unit = c = None

      override def onFailure(throwable: Throwable): Unit = c = None

    })
    while (c == null) {
      Thread.sleep(10)
    }
    c
  }

  def stream(url: String): Option[models.Stream] = {
    val name = url.drop(url.lastIndexOf("/"))
    var s: Option[models.Stream] = null
    twitch.streams().get(name, new StreamResponseHandler {

      override def onSuccess(stream: models.Stream): Unit = {
        s = Option(stream)
      }

      override def onFailure(i: Int, s0: String, s1: String): Unit = {
        s = None
      }

      override def onFailure(throwable: Throwable): Unit = {
        s = None
      }

    })
    while (s == null) {
      Thread.sleep(10)
    }
    s
  }

  def showTwitchEmbed(c: IChannel, _url: String): Boolean = {
    val url = _url.drop(_url.lastIndexOf("/") + 1)
    try {
      val tso = stream(url)
      val eb: EmbedBuilder = new EmbedBuilder
      val mb: MessageBuilder = new MessageBuilder(Main.client)
      mb.withChannel(c)
      tso match {
        case Some(ts) =>
          if (ts.isOnline) {
            mb.withContent(ts.getChannel.getDisplayName + " is **online** playing **" + ts.getGame + "**!")
            eb.withAuthorName(ts.getChannel.getStatus)
            eb.withImage(ts.getPreview.getLarge)
            eb.withColor(0x6441a5)
            eb.appendField("Views", ts.getChannel.getViews.toString, true)
            eb.appendField("Followers", ts.getChannel.getFollowers.toString, true)
            eb.appendField("Watching", ts.getViewers.toString, true)
          } else {
            mb.withContent(ts.getChannel.getDisplayName + " is **offline.**")
            eb.withAuthorName(ts.getChannel.getDisplayName)
            eb.withDescription("Last stream was **" + ts.getChannel.getStatus + "** playing **" + ts.getGame + "**.")
            eb.withColor(0x7f7f7f)
            eb.appendField("Views", ts.getChannel.getViews.toString, true)
            eb.appendField("Followers", ts.getChannel.getFollowers.toString, true)
          }
          eb.withAuthorIcon(ts.getChannel.getLogo)
          eb.withAuthorUrl(ts.getChannel.getUrl)
        case None =>
          val tco = channel(url)
          tco match {
            case Some(tc) =>
              mb.withContent(tc.getDisplayName + " is **offline.**")
              eb.withAuthorIcon(tc.getLogo)
              eb.withAuthorName(tc.getDisplayName)
              eb.withDescription("Last stream was **" + tc.getStatus + "** playing **" + tc.getGame + "**.")
              eb.withAuthorUrl(tc.getUrl)
              eb.withColor(0x7f7f7f)
              eb.appendField("Views", tc.getViews.toString, true)
              eb.appendField("Followers", tc.getFollowers.toString, true)
            case None =>
              c.sendMessage("That channel doesn't exist!")
              return true
          }
      }
      mb.withEmbed(eb.build())
      mb.send()
      true
    } catch {
      case e: Exception => e.printStackTrace(); false
    }
  }

}
