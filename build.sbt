import sbt.Keys.{libraryDependencies, resolvers}

ThisBuild / organization := "ch.japanimpact"
ThisBuild / scalaVersion := "2.13.5"
ThisBuild / libraryDependencies ++= Seq(
  "ch.japanimpact" %% "jiauthframework" % "2.1.0"
)
ThisBuild / resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/"
ThisBuild / resolvers += "Japan Impact Snapshots" at "https://repository.japan-impact.ch/snapshots"
ThisBuild / resolvers += "Japan Impact Releases" at "https://repository.japan-impact.ch/releases"


lazy val api = (project in file("api"))
  .settings(
    name := "cartographe-api",
    version := "0.1.16",
    libraryDependencies += cacheApi,
    publishTo := {
      Some("Japan Impact Repository" at {
        "https://repository.japan-impact.ch/" + (if (isSnapshot.value) "snapshots" else "releases")
      })
    },
    credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
  )


lazy val root = (project in file("."))
  .enablePlugins(PlayScala, JavaServerAppPackaging, DockerPlugin)
  .settings(
    name := "Cartographe",
    version := "0.1.16",
    libraryDependencies ++= Seq(jdbc, ehcache, ws, specs2 % Test, guice,
      "com.typesafe.play" %% "play-json" % "2.8.1",
      "org.playframework.anorm" %% "anorm" % "2.6.4",
      "net.postgis" % "postgis-jdbc" % "2.5.1",
      "org.postgresql" % "postgresql" % "42.2.23",
      "com.pauldijou" %% "jwt-play" % "4.2.0",
      "ch.japanimpact" %% "staff-api" % "1.5.2",
      "ch.japanimpact" %% "ji-events-api" % "1.0-SNAPSHOT",
    ),

    javaOptions in Universal ++= Seq(
      // Provide the PID file
      s"-Dpidfile.path=/dev/null",
      // s"-Dpidfile.path=/run/${packageName.value}/play.pid",

      // Set the configuration to the production file
      s"-Dconfig.file=/etc/${packageName.value}/production.conf",

      // Apply DB evolutions automatically
      "-DapplyEvolutions.default=true"
    ),

    dockerExposedPorts := Seq(80),
    dockerBaseImage := "openjdk:11",

    Docker / dockerRepository := Some("registry.japan-impact.ch"),
  )
  .aggregate(api)

