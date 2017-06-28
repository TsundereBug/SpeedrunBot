package com.tsunderebug.speedrunbot

import java.util.Properties

import com.tsunderebug.speedrunbot.command.CommandListener
import sx.blah.discord.api.{ClientBuilder, IDiscordClient}

import scala.collection.JavaConverters._
import scala.collection.mutable

object Main {

  private val p: Properties = new Properties()
  p.load(getClass.getResourceAsStream("/config.properties"))
  val config: mutable.Map[String, String] = p.asScala
  val client: IDiscordClient = new ClientBuilder().withToken(config("token")).registerListener(CommandListener).registerListener(PresenceChangeListener).login()
  val globals: Array[Long] = config("globals").split(",").map(_.toLong)

  def main(args: Array[String]): Unit = {

  }

}
