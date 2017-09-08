import java.text.SimpleDateFormat
import java.util.Date

import scala.util.matching.Regex

enablePlugins(JmhPlugin)

val jmhOutputFormat = settingKey[String]("Output format: {text|csv|scsv|json|latex}")
jmhOutputFormat := "csv"

val jmhOutputDir = settingKey[File]("Directory for writing JMH results")
jmhOutputDir := target.value

val jmhFileRegex = settingKey[Regex]("Filename regular expression for selecting files to benchmark")
jmhFileRegex := ".*Bench.*".r

val jmhThreads = settingKey[Int]("Number of worker threads")
jmhThreads := 1

val jmhFork = settingKey[Int]("How many times to fork a single benchmark")
jmhFork := 1

val jmhIterations = settingKey[Int]("Number of measurement iterations to do")
jmhIterations := 10

val jmhWarmupIterations = settingKey[Int]("Number of warmup iterations to do.")
jmhWarmupIterations := math.max(jmhIterations.value/2, 5)

val jmhTimeUnit = settingKey[String]("Benchmark results unit: {m|s|ms|us|ns}.")
jmhTimeUnit := "ms"


val jmhRun = Def.taskDyn {
  val rf = jmhOutputFormat.value
  def timestamp = new SimpleDateFormat("yyyyMMdd").format(new Date())
  val dir = jmhOutputDir.value
  IO.createDirectory(dir)

  val rff = target.value / s"jmh-results-$timestamp.${jmhOutputFormat.value}"
  val pat = jmhFileRegex.value.toString

  val t = jmhThreads.value
  val f = jmhFork.value
  val i = jmhIterations.value
  val wi = jmhWarmupIterations.value
  val tu = jmhTimeUnit.value

  val args = s" -t $t -f $f -i $i -wi $wi -tu $tu -rf $rf -rff $rff $pat"
  (run in Jmh).toTask(args)
}

val bench = taskKey[Unit]("Run JMH")
bench := jmhRun.value


