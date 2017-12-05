/*
 *
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
 *
 */

package geotrellis.bench

import org.openjdk.jmh.infra.BenchmarkParams
import pl.project13.scala.jmh.extras.profiler.FlightRecordingProfiler

/**
 * A derived JMH plugin for enabling the Oracle Flight Recorder during runs with some settings customized from the
 * default (hard-coded) ones. Enable by adding
 * {{{
 * jmhExtraOptions := Some("-prof geotrellis.bench.GeotrellisFlightRecordingProfiler")
 * }}}
 * to `build.sbt` in this sub-project.
 *
 * @author sfitch
 * @since 12/3/17
 */
class GeotrellisFlightRecordingProfiler(initLine: String) extends FlightRecordingProfiler(initLine) {
  val outFile = {
    val filenameField = getClass.getSuperclass.getDeclaredField("jfrData")
    filenameField.setAccessible(true)
    filenameField.get(this).asInstanceOf[String]
  }

  val settingsFile = getClass.getResource("/fine.jfc").getPath

  def optionString(opts: Map[String, String]) =
    opts.map(p ⇒ s"${p._1}=${p._2}").mkString(",")

  val startOptions = Map(
    "name" -> getClass.getSimpleName,
    "delay" -> "1s",
    "duration" -> "120s",
    "filename" -> outFile
  )

  val recorderOptions = Map(
    "samplethreads" -> "true",
    "stackdepth" -> "1024",
    "settings" -> settingsFile
  )

  override def addJVMOptions(params: BenchmarkParams) = {
    val opts = Seq(
      "-XX:+UnlockCommercialFeatures",
      "-XX:+FlightRecorder",
      "-XX:+UnlockDiagnosticVMOptions",
      "-XX:+DebugNonSafepoints",
      "-XX:StartFlightRecording=" + optionString(startOptions),
      "-XX:FlightRecorderOptions=" + optionString(recorderOptions)
    )
    println("JFR Options: " + opts.mkString(" "))
    java.util.Arrays.asList(opts: _*)
  }
}
