package com.tsunderebug.speedrunbot

import java.util.Properties

import com.tsunderebug.speedrunbot.command.CommandListener
import sx.blah.discord.api.events.IListener
import sx.blah.discord.api.{ClientBuilder, IDiscordClient}
import sx.blah.discord.handle.impl.events.ReadyEvent

import scala.collection.JavaConverters._
import scala.collection.mutable

object Main {

  private val p: Properties = new Properties()
  p.load(getClass.getResourceAsStream("/config.properties"))
  val config: mutable.Map[String, String] = p.asScala
  val client: IDiscordClient = new ClientBuilder().setMaxReconnectAttempts(500).withToken(config("token")).registerListener(CommandListener).registerListener(PresenceChangeListener).registerListener(new IListener[ReadyEvent] {
    override def handle(e: ReadyEvent): Unit = {
      e.getClient.online("-s help")
      RunTimer.startTimer()
    }
  }).login()
  val globals: Array[Long] = config("globals").split(",").map(_.toLong)

  def main(args: Array[String]): Unit = {

  }

}
