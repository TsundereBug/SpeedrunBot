name := "SpeedrunBot"

version := "1.0"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  "com.github.austinv11" % "Discord4J" % "f9695fb",
  "com.github.urgrue" % "Java-Twitch-Api-Wrapper" % "develop-SNAPSHOT",
  "com.github.TsundereBug" % "Speedrun4J" % "2749b3c",
  "com.sedmelluq" % "lavaplayer" % "1.2.42",
  "net.liftweb" %% "lift-json" % "3.1.0-RC1"
)

libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value
dependencyOverrides += "org.scala-lang" % "scala-compiler" % scalaVersion.value
libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
dependencyOverrides += "org.scala-lang" % "scala-reflect" % scalaVersion.value

resolvers += "jcenter" at "http://jcenter.bintray.com"
resolvers += "jitpack.io" at "https://jitpack.io"