package geotrellis.spark.op.local

import scala.collection.immutable.Map

import geotrellis.raster._
import geotrellis.raster.op.local.Majority

import geotrellis.spark._
import geotrellis.spark.rdd.RasterRDD

import spire.syntax.cfor._

object MajorityRasterRDD extends Serializable {

  def apply(rs: RasterRDD*): RasterRDD = apply(0, rs)

  def apply(rs: Seq[RasterRDD])(implicit d: DI): RasterRDD = apply(0, rs)

  def apply(level: Int, rs: RasterRDD*): RasterRDD = apply(level, rs)

  def apply(level: Int, rs: Seq[RasterRDD])(implicit d: DI): RasterRDD = {
    val head = rs.headOption.getOrElse(
      sys.error(s"Can't compute majority of empty sequence")
    )

    val count = head.metaData.count
    val cols = head.metaData.cols
    val rows = head.metaData.rows

    val rest = rs.tail

    val sc = head.context

    val tail = sc.parallelize(Array.ofDim[Array[Int]](count))

    val metaDatas = rs.map(r => sc.broadcast(head.metaData))

    if (head.metaData.cellType.isFloatingPoint) {
      val tail = sc.parallelize(Array.ofDim[Array[Map[Int, Int]]](count))
        .map(x => Array.fill(cols * rows)(Map[Int, Int]()))

      def recurse()
    } else {
    }

    null
  }

}

trait MajorityRasterRDDMethods extends RasterRDDMethods {
  /**
    * Assigns to each cell the value within the given rasters that is the
    * most numerous.
    */
  def localMajority(others: Traversable[RasterRDD]): RasterRDD =
    rasterRDD.combineTiles(others.toSeq) {
      case tmsTiles: Seq[TmsTile] =>
        TmsTile(tmsTiles.head.id, Majority(tmsTiles.map(_.tile)))
    }
  /**
    * Assigns to each cell the value within the given rasters that is the
    * most numerous.
    */
  def localMajority(rs: RasterRDD*): RasterRDD =
    localMajority(rs)
  /**
    * Assigns to each cell the value within the given rasters that is the
    * nth most numerous.
    */
  def localMajority(n: Int, others: Traversable[RasterRDD]): RasterRDD =
    rasterRDD.combineTiles(others.toSeq) {
      case tmsTiles: Seq[TmsTile] =>
        TmsTile(tmsTiles.head.id, Majority(n, tmsTiles.map(_.tile)))
    }

  /**
    * Assigns to each cell the value within the given rasters that is the
    * nth most numerous.
    */
  def localMajority(n: Int, rs: RasterRDD*): RasterRDD =
    localMajority(n, rs)
}
