package com.tsunderebug.speedrunbot.data

import java.io.{File, FileWriter}

import net.liftweb.json
import sx.blah.discord.handle.obj.{IChannel, IGuild, IRole, IUser}

import scala.io.Source

object Database {

  val file: File = new File(System.getProperty("user.home"), "speedrunbot.json")
  if (!file.exists()) {
    file.createNewFile()
    val fw = new FileWriter(file)
    fw.write(json.Serialization.write[Database](Database(Map(), Map(), Map(), Map(), Map(), Map(), Map()))(json.DefaultFormats))
    fw.close()
  }
  private var _db: Database = json.Serialization.read[Database](Source.fromFile(file).mkString)(json.DefaultFormats, manifest)

  def db: Database = _db

  def db_=(db: Database): Unit = {
    this.synchronized {
      val fw = new FileWriter(file)
      fw.write(json.Serialization.write[Database](db)(json.DefaultFormats))
      fw.close()
      _db = db
    }
  }

  def setStreamChannel(g: IGuild, c: IChannel): Unit = {
    db = Database(db.streamChannels + (g.getStringID -> c.getStringID), db.runnerRoles, db.games, db.srcomlinks, db.gameUpdates, db.runUpdatesChannel, db.modVerificationChannel)
  }

  def setStreamRole(g: IGuild, r: IRole): Unit = {
    db = Database(db.streamChannels, db.runnerRoles + (g.getStringID -> r.getStringID), db.games, db.srcomlinks, db.gameUpdates, db.runUpdatesChannel, db.modVerificationChannel)
  }

  def addGame(g: IGuild, game: String): Unit = {
    db = Database(db.streamChannels, db.runnerRoles, db.games + (g.getStringID -> (db.games.getOrElse(g.getStringID, Set()) + game)), db.srcomlinks, db.gameUpdates, db.runUpdatesChannel, db.modVerificationChannel)
  }

  def linkSrCoom(u: IUser, key: String): Unit = {
    db = Database(db.streamChannels, db.runnerRoles, db.games, db.srcomlinks + (u.getStringID -> key), db.gameUpdates, db.runUpdatesChannel, db.modVerificationChannel)
  }

  def toggleGame(game: String, g: IGuild): Unit = {
    db = Database(db.streamChannels, db.runnerRoles, db.games, db.srcomlinks, db.gameUpdates + (game -> (if (db.gameUpdates.getOrElse(game, Set()).contains(g.getStringID)) {
      db.gameUpdates(game) - g.getStringID
    } else {
      if (db.gameUpdates.contains("game")) db.gameUpdates(game) + g.getStringID else Set(g.getStringID)
    })), db.runUpdatesChannel, db.modVerificationChannel)
  }

  def setRunUpdatesChannel(g: IGuild, c: IChannel): Unit = {
    db = Database(db.streamChannels, db.runnerRoles, db.games, db.srcomlinks, db.gameUpdates, db.runUpdatesChannel + (g.getStringID -> c.getStringID), db.modVerificationChannel)
  }

  def setModVerificationChannel(g: IGuild, c: IChannel): Unit = {
    db = Database(db.streamChannels, db.runnerRoles, db.games, db.srcomlinks, db.gameUpdates, db.runUpdatesChannel, db.modVerificationChannel + (g.getStringID -> c.getStringID))
  }

}

case class Database(streamChannels: Map[String, String] = Map(), runnerRoles: Map[String, String] = Map(), games: Map[String, Set[String]] = Map(), srcomlinks: Map[String, String] = Map(), gameUpdates: Map[String, Set[String]] = Map(), runUpdatesChannel: Map[String, String] = Map(), modVerificationChannel: Map[String, String] = Map())
