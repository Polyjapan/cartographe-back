name := "Cartographe"
 
version := "1.0" 
      
lazy val `cartographe` = (project in file(".")).enablePlugins(PlayScala)

      
resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/"
      
scalaVersion := "2.13.5"

libraryDependencies ++= Seq( jdbc , ehcache , ws , specs2 % Test , guice ,
  "com.typesafe.play" %% "play-json" % "2.8.1",
  "org.playframework.anorm" %% "anorm" % "2.6.4",
  "net.postgis" % "postgis-jdbc" % "2.5.1",
  "org.postgresql" % "postgresql" % "42.2.23"
)
      