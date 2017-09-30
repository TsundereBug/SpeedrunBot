require "discordcr"
require "srcr"

module SpeedrunBot
  class RunEmbed
    def self.from_id(id : String) : Discord::Embed
      run = SRcr::Run.from_id(id)
      game = run.game
      cat = run.category
      colour = SRcr.average_colors(run.players.map do |p|
        p.color
      end)

      Discord::Embed.new(
        author: Discord::EmbedAuthor.new(
          name: "#{run.players.map{ |p| p.display_name }.reduce { |acc, n| acc + ", " + n }}'s run of #{game.names.international}: #{cat.name}",
          url: run.weblink.to_s
        ),
        fields: [
          Discord::EmbedField.new(
            name: "Primary Time",
            value: run.times[SRcr::TimeType::Primary].to_s,
            inline: true
          ),
          if rta = run.times[SRcr::TimeType::Realtime]
            Discord::EmbedField.new(
              name: "Realtime",
              value: rta.to_s,
              inline: true
            )
          end,
          if rta_nl = run.times[SRcr::TimeType::RealtimeNoLoads]
            Discord::EmbedField.new(
              name: "Realtime, No Loads",
              value: rta_nl.to_s,
              inline: true
            )
          end,
          if ingame = run.times[SRcr::TimeType::Ingame]
            Discord::EmbedField.new(
              name: "Ingame Time",
              value: ingame.to_s,
              inline: true
            )
          end
        ].compact,
        footer: Discord::EmbedFooter.new(
          text:
            case run.status.status
            when "verified"
              "Verified" + if e = run.status.examiner
                " by #{e.names.international}"
              else
                ""
              end
            when "rejected"
              "Rejected" + if e = run.status.examiner
                " by #{e.names.international}"
              else
                ""
              end
            else
              "Submitted"
            end
        ),
        timestamp:
          case run.status.status
          when "verified"
            run.status.verify_date
          else
            run.date
          end,
        colour: colour.to_u32,
        description: if run.videos && run.videos.not_nil!.text
          run.videos.not_nil!.text
        end
      )
    end
  end
end
