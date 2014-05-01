package geotrellis.feature.io

import com.vividsolutions.jts.io.{WKTReader, WKTWriter}
import com.vividsolutions.jts.{geom => jts}
import geotrellis.feature._

object WKT {
  private val readerBox = new ThreadLocal[WKTReader]
  private val writerBox = new ThreadLocal[WKTWriter]

  def read[G <: Geometry](value: String): G = {
    if (readerBox.get == null) readerBox.set(new WKTReader(GeomFactory.factory))
    Geometry.fromJts[G](readerBox.get.read(value))
  }

  def write(geom: Geometry): String = {
    if (writerBox.get == null) writerBox.set(new WKTWriter())
    writerBox.get.write(geom.jtsGeom)
  }
}