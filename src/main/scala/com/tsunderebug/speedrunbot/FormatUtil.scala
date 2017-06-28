package com.tsunderebug.speedrunbot

import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object FormatUtil {

  private val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
  sdf.setTimeZone(TimeZone.getTimeZone("UTC"))

  def msToTime(ms: Long): String = {
    String.format("%02d:%02d:%02d.%03d",
      new java.lang.Long(TimeUnit.MILLISECONDS.toHours(ms)),
      new java.lang.Long(TimeUnit.MILLISECONDS.toMinutes(ms) % TimeUnit.HOURS.toMinutes(1)),
      new java.lang.Long(TimeUnit.MILLISECONDS.toSeconds(ms) % TimeUnit.MINUTES.toSeconds(1)),
      new java.lang.Long(ms % 1000)
    )
  }

  def timeToMS(time: String): Long = {
    sdf.parse("1970-01-01 " + time).getTime
  }

}
