require "../commands.cr"
require "../gameembeds.cr"
require "../runembeds.cr"
require "discordcr"
require "srcr"

module SpeedrunBot
  module GameCommands
    extend self
    def game_list(query : String) : Array(SRcr::Game)
      games = SRcr::Game.search(query.gsub(" ", "_"))
      glselection = games.select { |g| g.names.international.downcase == query.downcase || g.id == query.downcase }
      if  glselection.size == 1
        [glselection[0]]
      else
        games
      end
    end
    def cat_list(game : SRcr::Game, query : String) : Array(SRcr::Category)
      categories = game.categories.select{ |c| c.type == SRcr::CategoryType::Game && c.name.downcase.index(query.downcase) }
      clselection = categories.select { |c| c.name.downcase == query.downcase }
      if  clselection.size == 1
        [clselection[0]]
      else
        categories
      end
    end
    GAME_LIST = Command.new(
      "game",
      "List games or show game info of a game",
      ->(m : Discord::Message) { true },
      ->(m : Discord::Message) {
        name = m.content.split(/\s+/)[2..-1].reduce { |acc, i| "#{acc} #{i}" }
        gamelist = SpeedrunBot::GameCommands.game_list(name)
        if gamelist.size == 1
          game = gamelist[0]
          SpeedrunBot::CLIENT.create_message(m.channel_id, "Info for **#{name}**", SpeedrunBot::GameInfoEmbed.from_id(game.id))
        elsif gamelist.size == 0
          SpeedrunBot::CLIENT.create_message(m.channel_id, "No games found for `#{name}`.")
        else
          SpeedrunBot::CLIENT.create_message(m.channel_id, "Found these games:\n```\n#{gamelist.map{ |g| "#{g.names.international} (#{g.id})" }.reduce{ |acc, i| acc + "\n" + i}}\n```")
        end
        true
      },
      [CATEGORY_WR]
    )
    GAMECAT_REG = /(?<game>[-\p{L}\d: _&()\[\]{}#]+) \|\| (?<cat>[-\p{L}\d: _&()\[\]{}#]+)/i
    CATEGORY_WR = Command.new(
      "wr",
      "Show the WR for a full-game category by ID or `Game || Category`",
      ->(m : Discord::Message) { true },
      ->(m : Discord::Message) {
        query = m.content.split(/\s+/)[3..-1].reduce { |acc, i| "#{acc} #{i}" }
        if GAMECAT_REG =~ query
          gamestr = GAMECAT_REG.match(query).try &.["game"]
          catstr = GAMECAT_REG.match(query).try &.["cat"]
          gamelist = SpeedrunBot::GameCommands.game_list(gamestr.not_nil!)
          if gamelist.size == 1
            game = gamelist[0]
            catlist = SpeedrunBot::GameCommands.cat_list(game, catstr.not_nil!)
            if catlist.size == 1
              cat = catlist[0]
              leaderboard = SRcr::Leaderboard.from_category(cat)
              leaderboard.runs.select{ |pr| pr.place == 1 }.each do |pr|
                SpeedrunBot::CLIENT.create_message(m.channel_id, "First place run of `#{cat.game.names.international} || #{cat.name}`", SpeedrunBot::RunEmbed.from_id(pr.run.id))
              end
            elsif catlist.size == 0
              SpeedrunBot::CLIENT.create_message(m.channel_id, "No categories named `#{catstr}` found for **#{game.names.international}**. Game info:", SpeedrunBot::GameInfoEmbed.from_id(game.id))
            else
              SpeedrunBot::CLIENT.create_message(m.channel_id, "Multiple categories found for `#{catstr}`:\n```\n#{catlist.map{ |c| "#{c.name} (#{c.id})"}.reduce{ |acc, i| "#{acc}\n#{i}" }}\n```")
            end
          elsif gamelist.size == 0
            SpeedrunBot::CLIENT.create_message(m.channel_id, "No games found for `#{name}`.")
          else
            SpeedrunBot::CLIENT.create_message(m.channel_id, "Found these games for #{gamestr}:\n```\n#{gamelist.map{ |g| "#{g.names.international} (#{g.id})" }.reduce{ |acc, i| acc + "\n" + i}}\n```")
          end
        else
          cat = SRcr::Category.from_id(query)
          leaderboard = SRcr::Leaderboard.from_category(cat)
          leaderboard.runs.select{ |pr| pr.place == 1 }.each do |pr|
            SpeedrunBot::CLIENT.create_message(m.channel_id, "First place run of `#{cat.game.names.international} || #{cat.name}`", SpeedrunBot::RunEmbed.from_id(pr.run.id))
          end
        end
        true
      }
    )
  end
end
