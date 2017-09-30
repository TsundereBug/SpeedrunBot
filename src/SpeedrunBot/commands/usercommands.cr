require "../commands.cr"
require "../embeds/runembeds.cr"
require "../embeds/userembeds.cr"
require "discordcr"
require "srcr"

module SpeedrunBot
  module UserCommands
    extend self
    def user_list(query : String) : Array(SRcr::User)
      users = SRcr::User.search(query)
      ulselection = users.select { |u| u.names.international.downcase == query.downcase || u.id == query.downcase }
      if  ulselection.size == 1
        [ulselection[0]]
      else
        users
      end
    end
    USER_LIST = Command.new(
      "user",
      "List users or show user info",
      ->(m : Discord::Message) { true },
      ->(m : Discord::Message) {
        userstr = m.content.split(/\s+/)[2]
        userlist = user_list(userstr)
        if userlist.size == 1
          user = userlist[0]
          SpeedrunBot::CLIENT.create_message(m.channel_id, "Info for user `#{user.names.international}`", SpeedrunBot::UserInfoEmbed.from_id(user.id))
        elsif userlist.size == 0
          SpeedrunBot::CLIENT.create_message(m.channel_id, "No users found for `#{userstr}`.")
        else
          SpeedrunBot::CLIENT.create_message(m.channel_id, "Found these users:\n```\n#{userlist.map{ |u| "#{u.names.international} (#{u.id})"}.reduce{ |acc, i| "#{acc}\n#{i}"}}\n```")
        end
        true
      }
    )
  end
end
