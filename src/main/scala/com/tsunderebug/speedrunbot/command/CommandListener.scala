package com.tsunderebug.speedrunbot.command

import com.tsunderebug.speedrunbot.Tree
import com.tsunderebug.speedrunbot.data.TwitchIntegration
import sx.blah.discord.api.events.IListener
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

object CommandListener extends IListener[MessageReceivedEvent] {

  val commands: Tree[Command] = Tree(Command("-s", "", (_: MessageReceivedEvent) => false, (_: MessageReceivedEvent) => false), Seq(
    Tree(Ping, Seq(
      Tree(Ping.Pong, Seq())
    )),
    Tree(Help, Seq()),
    Tree(Command("twitch", "Checks info about a user's stream.",
      (_: MessageReceivedEvent) => true,
      (e: MessageReceivedEvent) => {
        val arg = e.getMessage.getContent.split("\\s+").drop(2).mkString(" ")
        TwitchIntegration.showTwitchEmbed(e.getChannel, arg)
      }
    ), Seq()),
    Tree(StreamAlert, Seq(
      Tree(StreamAlert.Channel, Seq()),
      Tree(StreamAlert.Role, Seq()),
      Tree(StreamAlert.Game, Seq())
    )),
    Tree(PersonalBest, Seq(
      Tree(PersonalBest.Add, Seq()),
      Tree(PersonalBest.Game, Seq())
    ))
  ))

  override def handle(e: MessageReceivedEvent): Unit = {
    if (e.getMessage.getContent.startsWith("-s ")) {
      val command: Command = findCommand(e.getMessage.getContent.split("\\s+").drop(1).mkString(" "))
      if (command.check.test(e)) {
        command.call.test(e)
      }
    }
  }

  def findCommand(n: String): Command = {
    val searchArr = n.split("\\s+")
    var end = false
    var level = 0
    var c: Tree[Command] = commands
    while (!end) {
      try {
        c.child(_.name == searchArr(level)) match {
          case None => end = true
          case Some(m) => c = m
        }
      } catch {
        case _: ArrayIndexOutOfBoundsException => end = true
      }
      level += 1
    }
    c.t
  }

}
