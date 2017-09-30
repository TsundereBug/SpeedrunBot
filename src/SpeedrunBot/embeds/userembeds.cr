require "discordcr"
require "srcr"

module SpeedrunBot
  class UserInfoEmbed
    def self.from_id(id : String) : Discord::Embed
      user = SRcr::User.from_id(id)
      Discord::Embed.new(
        author: Discord::EmbedAuthor.new(
          name: "#{user.names.international} (#{user.id})",
          url: user.weblink.to_s
        ),
        colour: user.name_style.color.dark.to_u32,
        footer: if s = user.signup
          Discord::EmbedFooter.new(
            text: "Signed up on"
          )
        end,
        timestamp: s
      )
    end
  end
end
