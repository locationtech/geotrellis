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

import sbt.Keys._
import sbt._
import sbt.complete.DefaultParsers._
import sbt.complete.Parser
import java.text.SimpleDateFormat
import java.util.Date
import pl.project13.scala.sbt.JmhPlugin.JmhKeys.Jmh

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
    val jmhThreads = settingKey[Option[Int]]("Number of JMH worker threads")
    val jmhFork = settingKey[Option[Int]]("How many times to fork a single JMH benchmark")
    val jmhIterations = settingKey[Option[Int]]("Number of measurement iterations to do")
    val jmhWarmupIterations = settingKey[Option[Int]]("Number of warmup iterations to do")
    val jmhTimeUnit = settingKey[Option[String]]("Benchmark results time unit: {m|s|ms|us|ns}")
    val jmhExtraOptions = settingKey[Option[String]]("Additional arguments to jmh:run before the filename regex")
    val bench = taskKey[Unit]("Execute JMH benchmarks")
    val benchOnly = inputKey[Unit]("Execute JMH benchmarks on a single class")
  }

  val autoImport = Keys
  import autoImport._

  override def projectSettings = Seq(
    jmhOutputFormat := "json",
    jmhOutputDir := target.value,
    jmhFileRegex := ".*Bench.*".r,
    jmhThreads := None,
    jmhFork := Some(1),
    jmhIterations := Some(10),
    jmhWarmupIterations := Some(math.max(jmhIterations.value.getOrElse(0)/2, 5)),
    jmhTimeUnit := Some("ms"),
    jmhExtraOptions := None,
    cancelable in Global := true,
    bench := Def.taskDyn { jmhRun(jmhFileRegex.value) }.value,
    benchOnly := Def.inputTaskDyn {
      val file = benchFilesParser.parsed.base
      val pat = (".*" + file + ".*").r
      jmhRun(pat)
    }.evaluated
  )

  def jmhRun(filePattern: Regex) = Def.taskDyn {
    val rf = jmhOutputFormat.value
    def timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
    val dir = jmhOutputDir.value
    IO.createDirectory(dir)

    val rff = target.value / s"jmh-results-$timestamp.$rf"
    val pat = filePattern.toString

    val t = jmhThreads.value.map("-t " + _).getOrElse("")
    val f = jmhFork.value.map("-f " + _).getOrElse("")
    val i = jmhIterations.value.map("-i " + _).getOrElse("")
    val wi = jmhWarmupIterations.value.map("-wi " + _).getOrElse("")
    val tu = jmhTimeUnit.value.map("-tu " + _).getOrElse("")
    val extra = jmhExtraOptions.value.getOrElse("")

    val args = s" $t $f $i $wi $tu -rf $rf -rff $rff $extra $pat"
    state.value.log.debug("Starting: jmh:run " + args)
    (run in Jmh).toTask(args)
  }

  val benchFilesParser: Def.Initialize[State => Parser[File]] = Def.setting { (state: State) =>
    val extracted = Project.extract(state)
    val pat = new PatternFilter(
      extracted.getOpt(jmhFileRegex).getOrElse(".*".r).pattern
    )

    val dirs = Seq(
      extracted.get(scalaSource in Compile),
      extracted.get(scalaSource in Test)
    )

    def benchFileParser(dir: File) = fileParser(dir)
      .filter(f ⇒ pat.accept(f.name), s ⇒ "Not a benchmark file: " + s)

    val parsers = dirs.map(benchFileParser)

    val folded  = parsers
      .foldRight[Parser[File]](failure("<no input files>"))(_ | _)

    token(Space ~> folded)
  }
}
