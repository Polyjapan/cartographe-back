name := "Cartographe"
 
version := "0.1.14"

lazy val `cartographe` = (project in file(".")).enablePlugins(PlayScala, JavaServerAppPackaging, DockerPlugin)

      
resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/"
resolvers += "Japan Impact Snapshots" at "https://repository.japan-impact.ch/snapshots"
resolvers += "Japan Impact Releases" at "https://repository.japan-impact.ch/releases"

scalaVersion := "2.13.5"

libraryDependencies ++= Seq( jdbc , ehcache , ws , specs2 % Test , guice ,
  "com.typesafe.play" %% "play-json" % "2.8.1",
  "org.playframework.anorm" %% "anorm" % "2.6.4",
  "net.postgis" % "postgis-jdbc" % "2.5.1",
  "org.postgresql" % "postgresql" % "42.2.23",
  "ch.japanimpact" %% "jiauthframework" % "2.0.5",
  "com.pauldijou" %% "jwt-play" % "4.2.0"
)

javaOptions in Universal ++= Seq(
  // Provide the PID file
  s"-Dpidfile.path=/dev/null",
  // s"-Dpidfile.path=/run/${packageName.value}/play.pid",

  // Set the configuration to the production file
  s"-Dconfig.file=/etc/${packageName.value}/production.conf",

  // Apply DB evolutions automatically
  "-DapplyEvolutions.default=true"
)

dockerExposedPorts := Seq(80)
dockerBaseImage := "openjdk:11"

Docker / dockerRepository := Some("registry.japan-impact.ch")
