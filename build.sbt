name := "SpeedrunBot"

version := "1.0"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  "com.github.austinv11" % "Discord4J" % "dev-SNAPSHOT",
  "com.github.urgrue" % "Java-Twitch-Api-Wrapper" % "develop-SNAPSHOT",
  "com.sedmelluq" % "lavaplayer" % "1.2.42",
  "net.liftweb" %% "lift-json" % "3.1.0-RC1"
)

resolvers += "jcenter" at "http://jcenter.bintray.com"
resolvers += "jitpack.io" at "https://jitpack.io"