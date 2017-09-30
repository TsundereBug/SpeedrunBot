require "discordcr"
require "./embeds/runembeds"
require "./commands/*"
require "./database.cr"

module SpeedrunBot
  COMMAND_LIST = Command.new("-s", "Root prefix", ->(m : Discord::Message) { true }, ->(m : Discord::Message) { true }, [
      Command.new("ping", "Replies with `Pong!`", ->(m : Discord::Message) { true }, ->(m : Discord::Message) {
        before = Time.now
        message = SpeedrunBot::CLIENT.create_message(m.channel_id, "Pinging...")
        after = Time.now
        SpeedrunBot::CLIENT.edit_message(m.channel_id, message.id, "Pinged! __#{(after - before).total_milliseconds}ms__")
        true
      }),
      Command.new("speed", "Show speed", ->(m : Discord::Message) { true }, ->(m : Discord::Message) {
        SpeedrunBot::CLIENT.create_message(m.channel_id, "You have #{SpeedrunBot::SRDB.coins(m.author.id.not_nil!)}")
        true
      }, [
        Command.new("run", "Do a run for speed (every 4 hours)", ->(m : Discord::Message) { true }, ->(m : Discord::Message) {
          current = Time.now
          last = SpeedrunBot::SRDB.lastspeed(m.author.id.not_nil!)
          if(current - last < 4.hours)
            SpeedrunBot::CLIENT.create_message(m.channel_id, "You're a bit exhausted from your last run. Try again in `#{4.hours - (current - last)}`.")
          else
            begin
              current_coin = SpeedrunBot::SRDB.coins(m.author.id).not_nil!
              bet = m.content.split(/\s+/)[3].to_f64
              if(current_coin < bet || bet <= 0 || bet > 50)
                raise ArgumentError.new
              end
              rng = Random.rand(125) - (bet / 2)
              case
              when rng >= 99
                payout = bet * 4
                message = "📕 **YOOO that RNG!** You got World Record!"
              when rng < 99 && rng >= 77
                payout = bet * 3
                message = "📗 Pretty good! You have a new personal best."
              when rng < 77 && rng >= 45
                payout = bet * 2
                message = "📙 It was an ok run; your Sum of Best went down."
              when rng < 45 && rng >= 0
                payout = bet
                message = "📘 You lost the run to RNG, dude."
              else
                payout = bet / 2
                message = "You accidentally hurt yourself because of stress. You'll want to rest for a few hours."
              end
              net = payout - bet
              SpeedrunBot::SRDB.set_lastspeed(m.author.id, current)
              SpeedrunBot::SRDB.set_coins(m.author.id, current_coin + net)
              SpeedrunBot::CLIENT.create_message(m.channel_id, "#{message} Net gain: **#{net} speed**")
            rescue ex : ArgumentError
              if current_coin.not_nil! > 50
                current_coin = 50
              end
              SpeedrunBot::CLIENT.create_message(m.channel_id, "Please enter a valid amount to bet. This can be any number less than #{current_coin} and greater than 0, e.g. #{current_coin.not_nil! - (Random.rand * current_coin.not_nil!)}.")
            end
          end
          true
        }),
        Command.new("reset", "Reset coin", ->(m : Discord::Message) { m.author.id == 192322936219238400_u64 }, ->(m : Discord::Message) {
          SpeedrunBot::SRDB.@db.exec("DROP TABLE coin;")
          SpeedrunBot::SRDB.@db.exec("DROP TABLE lastspeed;")
          SpeedrunBot::SRDB.@db.exec("
          CREATE TABLE coin (
            id BIGINT,
            value NUMERIC
          );")
          SpeedrunBot::SRDB.@db.exec("
          CREATE TABLE lastspeed (
            id BIGINT,
            timeztamp TIMESTAMPTZ
          );")
          true
        })
      ]),
      Command.new("info", "Show Speedrun Bot info", ->(m : Discord::Message) { true }, ->(m : Discord::Message) {
        themes = [
          {0xc0c0c0_u32, "default"},
          {0x00c789_u32, "Mint"},
          {0xc4c2fa_u32, "Night"},
          {0xff36a6_u32, "Bubbles"},
          {0xd2683a_u32, "SpeedRunsLive"},
          {0xff5c3d_u32, "supermetroid"},
          {0xbab7ba_u32, "hl"},
          {0xf0ba4f_u32, "animalcrossing"},
          {0xff878f_u32, "user/Milk"}
        ]
        theme = themes.sample(1)[0]
        SpeedrunBot::CLIENT.create_message(m.channel_id, "<@!#{m.author.id}>", Discord::Embed.new(
          title: "Speedrun Bot",
          description: "Speedrun Bot is a bot made to access the [Speedrun.com API](https://github.com/speedruncomorg/api) to show info on runs, games, users, etc.",
          fields: [
            Discord::EmbedField.new(
              name: "Invite me",
              value: "Only users with the `MANAGE_SERVER` permission can add bots. If you have this permission on a server, use [this fancy discord link](https://discordapp.com/oauth2/authorize?client_id=329084604986294272&scope=bot&permissions=1714944193) to invite me.",
              inline: true
            ),
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
          colour: theme[0],
          image: Discord::EmbedImage.new("https://www.speedrun.com/themes/#{theme[1]}/logo.png")
        ))
        true
      }),
      Command.new("run", "Gets info of a run by ID", ->(m : Discord::Message) { true }, ->(m : Discord::Message) {
        id = m.content.split(/\s+/)[2]
        SpeedrunBot::CLIENT.create_message(m.channel_id, "Run info for ID: `#{id}`", SpeedrunBot::RunEmbed.from_id(id))
        true
      }),
      SpeedrunBot::GameCommands::GAME_LIST,
      SpeedrunBot::UserCommands::USER_LIST
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
