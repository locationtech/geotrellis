import sbt._

object Dependencies {
  // def compile   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
  // def provided  (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "provided")
  // def test      (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")
  // def runtime   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "runtime")
  // def container (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "container")

  val scalatest     = "org.scalatest" % "scalatest_2.10" % "2.0"
}

