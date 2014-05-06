package geotrellis.feature.io

import com.vividsolutions.jts.io.{WKBReader}
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

  def write(geom: Geometry, srid: Int = 0): Array[Byte] = {
    if (writerBox.get == null) writerBox.set(new WKBWriter(2))
    if (srid == 0)
      writerBox.get.write(geom)
    else
      writerBox.get.write(geom, Some(srid))
  }
}