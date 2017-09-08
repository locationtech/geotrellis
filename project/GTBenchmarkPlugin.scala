/*
 * Copyright 2017 Astraea, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt.Keys.{run, target}
import sbt._
import java.text.SimpleDateFormat
import java.util.Date

import pl.project13.scala.sbt.JmhPlugin

import scala.util.matching.Regex

/**
 * Plugin wrapping up the fine-grained settings associated with running JMH benchmarks
 *
 * @author sfitch 
 * @since 9/8/17
 */
object GTBenchmarkPlugin extends AutoPlugin {
  override def trigger = NoTrigger

  override def requires: Plugins = JmhPlugin

  object Keys {
    val jmhOutputFormat = settingKey[String]("Output format: {text|csv|scsv|json|latex}")
    val jmhOutputDir = settingKey[File]("Directory for writing JMH results")
    val jmhFileRegex = settingKey[Regex]("Filename regular expression for selecting files to benchmark")
    val jmhThreads = settingKey[Int]("Number of JMH worker threads")
    val jmhFork = settingKey[Int]("How many times to fork a single JMH benchmark")
    val jmhIterations = settingKey[Int]("Number of measurement iterations to do")
    val jmhWarmupIterations = settingKey[Int]("Number of warmup iterations to do")
    val jmhTimeUnit = settingKey[String]("Benchmark results time unit: {m|s|ms|us|ns}")
    val jmhExtraOptions = settingKey[String]("Additional arguments to jmh:run before the filename regex")
    val bench = taskKey[Unit]("Execute JMH benchmarks")
  }

  val autoImport = Keys
  import autoImport._
  import pl.project13.scala.sbt.JmhPlugin.JmhKeys.Jmh

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
    val extra = jmhExtraOptions.value

    val args = s" -t $t -f $f -i $i -wi $wi -tu $tu -rf $rf -rff $rff $extra $pat"
    (run in Jmh).toTask(args)
  }

  override def projectSettings = Seq(
    jmhOutputFormat := "csv",
    jmhOutputDir := target.value,
    jmhFileRegex := ".*Bench.*".r,
    jmhThreads := 1,
    jmhFork := 1,
    jmhIterations := 10,
    jmhWarmupIterations := math.max(jmhIterations.value/2, 5),
    jmhTimeUnit := "ms",
    jmhExtraOptions := "",
    bench := jmhRun.value
  )
}
