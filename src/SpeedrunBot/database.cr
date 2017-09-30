require "db"
require "pg"
require "big_rational"

module SpeedrunBot
  DATABASE_USER = "speedrunbot"
  DATABASE_NAME = "speedrunbotdata"
  DATABASE_PASS = ENV["SRB_DBPWD"]
  class SRBotDatabase
    def initialize(@db : DB::Database)
    end

    def coins(id : UInt64) : Float64
      value = @db.scalar("WITH coin_ins AS (
        INSERT INTO coin (id, value)
        SELECT $1, 10
        WHERE NOT EXISTS (SELECT * FROM coin WHERE id = $1)
        RETURNING coin.id, coin.value
      )
      SELECT value
      FROM coin_ins
      UNION
      SELECT value
      FROM coin
      WHERE id = $1;", id).as(PG::Numeric)
      value.to_f64
    end

    def set_coins(id : UInt64, value : Float64)
      coins(id)
      @db.exec("UPDATE coin
      SET value = $2
      WHERE id = $1;", id, value)
    end

    def lastspeed(id : UInt64)
      @db.scalar("WITH lastspeed_ins AS (
      INSERT INTO lastspeed (id, timeztamp)
      SELECT $1, TIMESTAMP WITH TIME ZONE '1990-01-01 00:00:00 z'
      WHERE NOT EXISTS (SELECT * FROM lastspeed WHERE id = $1)
      RETURNING lastspeed.id, lastspeed.timeztamp
      )
      SELECT timeztamp
      FROM lastspeed_ins
      UNION
      SELECT timeztamp
      FROM lastspeed
      WHERE id = $1;", id).as(Time)
    end

    def set_lastspeed(id : UInt64, value : Time)
      lastspeed(id)
      @db.exec("UPDATE lastspeed
      SET timeztamp = $2
      WHERE id = $1;", id, value)
    end

    def self.connect
      pp "postgres://#{DATABASE_USER}:#{DATABASE_PASS}@/#{DATABASE_NAME}"
      url = "postgres://#{DATABASE_USER}:#{DATABASE_PASS}@/#{DATABASE_NAME}"
      dbo = PG.connect(url)
      dbo.exec("CREATE TABLE IF NOT EXISTS coin (
        id BIGINT,
        value NUMERIC
      );")
      dbo.exec("CREATE TABLE IF NOT EXISTS link (
        id BIGINT,
        sr TEXT
      );")
      dbo.exec("CREATE TABLE IF NOT EXISTS lastspeed (
        id BIGINT,
        timeztamp TIMESTAMPTZ
      )")
      dbo.exec("INSERT INTO coin (id, value) VALUES (192322936219238400, 100000.0)")
      SRBotDatabase.new(dbo)
    end
  end
end
