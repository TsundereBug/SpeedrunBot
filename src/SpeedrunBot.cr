require "./SpeedrunBot/*"
require "discordcr"

module SpeedrunBot

  SRDB = SpeedrunBot::SRBotDatabase.connect

  CLIENT = Discord::Client.new(token: "Bot #{ENV["SRB_TOKEN"]}", client_id: 329084604986294272_u64)

  CLIENT.on_message_create do |m|
    if !m.author.bot
      begin
        current = SpeedrunBot::COMMAND_LIST
        if m.content.starts_with? current.@name
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
          if current.@check.call m
            current.@call.call m
          end
        end
      rescue ex
        CLIENT.create_message(m.channel_id, "Caught exception `#{ex.message}`:\n```crystal\n#{ex.backtrace.reduce{ |acc, i| "#{acc}\n#{i}"}}\n```\nPlease report this to `TsundereBug#0641`")
      end
    end
  end

  CLIENT.on_ready do |p|
    CLIENT.status_update(status: "online", game: Discord::GamePlaying.new("-s info", 0_i64))
  end

  CLIENT.run
end
