require "discordcr"
require "srcr"

module SpeedrunBot
  class GameInfoEmbed
    def self.from_id(id : String) : Discord::Embed
      game = SRcr::Game.from_id(id)
      name = game.names.international
      categories = game.categories
      moderators = game.moderators
      Discord::Embed.new(
        author: Discord::EmbedAuthor.new(
          name: "#{name} (#{game.id})",
          icon_url: game.assets.icon.uri.to_s,
          url: game.weblink.to_s
        ),
        fields: [
          Discord::EmbedField.new(
            name: "Full Categories",
            value: "```\n#{categories.select{ |c| c.type == SRcr::CategoryType::Game }.map{ |c| "#{c.name} (#{c.id})" }.reduce{ |acc, i| "#{acc}\n#{i}"}}\n```",
            inline: false
          ),
          Discord::EmbedField.new(
            name: "Level Categories",
            value: if categories.select{ |c| c.type == SRcr::CategoryType::Level }.size > 0
              "```\n#{categories.select{ |c| c.type == SRcr::CategoryType::Level }.map{ |c| "#{c.name} (#{c.id})"}.reduce{ |acc, i| "#{acc}\n#{i}"}}\n```"
            else
              "None"
            end,
            inline: false
          ),
          Discord::EmbedField.new(
            name: "Super Moderators",
            value: "```\n#{moderators.select{ |k, v| v == SRcr::ModeratorType::SuperModerator }.map{ |k, v| "#{k.names.international} (#{k.id})" }.reduce{ |acc, i| "#{acc}\n#{i}"}}\n```",
            inline: true
          ),
          Discord::EmbedField.new(
            name: "Moderators",
            value: "```\n#{moderators.select{ |k, v| v == SRcr::ModeratorType::Moderator }.map{ |k, v| "#{k.names.international} (#{k.id})" }.reduce{ |acc, i| "#{acc}\n#{i}"}}\n```",
            inline: true
          )
        ],
        thumbnail: Discord::EmbedThumbnail.new(game.assets.cover_large.uri.to_s)
      )
    end
  end
end
