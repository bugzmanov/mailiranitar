lazy val akkaHttpVersion = "10.1.11"
lazy val akkaVersion = "2.6.4"

lazy val versions = new {
  val finatra = "20.6.0"
}

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.bugzmanov",
      scalaVersion := "2.12.8"
    )),
    name := "mailiranitar",
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.github.ben-manes.caffeine" % "caffeine" % "2.8.5",
      "com.twitter" %% "finatra-http" % "20.6.0",
      "com.typesafe" % "config" % "1.4.0",
      "io.micrometer" % "micrometer-registry-graphite" % "latest.release",

      "org.scalatest" %% "scalatest" % "3.0.8" % Test,

      "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.11.0",
      // finatra-test
      "com.twitter" %% "finatra-httpclient" % "20.6.0" % "test" ,
      "com.twitter" %% "finatra-http" % "20.6.0" % "test" classifier "tests",
      "com.twitter" %% "finatra-jackson" % "20.6.0" % "test" classifier "tests",
      "com.twitter" %% "inject-server" % "20.6.0" % "test" classifier "tests",
      "com.twitter" %% "inject-app" % "20.6.0" % "test" classifier "tests",
      "com.twitter" %% "inject-core" % "20.6.0" % "test" classifier "tests",
      "com.twitter" %% "inject-modules" % "20.6.0" % "test" classifier "tests"
    )
  )

enablePlugins(JavaAppPackaging)
javaOptions in Universal ++= Seq(
  "-J-XX:MaxRAMPercentage=100",
  "-Dconfig.file=/opt/mailiranitar/application.conf"
)

enablePlugins(DockerPlugin)
enablePlugins(AshScriptPlugin)

dockerBaseImage := "expert/docker-java-minimal:jdk12-alpine"

mainClass in Compile := Some("com.bugzmanov.mailiranitar.StandaloneApp")

