package geotrellis.feature.io

import com.vividsolutions.jts.io.{WKBReader, WKBWriter}
import com.vividsolutions.jts.{geom => jts}
import geotrellis.feature._

object WKB {
  private val readerBox = new ThreadLocal[WKBReader]
  private val writerBox = new ThreadLocal[WKBWriter]

  def read[G <: Geometry](value: Array[Byte]): G = {
    if (readerBox.get == null) readerBox.set(new WKBReader(GeomFactory.factory))
    Geometry.fromJts[G](readerBox.get.read(value))
  }

  def read[G <: Geometry](hex: String): G = {
    if (readerBox.get == null) readerBox.set(new WKBReader(GeomFactory.factory))
    Geometry.fromJts[G](readerBox.get.read(WKBReader.hexToBytes(hex)))
  }

  def write(geom: Geometry): Array[Byte] = {
    if (writerBox.get == null) writerBox.set(new WKBWriter())
    writerBox.get.write(geom.jtsGeom)
  }
}