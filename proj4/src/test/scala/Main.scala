package dwins

import scala.sys.process._

object Proj {
  implicit class withAsStream(val a: String) extends AnyVal {
    def asStream: java.io.InputStream = 
      new java.io.ByteArrayInputStream(a.getBytes(java.nio.charset.StandardCharsets.UTF_8))
  }

  def grid(minx: Int, miny: Int, maxx: Int, maxy: Int): Iterator[(Double, Double)] = 
    for { 
      x <- (minx to maxx).iterator
      y <- miny to maxy
    } yield (x.toDouble, y.toDouble)

  def loan[T](r: => scala.io.Source)(f: scala.io.Source => T): T = {
    val resource = r
    try
      f(resource)
    finally
      resource.close
  }

  def main(args: Array[String]): Unit = {
    val registeredCodes = 
      loan(scala.io.Source.fromInputStream(getClass().getResourceAsStream("/geotrellis/proj4/nad/epsg"))) { source =>
        source.getLines
          .filter { _ startsWith "<" }
          .map { s => s.tail.take(s.indexOf('>') - 1) }
          .to[Set]
      }
    val knownCodes = 
      loan(scala.io.Source.fromFile("/Users/dwins/Projects/radiantblue/proj.4/nad/epsg")) { source =>
        source.getLines
          // .filterNot { _ contains "+pm=" }
          // .filterNot { _ contains "+axis=" }
          // .filterNot { _ contains "+gamma=" }
          // .filterNot { _ contains "+proj=cea" }
          // .filterNot { _ contains "+proj=krovak" }
          // .filterNot { _ contains "+proj=nzmg" }
          .filter { _ startsWith "<" }
          .map { s => s.tail.take(s.indexOf('>') - 1) }
          .filter { _ != "4326" }
          .filter { registeredCodes }
          .to[Vector]
      }
    
    val output = new java.io.FileWriter("proj4/src/test/resources/proj4-epsg.csv");

    val csvWriter = new com.opencsv.CSVWriter(output)
    csvWriter.writeNext(
      Array("testName","testMethod","srcCrsAuth","srcCrs","tgtCrsAuth","tgtCrs","srcOrd1","srcOrd2","srcOrd3","tgtOrd1","tgtOrd2","tgtOrd3","tolOrd1","tolOrd2","tolOrd3","using","dataSource","dataCmnts","maintenanceCmnts"));

    def writeLine(srcCrs: Int, tgtCrs: Int, src: (Double, Double, Double), tgt: (Double, Double, Double)): Array[String] =
      Array(
        s"$srcCrs -> $tgtCrs", // "testName",
        "proj4j", // "testMethod",
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
        f"${1e-6}%f", // "tolOrd1",
        f"${1e-6}%f", // "tolOrd2",
        f"${1e-6}%f", // "tolOrd3",
        "", // "using",
        "", // "dataSource",
        "", // "dataCmnts",
        "Auto-generated from proj.4 epsg database") // "maintenanceCmnts");
    
    knownCodes.foreach { code =>
      val forward = cs2cs(4326, code.toInt, (1, -1, 0))
      if (forward.isDefined) {
        csvWriter.writeNext(writeLine(4326, code.toInt, (1, -1, 0), forward.get))
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
      "../proj.4/built/bin/cs2cs",
      "-e", "NaN NaN",
      "-f", "%1.06f",
      s"+init=epsg:$src",
      "+to",
      s"+init=epsg:$dst")

    var error: Boolean = false
    val logger = ProcessLogger(
      out => ???,
      err => error = true)
    val line = ((cmd #< f"${xyz._1}%1.6f ${xyz._2}%1.6f ${xyz._3}%1.6f".asStream) !! logger).trim

    if (error)
      None
    else {
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
