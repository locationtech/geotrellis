package geotrellis.spark

import geotrellis.raster._
import geotrellis.raster.io.tms._
import geotrellis.vector.{Point, Extent}
import geotrellis.spark.tiling.ZoomedLayoutScheme

import spire.syntax.cfor._
import scalaj.http._

import scala.collection.mutable._
import java.net.{URI, URL}
import javax.imageio._


import org.apache.spark.rdd._
import geotrellis.spark._


class SparkTMSReader[T](
  uriTemplate: String,
  f: URI => T = TMSReader.decodeTile _,
  tileSize = ZoomedLayoutScheme.DEFAULT_TILE_SIZE
) extends TMSReader(uriTemplate, f) {
  /** Read a region from a ZXY source into RDD
   * @param zoom Level of zoom to read from
   * @param bounds Query bounds to read
   * @param window Window that will cover partitions, offset from (0,0) of bounds
   */
  def rdd(zoom: Int, bounds: GridBounds, window: GridBounds = GridBounds(0,0,10,10)): RDD[(SpatialKey, T)] = {
    // - divide bound into num "rectangular" partitions
    // - make rdd of bounds
    // - map from bounds to records using f
    ???
  }
}
