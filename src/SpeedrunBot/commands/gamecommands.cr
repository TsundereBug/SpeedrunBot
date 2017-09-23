require "../commands.cr"
require "../gameembeds.cr"
require "discordcr"
require "srcr"

module SpeedrunBot
  module GameCommands
    GAME_LIST = Command.new(
      "game",
      "List games or show game info of a game",
      ->(m : Discord::Message) { true },
      ->(m : Discord::Message) {
        name = m.content.split(/\s+/)[2..-1].reduce { |acc, i| "#{acc}_#{i}" }
        gamelist = SRcr::Game.search(name)
        glselection = gamelist.select { |g| g.names.international.downcase == name.downcase.gsub("_", " ") || g.id == name.downcase }
        if gamelist.size == 1 || glselection.size == 1
          game = if glselection.size == 1
            glselection[0]
          else
            gamelist[0]
          end
          SpeedrunBot::CLIENT.create_message(m.channel_id, "Info for **#{name}**", SpeedrunBot::GameInfoEmbed.from_id(game.id))
        elsif gamelist.size == 0
          SpeedrunBot::CLIENT.create_message(m.channel_id, "No games found for `#{name}`.")
        else
          SpeedrunBot::CLIENT.create_message(m.channel_id, "Found these games:\n```\n#{gamelist.map{ |g| "#{g.names.international} (#{g.id})" }.reduce{ |acc, i| acc + "\n" + i}}\n```")
        end
        true
      }
    )
  end
end
