require "discordcr"
require "./runembeds"
require "./commands/*"

module SpeedrunBot
  COMMAND_LIST = Command.new("-s", "Root prefix", ->(m : Discord::Message) { true }, ->(m : Discord::Message) { true }, [
      Command.new("ping", "Replies with `Pong!`", ->(m : Discord::Message) { true }, ->(m : Discord::Message) {
        before = Time.now
        message = SpeedrunBot::CLIENT.create_message(m.channel_id, "Pinging...")
        after = Time.now
        SpeedrunBot::CLIENT.edit_message(m.channel_id, message.id, "Pinged! __#{(after - before).total_milliseconds}ms__")
        true
      }),
      Command.new("info", "Show Speedrun Bot info", ->(m : Discord::Message) { true }, ->(m : Discord::Message) {
        SpeedrunBot::CLIENT.create_message(m.channel_id, "<@!#{m.author.id}>", Discord::Embed.new(
          title: "Speedrun Bot",
          description: "Speedrun Bot is a bot made to access the [Speedrun.com API](https://github.com/speedruncomorg/api) to show info on runs, games, users, etc.",
          fields: [
            Discord::EmbedField.new(
              name: "Language",
              value: "[Crystal](https://crystal-lang.org)",
              inline: true
            ),
            Discord::EmbedField.new(
              name: "Discord API Wrapper",
              value: "[discordcr](https://github.com/meew0/discordcr)",
              inline: true
            ),
            Discord::EmbedField.new(
              name: "Speedrun.com API Wrapper",
              value: "[srcr](https://github.com/TsundereBug/srcr)",
              inline: true
            ),
            Discord::EmbedField.new(
              name: "Source",
              value: "[SpeedrunBot:rewrite](https://github.com/TsundereBug/SpeedrunBot/tree/rewrite)",
              inline: true
            ),
            Discord::EmbedField.new(
              name: "Developer",
              value: "TsundereBug#0641 (<@!192322936219238400>)",
              inline: true
            ),
            Discord::EmbedField.new(
              name: "Found any bugs?",
              value: "Report them on [the issues page](https://github.com/TsundereBug/SpeedrunBot/issues).",
              inline: true
            )
          ],
          colour: 0xf0c03e_u32,
          image: Discord::EmbedImage.new("https://www.speedrun.com/themes/#{["Mint", "Night", "Bubbles", "SpeedRunsLive", "supermetroid", "hl", "animalcrossing", "user/Milk"].sample(1)[0]}/logo.png")
        ))
        true
      }),
      Command.new("run", "Gets info of a run by ID", ->(m : Discord::Message) { true }, ->(m : Discord::Message) {
        id = m.content.split(/\s+/)[2]
        SpeedrunBot::CLIENT.create_message(m.channel_id, "Run info for ID: `#{id}`", SpeedrunBot::RunEmbed.from_id(id))
        true
      }),
      SpeedrunBot::GameCommands::GAME_LIST
    ])
  class Command
    def initialize(@name : String, @help : String, @check : Proc(Discord::Message, Bool), @call : Proc(Discord::Message, Bool), @subs : Array(Command))
    end
    def initialize(@name : String, @help : String, @check : Proc(Discord::Message, Bool), @call : Proc(Discord::Message, Bool))
      initialize(@name, @help, @check, @call, [] of Command)
    end
    def initialize(@name : String, @check : Proc(Discord::Message, Bool), @call : Proc(Discord::Message, Bool))
      initialize(@name, "Help has not been provided for this command yet.", @check, @call)
    end
  end
end