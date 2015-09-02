import Dependencies._

name := "geotrellis-vector-benchmark"
libraryDependencies ++= Seq(
  scalatest % "test",
  scalacheck % "test",
  caliper,
  "com.google.guava" % "guava" % "r09",
  "com.google.code.java-allocation-instrumenter" % "java-allocation-instrumenter" % "2.0",
  "com.google.code.gson" % "gson" % "1.7.1")
fork := true

// custom kludge to get caliper to see the right classpath
// we need to add the runtime classpath as a "-cp" argument to the
// `javaOptions in run`, otherwise caliper will not see the right classpath
// and die with a ConfigurationException unfortunately `javaOptions` is a
// SettingsKey and `fullClasspath in Runtime` is a TaskKey, so we need to
// jump through these hoops here in order to feed the result of the latter
// into the former
lazy val vectorBenchmarkKey = AttributeKey[Boolean]("vectorJavaOptionsPatched")

onLoad in Global ~= { previous => state =>
  previous {
    state.get(vectorBenchmarkKey) match {
      case None =>
        // get the runtime classpath, turn into a colon-delimited string
        Project
          .runTask(fullClasspath in Runtime, state)
          .get
          ._2
          .toEither match {
          case Right(x) =>
            val classPath =
              x.files
                .mkString(":")
            // return a state with javaOptionsPatched = true and javaOptions set correctly
            Project
              .extract(state)
              .append(
              Seq(javaOptions in run ++= Seq("-Xmx8G", "-cp", classPath)),
                state.put(vectorBenchmarkKey, true)
            )
          case _ => state
        }

      case Some(_) =>
        state // the javaOptions are already patched
    }
  }
}