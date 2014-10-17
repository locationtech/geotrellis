package geotrellis.spark

import geotrellis.spark._
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
  case class ProjectedExtent(extent: Extent, crs: CRS)

  implicit class ReprojectWrapper[T](rdd: RDD[(T, Tile)])(implicit _projectedExtent: SimpleLens[T, ProjectedExtent]) {
    def reproject(destCRS: CRS): RDD[(T, Tile)] =
      rdd.map { case (key, tile) =>
        val ProjectedExtent(extent, crs) = key |-> _projectedExtent get
        val (newTile, newExtent) = tile.reproject(extent, crs, destCRS)
        (key |-> _projectedExtent set(ProjectedExtent(newExtent, destCRS))) -> newTile
      }
  }

  implicit class MetaDataWrapper[T](rdd: RDD[(T, Tile)])(implicit _projectedExtent: SimpleLens[T, ProjectedExtent]) {

    /**
      * Creates metadata from the input RDD, based off of the keys and the LayoutScheme.
      * Assumes all CRS's are the same for each key.
      * 
      *   @param layerName       Name of the layer.
      *   @param layoutScheme    The LayoutScheme that will determine the zoom level for this layer.
      *   @param isUniform       If this is true, go through each key to determine the overall extent,
      *                          cell type and cell size. Otherwise, just take the first key's values.
      *   @param newMax     New maximum value
      */
    def createMetaData(layerName: String, layoutScheme: LayoutScheme, isUniform: Boolean = true): LayerMetaData = {
      val (uncappedExtent, cellType, cellSize, crs): (Extent, CellType, CellSize, CRS) =
        if(isUniform) {
          val (key, tile) = rdd.first
          val ProjectedExtent(extent, crs) = (key |-> _projectedExtent get)
          (extent, tile.cellType, CellSize(extent, tile.cols, tile.rows), crs)
        } else {
          rdd
            .map { case (key, tile) =>
              val ProjectedExtent(extent, crs) = (key |-> _projectedExtent get)
              (extent, tile.cellType, CellSize(extent, tile.cols, tile.rows), crs)
             }
            .reduce { (t1, t2) =>
              val (e1, ct1, cs1, crs1) = t1
              val (e2, ct2, cs2, _) = t2
              (
                e1.combine(e2),
                ct1.union(ct2),
                if(cs1.resolution < cs2.resolution) cs1 else cs2,
                crs1
              )
             }
        }

      val worldExtent = crs.worldExtent
      val layoutLevel: LayoutLevel = layoutScheme.levelFor(worldExtent, cellSize)

      val extentIntersection = worldExtent.intersection(uncappedExtent).get

      LayerMetaData(
        LayerId(layerName, layoutLevel.zoom),
        RasterMetaData(cellType, extentIntersection, crs, layoutLevel.tileLayout)
      )
    }
  }

  implicit class RetileWrapper[T, K: SpatialComponent: ClassTag](rdd: RDD[(T, Tile)])(implicit _extent: SimpleLens[T, Extent]) {
    def retile(rasterMetaData: RasterMetaData)(getExtent: T => Extent)(createKey: (T, SpatialKey) => K): RasterRDD[K] = {
      val bcMetaData = rdd.sparkContext.broadcast(rasterMetaData)

      val tilesWithKeys: RDD[(K, (K, Extent, Tile))] = 
        rdd
          .flatMap { case (key, tile) =>
            val transform = bcMetaData.value.transform
            val extent = key |-> _extent get

            transform.mapToGrid(extent)
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
        tmsTile.merge(metaData.transform.gridToMap(key), extent, tile)
      }

      def combineTiles1(tile: MutableArrayTile, tup: (K, Extent, Tile)): MutableArrayTile = {
        val (key, extent, prevTile) = tup
        val metaData = bcMetaData.value
        tile.merge(metaData.transform.gridToMap(key.spatialComponent), extent, prevTile)
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
