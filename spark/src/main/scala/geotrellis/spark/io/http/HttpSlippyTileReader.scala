package geotrellis.spark.io.http

import geotrellis.vector._
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.spark._
import geotrellis.spark.io.s3._

import geotrellis.spark.io.slippy._
import geotrellis.util.Filesystem

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter._
import org.apache.commons.io.IOUtils._
import org.apache.spark._
import org.apache.spark.rdd._
import java.net._
import java.io.File

class HttpSlippyTileReader[T](pathTemplate: String)(fromBytes: (SpatialKey, Array[Byte]) => T) extends SlippyTileReader[T] {
    def getURL(template: String, z: Int, x: Int, y: Int) = 
        template.replace("{z}", z.toString).replace("{x}", x.toString).replace("{y}", y.toString)
    def getByteArray(url: String) = {
      val inStream = new URL(url).openStream()
      try {
        toByteArray(inStream)
      } finally {
        inStream.close()
      }
    }

    def read(zoom: Int)(implicit sc: SparkContext): RDD[(SpatialKey, T)] = ???
    def read(zoom: Int, key: SpatialKey): T = fromBytes(key, getByteArray(getURL(pathTemplate, zoom, key.col, key.row)))
    override def read(zoom: Int, x: Int, y: Int): T =
        read(zoom, SpatialKey(x, y))
}