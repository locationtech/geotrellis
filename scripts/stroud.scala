import scala.math.{round, min, max, sqrt}

import java.io.FileWriter

// uses the Stroud object to generate table values for the mini app
object StroudMiniApp {
  val Ps = Array(50, 100, 200, 320, 800)
  val numLands = 9
  val numSoils = 4

  def dbl(i:Int) = i.asInstanceOf[Double] / 100

  def writeCSVFile(path:String) {
    val fw = new FileWriter(path)

    fw.write("P,land,soil,ET,I,R\n")

    val fmt = "%.1f,%d,%d,%.1f,%.1f,%.1f\n"
    (0 until numLands).foreach {
      land => (0 until numSoils).foreach {
        soil => Ps.foreach {
          P => {
            //val (ET, I, R) = Stroud.getOutputs(land, soil, P)
            val tpl = Stroud.getOutputs(land, soil, P)
            val ET = tpl._1
            val I = tpl._2
            val R = tpl._3
            fw.write(fmt.format(dbl(P), land, soil, dbl(ET), dbl(I), dbl(R)))
          }
        }
      }
    }
    fw.close
  }

  def main(args:Array[String]) {
    if (args.length > 0) {
      val path = args(0)
      printf("writing CSV file to %s\n", path)
      writeCSVFile(args(0))
    } else {
      printf("usage: script PATH\n")
    }
  }
}

// P  precipitation
// ET evapotranspiration
// I  infiltration
// R  runoff
// Precipitation, Infiltration, Runoff and ET are in hundredths of an inch
// (so a value of 13 means 0.13 inches).
object Stroud {
  val maxET = Array(30, 30, 30, 30, 60, 60, 60, 60, 60)

  def getOutputs(land:Int, soil:Int, P:Int) = {
    val maxET = if (land > 3) { 60 } else { 30 }
    val ET = min(P, maxET)
    val P2 = P - ET
    val I = min((land * 300) / 10, P2)
    val R = P2 - I
    (ET, I, R)
  }
}

StroudMiniApp.main(args)
