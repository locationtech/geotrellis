enablePlugins(GTBenchmarkPlugin)

jmhOutputFormat := "csv"
jmhFileRegex := ".*Bench.*".r
jmhThreads := 1
jmhFork := 1
jmhIterations := 10
jmhWarmupIterations := math.max(jmhIterations.value/2, 5)
