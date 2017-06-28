package com.tsunderebug.speedrunbot.command

import java.util.function.Predicate

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

case class Command(name: String, help: String, check: Predicate[MessageReceivedEvent], call: Predicate[MessageReceivedEvent]) {

  def this(name: String, check: Predicate[MessageReceivedEvent], call: Predicate[MessageReceivedEvent]) = {
    this(name, "Usage of this command is obvious.", check, call)
  }

}
