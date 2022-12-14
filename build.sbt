ThisBuild / version := "0.1.0"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "socialnetwork",
    organization := "me.alstepan",
    assembly / mainClass := Some("socialnetwork.Application"),
    libraryDependencies ++= Seq(
      // cats
      "org.typelevel" %% "cats-core" % "2.8.0",
      "org.typelevel" %% "cats-effect" % "3.3.14",
      // fs2
      "co.fs2" %% "fs2-core" % "3.2.12",
      // ScalaTest
      "org.scalatest" %% "scalatest" % "3.2.12" % Test,
      "org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0" % Test,
    )
  )

ThisBuild / assemblyMergeStrategy := {
  case "application.conf" => MergeStrategy.concat
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}