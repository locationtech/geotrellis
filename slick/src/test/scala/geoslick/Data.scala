package geotrellis.slick

import com.vividsolutions.jts.{geom => jts}
import geotrellis.feature._
import geotrellis.feature.io._

object util {

  def data: Array[(String, Point)] = 
"""[ABE]  40.65   75.43  Allentown,PA
[AOO]  40.30   78.32  Altoona,PA
[BVI]  40.75   80.33  Beaver Falls,PA
[BSI]  40.27   79.09  Blairsville,PA
[BFD]  41.80   78.63  Bradford,PA
[DUJ]  41.18   78.90  Dubois,PA
[ERI]  42.08   80.18  Erie,PA
[FKL]  41.38   79.87  Franklin,PA
[CXY]  40.22   76.85  Harrisburg,PA
[HAR]  40.37   77.42  Harrisburg,PA
[JST]  40.32   78.83  Johnstown,PA
[LNS]  40.13   76.30  Lancaster,PA
[LBE]  40.28   79.40  Latrobe,PA
[MDT]  40.20   76.77  Middletown,PA
[MUI]  40.43   76.57  Muir,PA
[PNE]  40.08   75.02  Nth Philadel,PA
[PHL]  39.88   75.25  Philadelphia,PA
[PSB]  41.47   78.13  Philipsburg,PA
[AGC]  40.35   79.93  Pittsburgh,PA
[PIT]  40.50   80.22  Pittsburgh,PA
[RDG]  40.38   75.97  Reading,PA
[43M]  39.73   77.43  Site R,PA
[UNV]  40.85   77.83  State Colleg,PA
[AVP]  41.33   75.73  Wilkes-Barre,PA
[IPT]  41.25   76.92  Williamsport,PA
[NXX]  40.20   75.15  Willow Grove,PA
""".split("\n")
  .map(str => (str.substring(7,12), str.substring(15,20), str.substring(22)))
  .map(_ match {
    case (lat,lng,city) =>
      (city, WKT.read[Point]("POINT(%f %f)" format (lng.toDouble, lat.toDouble)))
  })

  def bboxBuffer(x: Double, y: Double, d: Double) = 
    WKT.read[Polygon]("POLYGON((%f %f, %f %f, %f %f, %f %f, %f %f))" format (
          x - d, y - d,
          x - d, y + d,
          x + d, y + d,
          x + d, y - d,
          x - d, y - d))

  def pt(x: Double, y: Double) = Point(x, y)
}
