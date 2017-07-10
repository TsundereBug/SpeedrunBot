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
    fw.write(json.Serialization.write[Database](Database(Map(), Map(), Map(), Map()))(json.DefaultFormats))
    fw.close()
  }
  private var _db: Database = json.Serialization.read[Database](Source.fromFile(file).mkString)(json.DefaultFormats, manifest)

  def db: Database = _db

  def db_=(db: Database): Unit = {
    val fw = new FileWriter(file)
    fw.write(json.Serialization.write[Database](db)(json.DefaultFormats))
    fw.close()
    _db = db
  }

  def setStreamChannel(g: IGuild, c: IChannel): Unit = {
    db = Database(db.streamChannels + (g.getStringID -> c.getStringID), db.runnerRoles, db.games, db.srcomlinks)
  }

  def setStreamRole(g: IGuild, r: IRole): Unit = {
    db = Database(db.streamChannels, db.runnerRoles + (g.getStringID -> r.getStringID), db.games, db.srcomlinks)
  }

  def addGame(g: IGuild, game: String): Unit = {
    db = Database(db.streamChannels, db.runnerRoles, db.games + (g.getStringID -> (db.games.getOrElse(g.getStringID, Array()) :+ game)), db.srcomlinks)
  }

  def linkSrCoom(u: IUser, key: String): Unit = {
    db = Database(db.streamChannels, db.runnerRoles, db.games, db.srcomlinks + (u.getStringID -> key))
  }

}

case class Database(streamChannels: Map[String, String] = Map(), runnerRoles: Map[String, String] = Map(), games: Map[String, Array[String]] = Map(), srcomlinks: Map[String, String])
