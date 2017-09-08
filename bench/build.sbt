import java.text.SimpleDateFormat
import java.util.Date

import scala.util.matching.Regex

enablePlugins(JmhPlugin)

val jmhOutputFormat = settingKey[String]("Output format: {text|csv|scsv|json|latex}")
jmhOutputFormat := "csv"

val jmhFileRegex = settingKey[Regex]("Filename regular expression for selecting files to benchmark")
jmhFileRegex := ".*Bench.*".r

val jmhRun = Def.taskDyn {
  val rf = jmhOutputFormat.value
  def timestamp = new SimpleDateFormat("yyyyMMdd").format(new Date())
  val rff = target.value / s"jmh-results-$timestamp.${jmhOutputFormat.value}"
  val pat = jmhFileRegex.value.toString

  val args = s" -rf $rf -rff $rff $pat"
  (run in Jmh).toTask(args)
}

val bench = taskKey[Unit]("Run JMH")
bench := jmhRun.value


