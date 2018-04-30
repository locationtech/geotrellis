package geotrellis.spark.io.tms

import geotrellis.raster._
import geotrellis.vector.{Point, Extent}
import geotrellis.spark.tiling.ZoomedLayoutScheme
import geotrellis.spark._

import spire.syntax.cfor._
import scalaj.http._
import org.apache.spark._
import org.apache.spark.rdd._

import scala.collection.mutable._
import java.net.{URI, URL}
import javax.imageio._


class SparkTMSReader[T](
  uriTemplate: String,
  f: URI => T = TMSReader.decodeTile _,
  tileSize: Int = ZoomedLayoutScheme.DEFAULT_TILE_SIZE
) extends TMSReader(uriTemplate, f) {
  /** Read a region from a ZXY source into RDD
   * @param zoom Level of zoom to read from
   * @param bounds Query bounds to read
   * @param windowCols The number of columns per window within the provided gridbounds
   * @param windowRows The number of rows per window within the provided gridbounds
   */
  def rdd(zoom: Int, bounds: GridBounds, windowCols: Int = 4, windowRows: Int = 4)(implicit sc: SparkContext): RDD[(SpatialKey, T)] = {
    var boundsSeq = bounds.split(windowCols, windowRows).toSeq
    sc.parallelize(boundsSeq).mapPartitions({ partition =>
      val localReader = TMSReader(uriTemplate, f)
      partition.flatMap { window => localReader.read(zoom, window) }
    })
  }
}
