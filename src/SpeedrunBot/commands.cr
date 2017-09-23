require "discordcr"
require "./runembeds"

module SpeedrunBot
  COMMAND_LIST = Command.new("-s", "Root prefix", ->(m : Discord::Message) { true }, ->(m : Discord::Message) { true }, [
      Command.new("ping", "Replies with `Pong!`", ->(m : Discord::Message) { true }, ->(m : Discord::Message) {
        SpeedrunBot::CLIENT.create_message(m.channel_id, "Pong!")
        true
      }),
      Command.new("run", "Gets info of a run by ID", ->(m : Discord::Message) { true }, ->(m : Discord::Message) {
        id = m.content.split(/\s+/)[2]
        SpeedrunBot::CLIENT.create_message(m.channel_id, "Run info for ID: `#{id}`", SpeedrunBot::RunEmbed.from_id(id))
        true
      })
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
