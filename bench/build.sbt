enablePlugins(GTBenchmarkPlugin)

libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.25"

jmhIterations := Some(5)
jmhTimeUnit := Some("ms")
jmhExtraOptions := Some("-jvmArgsAppend -Xmx8G")
//jmhExtraOptions := Some("-jvmArgsAppend -prof geotrellis.bench.GeotrellisFlightRecordingProfiler")
