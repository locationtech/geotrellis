/*
 * Copyright 2016 Azavea
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

package geotrellis.proj4

import scala.sys.process._
import scala.util.Try

/**
 * Use the C proj.4 library to generate test cases to cross-validate the proj4j implementation.
 */
object GenerateTestCases {
  implicit class withAsStream(val a: String) extends AnyVal {
    def asStream: java.io.InputStream =
      new java.io.ByteArrayInputStream(a.getBytes(java.nio.charset.StandardCharsets.UTF_8))
  }

  def loan[T](r: => scala.io.Source)(f: scala.io.Source => T): T = {
    val resource = r
    try
      f(resource)
    finally
      resource.close
  }

  def main(args: Array[String]): Unit = {
    val knownCodes =
      loan(scala.io.Source.fromInputStream(getClass().getResourceAsStream("/geotrellis/proj4/nad/epsg"))) { source =>
        source.getLines
          .filter { _ startsWith "<" }
          .map { s => s.tail.take(s.indexOf('>') - 1) }
          .filterNot { _ == "4326" }
          .to[Vector]
      }

    val output = new java.io.FileWriter("proj4/src/test/resources/proj4-epsg.csv");

    val csvWriter = new com.opencsv.CSVWriter(output)
    csvWriter.writeNext(
      Array("testName","testMethod","srcCrsAuth","srcCrs","tgtCrsAuth","tgtCrs","srcOrd1","srcOrd2","srcOrd3","tgtOrd1","tgtOrd2","tgtOrd3","tolOrd1","tolOrd2","tolOrd3","using","dataSource","dataCmnts","maintenanceCmnts"));

    def writeLine(srcCrs: Int, tgtCrs: Int, method: String, src: (Double, Double, Double), tgt: (Double, Double, Double), tol: Double): Array[String] =
      Array(
        s"$srcCrs -> $tgtCrs", // "testName",
        method, // "testMethod",
        "EPSG", // "srcCrsAuth",
        srcCrs.toString, // "srcCrs",
        "EPSG", // "tgtCrsAuth",
        tgtCrs.toString, // "tgtCrs",
        src._1.toString, // "srcOrd1",
        src._2.toString, // "srcOrd2",
        src._3.toString, // "srcOrd3",
        tgt._1.toString, // "tgtOrd1",
        tgt._2.toString, // "tgtOrd2",
        tgt._3.toString, // "tgtOrd3",
        f"${tol}%f", // "tolOrd1",
        f"${tol}%f", // "tolOrd2",
        f"${tol}%f", // "tolOrd3",
        "", // "using",
        "", // "dataSource",
        "", // "dataCmnts",
        "Auto-generated from proj.4 epsg database") // "maintenanceCmnts");

    knownCodes.foreach { code =>
      val forward = cs2cs(4326, code.toInt, (1, -1, 0))
      for (pt @ (x, y, z) <- forward) {
        val (method, tolerance) =
          try {
            val dst = CRS.fromName(s"EPSG:$code")
            val tolerance = dst.proj4jCrs.getProjection.getUnits match {
              case null => 1
              case u if Set(org.osgeo.proj4j.units.Units.DEGREES) contains u => 1e-6 * u.value
              case u => 0.1 * u.value
            }
            val tx = Transform(LatLng, dst)
            val (u, v) = tx(1, -1)
              if ((x - u).abs < tolerance && (v - y).abs < tolerance)
                ("passing", tolerance)
              else
                ("failing", tolerance)
          } catch {
            case _: org.osgeo.proj4j.Proj4jException => ("error", 0.01d)
          }

        csvWriter.writeNext(writeLine(4326, code.toInt, method, (1, -1, 0), pt, tolerance))
        // TODO: Need to do better selection of the starting point, we get
        // results sometimes that are outside the area of validity for the
        // target coordinate system.  Until we do, the inverse transform tests won't all pass.

        // val inverse = cs2cs(code.toInt, 4326, forward.get)
        // if (inverse.isDefined)
        //   csvWriter.writeNext(writeLine(code.toInt, 4326, forward.get, inverse.get))
      }
    }
    csvWriter.flush()
    output.close()
  }

  def cs2cs(src: Int, dst: Int, xyz: (Double, Double, Double)): Option[(Double, Double, Double)] = {
    val cmd = Vector(
      "cs2cs",
      "-e", "NaN NaN",
      "-f", "%1.06f",
      s"+init=epsg:$src",
      "+to",
      s"+init=epsg:$dst")

    var error: Boolean = false
    val logger = ProcessLogger(
      out => ???,
      err => error = true)
    val line = Try { ((cmd #< f"${xyz._1}%1.6f ${xyz._2}%1.6f ${xyz._3}%1.6f".asStream) !! logger).trim }

    if (error)
      None
    else {
      line.toOption.flatMap { line =>
        val coords @ Seq(x, y, z) = line.split("\\s+").toSeq.map {
          case "nan" => Double.NaN
          case "inf" => Double.PositiveInfinity
          case s => s.toDouble
        }
        if (coords.exists(d => d.isInfinite || d.isNaN))
          None
        else
          Some((x, y, z))
      }
    }
  }
}
