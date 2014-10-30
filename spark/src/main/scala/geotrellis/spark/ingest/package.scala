package geotrellis.spark

import geotrellis.spark.tiling._
import geotrellis.vector._
import geotrellis.raster._
import geotrellis.raster.reproject._

import geotrellis.proj4.CRS

import org.apache.spark.rdd._

import monocle._
import monocle.syntax._

import spire.syntax.cfor._

import scala.reflect.ClassTag

package object ingest {
  type IngestKey[T] = SimpleLens[T, ProjectedExtent]

  case class ProjectedExtent(extent: Extent, crs: CRS)
  object ProjectedExtent {
    implicit def ingestKey: IngestKey[ProjectedExtent] = SimpleLens(x => x, (_, x) => x)
  }

  implicit def projectedExtentToSpatialKeyTiler: Tiler[ProjectedExtent, SpatialKey] =
    new Tiler[ProjectedExtent, SpatialKey] {
      def getExtent(inKey: ProjectedExtent): Extent = inKey.extent
      def createKey(inKey: ProjectedExtent, spatialComponent: SpatialKey): SpatialKey = spatialComponent
    }

  implicit class ReprojectWrapper[T: IngestKey](rdd: RDD[(T, Tile)]) {
    val _projectedExtent = implicitly[IngestKey[T]]
    def reproject(destCRS: CRS): RDD[(T, Tile)] =
      rdd.map { case (key, tile) =>
        val ProjectedExtent(extent, crs) = key |-> _projectedExtent get
        val (newTile, newExtent) = tile.reproject(extent, crs, destCRS)
        (key |-> _projectedExtent set(ProjectedExtent(newExtent, destCRS))) -> newTile
      }
  }

  implicit class RetileWrapper[T: IngestKey](rdd: RDD[(T, Tile)]) {
    val _projectedExtent = implicitly[IngestKey[T]]
    private def getExtent(ingestKey: T): Extent =
      (ingestKey |-> _projectedExtent get).extent

    def retile[K: SpatialComponent: ClassTag](rasterMetaData: RasterMetaData)(createKey: (T, SpatialKey) => K): RasterRDD[K] = {
      val bcMetaData = rdd.sparkContext.broadcast(rasterMetaData)

      val tilesWithKeys: RDD[(K, (K, Extent, Tile))] = 
        rdd
          .flatMap { case (key, tile) =>
            val mapTransform = bcMetaData.value.mapTransform
            val extent = getExtent(key)

            mapTransform(extent)
              .coords
              .map { spatialKey =>
                val newKey = createKey(key, spatialKey)
                (newKey, (newKey, extent, tile))
              }
           }

      // Functions for combine step
      def createTile(tup: (K, Extent, Tile)): MutableArrayTile = {
        val (key, extent, tile) = tup
        val metaData = bcMetaData.value
        val tmsTile = ArrayTile.empty(metaData.cellType, metaData.tileLayout.pixelCols, metaData.tileLayout.pixelRows)
        tmsTile.merge(metaData.mapTransform(key), extent, tile)
      }

      def combineTiles1(tile: MutableArrayTile, tup: (K, Extent, Tile)): MutableArrayTile = {
        val (key, extent, prevTile) = tup
        val metaData = bcMetaData.value
        tile.merge(metaData.mapTransform(key), extent, prevTile)
      }

      def combineTiles2(tile1: MutableArrayTile, tile2: MutableArrayTile): MutableArrayTile =
        tile1.merge(tile2)

      val newRdd: RDD[(K, Tile)] = 
        new PairRDDFunctions(tilesWithKeys)
          .combineByKey(createTile, combineTiles1, combineTiles2)
          .map { case (key, tile) => (key, tile: Tile) }

      new RasterRDD[K](newRdd, rasterMetaData)
    }
  }

  /** Tile methods used by the mosaicing function to merge tiles. */
  implicit class TileMerger(val tile: MutableArrayTile) {
    def merge(other: Tile): MutableArrayTile = {
      Seq(tile, other).assertEqualDimensions
      if(tile.cellType.isFloatingPoint) {
        cfor(0)(_ < tile.rows, _ + 1) { row =>
          cfor(0)(_ < tile.cols, _ + 1) { col =>
            if(isNoData(tile.getDouble(col, row))) {
              tile.setDouble(col, row, other.getDouble(col, row))
            }
          }
        }
      } else {
        cfor(0)(_ < tile.rows, _ + 1) { row =>
          cfor(0)(_ < tile.cols, _ + 1) { col =>
            if(isNoData(tile.get(col, row))) {
              tile.setDouble(col, row, other.get(col, row))
            }
          }
        }
      }

      tile
    }

    def merge(extent: Extent, otherExtent: Extent, other: Tile): MutableArrayTile =
      otherExtent & extent match {
        case PolygonResult(sharedExtent) =>
          val re = RasterExtent(extent, tile.cols, tile.rows)
          val GridBounds(colMin, rowMin, colMax, rowMax) = re.gridBoundsFor(sharedExtent)
          val otherRe = RasterExtent(otherExtent, other.cols, other.rows)

          def thisToOther(col: Int, row: Int): (Int, Int) = {
            val (x, y) = re.gridToMap(col, row)
            otherRe.mapToGrid(x, y)
          }

          if(tile.cellType.isFloatingPoint) {
            cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
              cfor(colMin)(_ <= colMax, _ + 1) { col =>
                if(isNoData(tile.getDouble(col, row))) {
                  val (otherCol, otherRow) = thisToOther(col, row)
                  if(otherCol >= 0 && otherCol < other.cols &&
                    otherRow >= 0 && otherRow < other.rows)
                    tile.setDouble(col, row, other.getDouble(otherCol, otherRow))
                }
              }
            }
          } else {
            cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
              cfor(colMin)(_ <= colMax, _ + 1) { col =>
                if(isNoData(tile.get(col, row))) {
                  val (otherCol, otherRow) = thisToOther(col, row)
                  if(otherCol >= 0 && otherCol < other.cols &&
                    otherRow >= 0 && otherRow < other.rows)
                    tile.set(col, row, other.get(otherCol, otherRow))
                }
              }
            }

          }

          tile
        case _ =>
          tile
      }
  }
}
