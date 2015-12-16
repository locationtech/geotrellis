package geotrellis.spark.op.focal

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.raster.op.focal._
import geotrellis.raster.mosaic._
import geotrellis.vector._

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext._

import spire.syntax.cfor._

import annotation.tailrec
import scala.reflect.ClassTag
import scala.collection.mutable.ArrayBuffer

object FocalOperation {

  def apply[K: SpatialComponent: ClassTag](rdd: RDD[(K, Tile)], neighborhood: Neighborhood, opBounds: Option[GridBounds] = None)
      (calc: (Tile, Neighborhood, Option[GridBounds]) => Tile): RDD[(K, Tile)] = {

    val bounds = opBounds.getOrElse(GridBounds(Int.MinValue, Int.MinValue, Int.MaxValue, Int.MaxValue))
    val m: Int = neighborhood.extent // how many pixels we need for the margin        
    
    rdd
      .flatMap { case record @ (key, tile) =>
        val SpatialKey(col, row) = key.spatialComponent
        val slivers = new ArrayBuffer[(K, (Direction, Tile))](9)

        // Tile.crop is inclusive on the max bounds, adjusting offsets to account for that
        val cols = tile.cols-1
        val rows = tile.rows-1
        val mm = m - 1
        
        // ex: adding "TopLeft" corner of this tile to contribute to "TopLeft" tile at key
        def addSlice(spatialKey: SpatialKey, direction: => Direction, sliver: => Tile) {
          if (bounds.contains(spatialKey.col, spatialKey.row))
            slivers += key.updateSpatialComponent(spatialKey) -> (direction, sliver.toArrayTile) // force tile crop                    
        }

        // ex: A tile that contributes to the top (tile above it) will give up it's top slice, which will be placed at the bottom of the target focal window        
        addSlice(SpatialKey(col,row), Center, tile)
      
        addSlice(SpatialKey(col-1, row), Left, tile.crop(0, 0, mm, rows))
        addSlice(SpatialKey(col+1, row), Right, tile.crop(cols-mm, 0, cols, rows))
        addSlice(SpatialKey(col, row-1), Top, tile.crop(0, 0,  cols, mm))
        addSlice(SpatialKey(col, row+1), Bottom, tile.crop(0, rows-mm, cols, rows))

        addSlice(SpatialKey(col-1, row-1), TopLeft, tile.crop(0, 0, mm, mm))
        addSlice(SpatialKey(col+1, row-1), TopRight, tile.crop(cols-mm, 0, cols, mm))
        addSlice(SpatialKey(col+1, row+1), BottomRight, tile.crop(cols-mm, rows-mm, cols, rows))
        addSlice(SpatialKey(col-1, row+1), BottomLeft, tile.crop(0, rows-mm, mm, rows))

        slivers
      }
      .groupByKey
      .flatMap { case (key, neighbors) =>
        neighbors.find( _._1 == Center) map { case (_, tile) =>
          // assume that all tiles are of the same size
          val cols = tile.cols
          val rows = tile.rows          
          val focalTile = ArrayTile.empty(tile.cellType, m+cols+m, m+rows+m)
          
          for ((direction, slice) <- neighbors) {
            val updateBounds = direction.toGridBounds(cols, rows, m)
            focalTile.update(updateBounds.colMin, updateBounds.rowMin, slice)
          }
          val result = calc(focalTile, neighborhood, Some(GridBounds(m, m, m+cols-1, m+rows-1)))
          key -> result
        }                
      }
  }

  def apply[K: SpatialComponent: ClassTag](rasterRDD: RasterRDD[K], neighborhood: Neighborhood)
      (calc: (Tile, Neighborhood, Option[GridBounds]) => Tile): RasterRDD[K] = {
    new ContextRDD(
      apply(rasterRDD, neighborhood, Some(rasterRDD.metaData.gridBounds))(calc),
      rasterRDD.metadata)
  }
}

abstract class FocalOperation[K: SpatialComponent: ClassTag] extends RasterRDDMethods[K] {

  def focal(n: Neighborhood)
      (calc: (Tile, Neighborhood, Option[GridBounds]) => Tile): RasterRDD[K] =
    FocalOperation(rasterRDD, n)(calc)

  def focalWithExtent(n: Neighborhood)
      (calc: (Tile, Neighborhood, Option[GridBounds], RasterExtent) => Tile): RasterRDD[K] = {
    val extent = rasterRDD.metaData.layout.rasterExtent
    FocalOperation(rasterRDD, n){ (tile, n, bounds) =>
      calc(tile, n, bounds, extent)
    }
  }
}

sealed trait Direction {
  /** Map from source of contribution to GridBounds of cells in the target focal tile*/
  def toGridBounds(cols: Int, rows: Int, m: Int): GridBounds
}

case object Center extends Direction {
  def toGridBounds(cols: Int, rows: Int, m: Int) = GridBounds(m, m, m+cols-1, m+rows-1)
}
case object Top extends Direction {
  def toGridBounds(cols: Int, rows: Int, m: Int) = GridBounds(m, m+rows, m+cols-1, m+rows+m-1)
}
case object TopRight extends Direction {
  def toGridBounds(cols: Int, rows: Int, m: Int) = GridBounds(0, m+rows, m-1, m+rows+m-1)
}
case object Right extends Direction {
  def toGridBounds(cols: Int, rows: Int, m: Int) = GridBounds(0, m, m-1, m+rows-1)
}
case object BottomRight extends Direction {
  def toGridBounds(cols: Int, rows: Int, m: Int) = GridBounds(0, 0, m-1, m-1)
}
case object Bottom extends Direction {
  def toGridBounds(cols: Int, rows: Int, m: Int) = GridBounds(m, 0, m+cols-1, m-1)
}
case object BottomLeft extends Direction {
  def toGridBounds(cols: Int, rows: Int, m: Int) = GridBounds(m+cols, 0, m+cols+m-1, m+rows+m-1)
}
case object Left extends Direction {
  def toGridBounds(cols: Int, rows: Int, m: Int) = GridBounds(m+cols, m, m+cols+m-1, m+rows-1)
}
case object TopLeft extends Direction {
  def toGridBounds(cols: Int, rows: Int, m: Int) = GridBounds(m+cols, m+rows, m+cols+m-1, m+rows+m-1)
}
