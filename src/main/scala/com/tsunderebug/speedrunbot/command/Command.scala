package com.tsunderebug.speedrunbot.command

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

case class Command(name: String, help: String, check: (MessageReceivedEvent) => Boolean, call: (MessageReceivedEvent) => Boolean) {

  def this(name: String, check: (MessageReceivedEvent) => Boolean, call: (MessageReceivedEvent) => Boolean) = {
    this(name, "Usage of this command is obvious.", check, call)
  }

}
