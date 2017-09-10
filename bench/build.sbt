enablePlugins(GTBenchmarkPlugin)

jmhIterations := Some(5)
jmhTimeUnit := Some("ms")
jmhExtraOptions := Some("-jvmArgsAppend -Xmx8G")
