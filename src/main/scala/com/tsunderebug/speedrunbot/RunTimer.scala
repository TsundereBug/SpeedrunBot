package com.tsunderebug.speedrunbot

import com.tsunderebug.speedrun4j.game.run.RunList
import com.tsunderebug.speedrun4j.game.{Category, Game, Leaderboard}
import com.tsunderebug.speedrunbot.command.Runs
import com.tsunderebug.speedrunbot.data.Database
import sx.blah.discord.handle.obj.{IGuild, IMessage}
import sx.blah.discord.util.RequestBuffer

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object RunTimer {

  private var stop = false
  private var timer: Thread = _
  private var lastTimes: mutable.Map[String, Long] = mutable.Map()

  def startTimer(): Unit = {
    stop = false
    timer = new Thread(() => {
      while (!stop) {
        Database.db.gameUpdates.foreach((t) => {
          val game: Game = Game.fromID(t._1)
          val guilds: Set[IGuild] = t._2.map((s) => Main.client.getGuildByID(s.toLong))
          val cats: Array[Category] = game.getCategories.getCategories.filter(_.getType != "per-level")
          cats.foreach((c) => {
            if (!lastTimes.contains(c.getId)) {
              lastTimes += (c.getId -> System.currentTimeMillis())
            } else {
              val t = System.currentTimeMillis()
              Leaderboard.forCategory(c).getRuns.filter((r) => (try {
                r.getRun.getStatus.getVerifyDate
              } catch {
                case _: NullPointerException => null
              }) != null && r.getRun.getStatus.getVerifyDate.getTime >= lastTimes(c.getId)).sortBy(_.getPlace).reverse.foreach((r) => {
                guilds.foreach((g) => {
                  val ch = Main.client.getChannelByID(Database.db.runUpdatesChannel.getOrElse(g.getStringID, g.getGeneralChannel.getStringID).toLong)
                  RequestBuffer.request(() => ch.sendMessage("New " + Runs.place(r.getPlace) + " place run of " + game.getNames.get("international") + ": " + c.getName + " in " + FormatUtil.msToTime((r.getRun.getTimes.getPrimaryT * 1000).toLong), Runs.runInfoEmbed(r))).get()
                })
              })
              lastTimes += (c.getId -> t)
              val td: ListBuffer[IMessage] = ListBuffer()
              val dd: ListBuffer[IMessage] = ListBuffer()
              val nr = RunList.forStatus(c, "new").getRuns
              if (nr.isEmpty) {
                guilds.foreach((g) => {
                  if (Database.db.modVerificationChannel.contains(g.getStringID)) {
                    val ch = Main.client.getChannelByID(Database.db.modVerificationChannel(g.getStringID).toLong)
                    import scala.collection.JavaConverters._
                    val m = ch.getFullMessageHistory.asScala
                    td ++= m
                  }
                })
              } else {
                nr.foreach((r) => {
                  guilds.foreach((g) => {
                    if (Database.db.modVerificationChannel.contains(g.getStringID)) {
                      val ch = Main.client.getChannelByID(Database.db.modVerificationChannel(g.getStringID).toLong)
                      import scala.collection.JavaConverters._
                      val m = ch.getFullMessageHistory.asScala
                      td ++= m
                      if (m.exists(_.getAuthor != Main.client.getOurUser)) {
                        Database.db = Database(Database.db.streamChannels, Database.db.runnerRoles, Database.db.games, Database.db.srcomlinks, Database.db.gameUpdates, Database.db.runUpdatesChannel, Database.db.modVerificationChannel - g.getStringID)
                        ch.sendMessage("Due to this channel being non-empty, run verification updates have been disabled.")
                      } else {
                        m.find(_.getContent.startsWith(r.getId)) match {
                          case Some(dm) => dd += dm
                          case None => dd += ch.sendMessage(r.getId + " Run waiting in " + c.getGame.getNames.get("international") + ": " + c.getName, Runs.runInfoEmbed(r))
                        }
                      }
                    }
                  })
                })
              }
              td --= dd
              td.foreach((m) => RequestBuffer.request(() => m.delete()).get())
            }
          })
        })
      }
    })
    timer.start()
  }

  def stopTimer(): Unit = {
    stop = true
    if (timer != null) timer.join()
    lastTimes = mutable.Map()
  }

}
