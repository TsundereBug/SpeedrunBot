package com.tsunderebug.speedrunbot.command

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

object Ping extends Command("ping",
  (_: MessageReceivedEvent) => true,
  (e: MessageReceivedEvent) => {
    try {
      val m = e.getChannel.sendMessage("Gateway Response: **" + e.getMessage.getShard.getResponseTime + "ms**")
      true
    } catch {
      case _: Exception => false
    }
  }
) {

  object Pong extends Command("pong", "`ping` but with message send latency.",
    (_: MessageReceivedEvent) => true,
    (e: MessageReceivedEvent) => {
      try {
        val ping = Ping.call.test(e)
        val b = System.currentTimeMillis()
        val m = e.getChannel.sendMessage(":ping_pong:")
        val a = System.currentTimeMillis()
        m.edit(":ping_pong: Message bounced in **" + (a - b) + "ms**")
        ping
      } catch {
        case _: Exception => false
      }
    }
  )

}