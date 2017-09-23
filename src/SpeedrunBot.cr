require "./SpeedrunBot/*"
require "discordcr"

module SpeedrunBot
  CLIENT = Discord::Client.new(token: "Bot #{ENV["SRB_TOKEN"]}", client_id: 329084604986294272_u64)

  CLIENT.on_message_create do |m|
    current = SpeedrunBot::COMMAND_LIST
    m.content[3..-1].split(/\s+/).each do |w|
      selected = current.@subs.select do |c|
        c.@name == w
      end
      if selected.size < 1
        break
      else
        current = selected[0]
      end
    end
    current.@call.call m
  end

  CLIENT.run
end
