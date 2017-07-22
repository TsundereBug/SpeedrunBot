package com.tsunderebug.speedrunbot.command

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

object Help extends Command("help", (_: MessageReceivedEvent) => true, (e: MessageReceivedEvent) => {
  val c = CommandListener.findCommand(e.getMessage.getContent.split("\\s+").drop(2).mkString(" "))
  if (c.name != "-s") {
    e.getChannel.sendMessage(c.help)
  } else {
    e.getChannel.sendMessage(
      """**Speedrun Bot by TsundereBug**
        |
        |Speedrun Bot is a bot made specifically for doing things with the Speedrun.com API. Use `-s help [command]` to get some info on a command. E.g. `-s help stream game`.
        |
        |To invite me to your own server, click this link: https://discordapp.com/oauth2/authorize?client_id=329084604986294272&scope=bot&permissions=506850417
        |
        |Commands:
        |```""".stripMargin + "\n" + CommandListener.commands.treeString((c, l) => c.name + ("." * (16 - c.name.length)) + "[" + l + "] " + c.help) + "\n```")
  }
  c.name != "-s"
})
