enablePlugins(GTBenchmarkPlugin)

jmhThreads := 6
jmhFork := 1
jmhIterations := 5
jmhWarmupIterations := math.max(jmhIterations.value/2, 2)
jmhTimeUnit := "ms"
