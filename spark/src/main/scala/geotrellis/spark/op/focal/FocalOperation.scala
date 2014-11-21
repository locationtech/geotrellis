package geotrellis.spark.op.focal

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.raster.op.focal._

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext._

import collection.mutable.ListBuffer

trait FocalOperation[K] extends RasterRDDMethods[K] {

  val _sc: SpatialComponent[K]

  def zipWithNeighbors: RDD[(K, Tile, TileNeighbors)] = {
    val sc = rasterRDD.sparkContext
    val bcMetadata = sc.broadcast(rasterRDD.metaData)
    rasterRDD.map { case (key, tile) =>
      val metadata = bcMetadata.value
      val gridBounds = metadata.gridBounds
      val SpatialKey(row, col) = key

      val b = (
        (if (col == gridBounds.colMin) 1 else 0) |
          (if (col == gridBounds.colMax) 2 else 0) |
          (if (row == gridBounds.rowMin) 4 else 0) |
          (if (row == gridBounds.rowMax) 8 else 0)
      )

      def getCoords(idx: Int) = (col + (idx % 3 - 1), row + (idx / 3) - 1)

      def getTile(idx: Int): Option[Tile] =
        if (idx == -1) None
        else {
          val bcCoords = rasterRDD.sparkContext.broadcast(getCoords(idx))
          val (k, t) = rasterRDD.filter { case (k, t) =>
            val (targetCol, targetRow) = bcCoords.value
            val SpatialKey(row, col) = k

            targetCol == col && targetRow == row
          }.first

          Some(t)
        }

      /* TileNeighbors Index Scheme:
       *  0 1 2
       *  3 X 5
       *  6 7 8
       *
       * Only 9 scenarios:
       * 0 => no bounds collided.
       * 1 => left bound collided.
       * 2 => right bound collided.
       * 4 => top bound collided.
       * 5 => top left bound collided.
       * 6 => top right bound collided.
       * 8 => bottom bound collided.
       * 9 => bottom left bound collided.
       * 10 => bottom right bound collided.
       */
      val tileNeighborIndices = b match {
        case 0 => Seq(0, 1, 2, 3, 5, 6, 7, 8)
        case 1 => Seq(-1, 1, 2, -1, 5, -1, 7, 8)
        case 2 => Seq(0, 1, -1, 3, -1, 6, 7, -1)
        case 4 => Seq(-1, -1, -1, 3, 5, 6, 7, 8)
        case 5 => Seq(-1, -1, -1, -1, 5, -1, 7, 8)
        case 6 => Seq(-1, -1, -1, 3, -1, 6, 7, -1)
        case 8 => Seq(0, 1, 2, 3, 5, -1, -1, -1)
        case 9 => Seq(-1, 1, 2, -1, 5, -1, -1, -1)
        case 10 => Seq(0, 1, -1, 3, -1, -1, -1, -1)
        case w => throw new IllegalStateException(
          s"Wrong tile neighbor config $w, programmer error."
        )
      }

      (key, tile, SeqTileNeighbors(tileNeighborIndices.map(getTile)))
    }
  }

  def focal(n: Neighborhood)
    (calc: (Tile, Neighborhood, Option[GridBounds]) => Tile): RasterRDD[K] = {
    val sc = rasterRDD.sparkContext
    val scCalc = sc.broadcast(calc)
    val scNeighborhood = sc.broadcast(n)

    val rdd = zipWithNeighbors.map { case (key, center, neighbors) =>
      val calc = scCalc.value
      val neighborhood = scNeighborhood.value

      val (neighborhoodTile, analysisArea) =
        TileWithNeighbors(center, neighbors.getNeighbors)
      (key, calc(neighborhoodTile, neighborhood, Some(analysisArea)))
    }

    new RasterRDD(rdd, rasterRDD.metaData)
  }
}
