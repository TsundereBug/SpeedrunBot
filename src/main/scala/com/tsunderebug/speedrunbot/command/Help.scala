package com.tsunderebug.speedrunbot.command

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

object Help extends Command("help", (_: MessageReceivedEvent) => true, (e: MessageReceivedEvent) => {
  val c = CommandListener.findCommand(e.getMessage.getContent.split("\\s+").drop(2).mkString(" "))
  if (c.name != "-s") {
    e.getChannel.sendMessage(c.help)
  } else {
    e.getChannel.sendMessage("That's not a valid command!")
  }
  c.name != "-s"
})
