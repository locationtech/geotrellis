package geotrellis.spark.buffer

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.stitch._

import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel

import scala.reflect.ClassTag
import scala.collection.mutable.ArrayBuffer

object BufferTiles {
  sealed trait Direction

  case object Center extends Direction
  case object Top extends Direction
  case object TopRight extends Direction
  case object Right extends Direction
  case object BottomRight extends Direction
  case object Bottom extends Direction
  case object BottomLeft extends Direction
  case object Left extends Direction
  case object TopLeft extends Direction

  def collectWithNeighbors[K: SpatialComponent, V <: CellGrid: (? => CropMethods[V])](
    key: K, 
    tile: V, 
    includeKey: SpatialKey => Boolean,
    getBufferSizes: SpatialKey => BufferSizes
  ): Seq[(K, (Direction, V))] = {
    val SpatialKey(col, row) = key.spatialComponent
    val parts = new ArrayBuffer[(K, (Direction, V))](9)

    val cols = tile.cols
    val rows = tile.rows

    // ex: adding "TopLeft" corner of this tile to contribute to "TopLeft" tile at key
    def addSlice(spatialKey: SpatialKey, direction: => Direction) {
      if(includeKey(spatialKey)) {
        val bufferSizes = getBufferSizes(spatialKey)

        val part: V =
          direction match {
            case Center => tile
            case Right => tile.crop(0, 0, bufferSizes.right - 1, rows - 1, force = true)
            case Left => tile.crop(cols - bufferSizes.left, 0, cols - 1, rows - 1, force = true)
            case Top => tile.crop(0, rows - bufferSizes.top, cols - 1, rows - 1, force = true)
            case Bottom => tile.crop(0, 0, cols - 1, bufferSizes.bottom - 1, force = true)
            case TopLeft => tile.crop(cols - bufferSizes.left, rows - bufferSizes.top, cols - 1, rows - 1, force = true)
            case TopRight => tile.crop(0, rows - bufferSizes.top, bufferSizes.right - 1, rows - 1, force = true)
            case BottomLeft => tile.crop(cols - bufferSizes.left, 0, cols - 1, bufferSizes.bottom - 1, force = true)
            case BottomRight => tile.crop(0, 0, bufferSizes.right - 1, bufferSizes.bottom - 1, force = true)
          }

        parts += ( (key.updateSpatialComponent(spatialKey), (direction, part)) )
      }
    }

    // ex: A tile that contributes to the top (tile above it) will give up it's top slice, which will be placed at the bottom of the target focal window
    addSlice(SpatialKey(col,row), Center)

    addSlice(SpatialKey(col-1, row), Right)
    addSlice(SpatialKey(col+1, row), Left)
    addSlice(SpatialKey(col, row-1), Bottom)
    addSlice(SpatialKey(col, row+1), Top)

    addSlice(SpatialKey(col-1, row-1), BottomRight)
    addSlice(SpatialKey(col+1, row-1), BottomLeft)
    addSlice(SpatialKey(col+1, row+1), TopLeft)
    addSlice(SpatialKey(col-1, row+1), TopRight)

    parts
  }

  def bufferWithNeighbors[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: Stitcher: ClassTag
  ](rdd: RDD[(K, Iterable[(Direction, V)])]): RDD[(K, BufferedTile[V])] = {
    rdd
      .flatMapValues { neighbors =>
        neighbors.find( _._1 == Center) map { case (_, centerTile) =>

            val bufferSizes =
              neighbors.foldLeft(BufferSizes(0, 0, 0, 0)) { (acc, tup) =>
                val (direction, slice) = tup
                direction match {
                  case Left        => acc.copy(left = slice.cols)
                  case Right       => acc.copy(right = slice.cols)
                  case Top         => acc.copy(top = slice.rows)
                  case Bottom      => acc.copy(bottom = slice.rows)
                  case _           => acc
                }
              }

          val pieces =
            neighbors.map { case (direction, slice) =>
              val (updateColMin, updateRowMin) =
                direction match {
                  case Center      => (bufferSizes.left, bufferSizes.top)
                  case Left        => (0, bufferSizes.top)
                  case Right       => (bufferSizes.left + centerTile.cols, bufferSizes.top)
                  case Top         => (bufferSizes.left, 0)
                  case Bottom      => (bufferSizes.left, bufferSizes.top + centerTile.rows)
                  case TopLeft     => (0, 0)
                  case TopRight    => (bufferSizes.left + centerTile.cols, 0)
                  case BottomLeft  => (0, bufferSizes.top + centerTile.rows)
                  case BottomRight => (bufferSizes.left + centerTile.cols, bufferSizes.top + centerTile.rows)
                }

              (slice, (updateColMin, updateRowMin))
          }

          val cols = centerTile.cols + bufferSizes.left + bufferSizes.right
          val rows = centerTile.rows + bufferSizes.top + bufferSizes.bottom

          val stitched = implicitly[Stitcher[V]].stitch(pieces, cols, rows)

          BufferedTile(stitched, GridBounds(bufferSizes.left, bufferSizes.top, cols - bufferSizes.right - 1, rows - bufferSizes.bottom - 1))
        }
    }
  }

  def apply[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: Stitcher: ClassTag: (? => CropMethods[V])
  ](rdd: RDD[(K, V)], bufferSize: Int): RDD[(K, BufferedTile[V])] =
    apply(rdd, bufferSize, GridBounds(Int.MinValue, Int.MinValue, Int.MaxValue, Int.MaxValue))

  def apply[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: Stitcher: ClassTag: (? => CropMethods[V])
  ](rdd: RDD[(K, V)], bufferSize: Int, layerBounds: GridBounds): RDD[(K, BufferedTile[V])] = {
    val bufferSizes = BufferSizes(bufferSize, bufferSize, bufferSize, bufferSize)
    val tilesAndSlivers =
      rdd
        .flatMap { case (key, tile) => 
          collectWithNeighbors(key, tile, { key => layerBounds.contains(key.col, key.row) }, { key => bufferSizes })
        }

    val grouped =
      rdd.partitioner match {
        case Some(partitioner) => tilesAndSlivers.groupByKey(partitioner)
        case None => tilesAndSlivers.groupByKey
      }

    bufferWithNeighbors(grouped)
  }

  def apply[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: Stitcher: ClassTag: (? => CropMethods[V])
  ](rdd: RDD[(K, V)], getBufferSizes: K => BufferSizes): RDD[(K, BufferedTile[V])] = {
    val bufferSizesPerKey =
      rdd
        .mapPartitions({ partition =>
          partition.map { case (key, _) => (key, getBufferSizes(key)) }
        }, preservesPartitioning = true)
        .persist(StorageLevel.MEMORY_ONLY)
 
    val result = apply(rdd, bufferSizesPerKey)
    bufferSizesPerKey.unpersist(blocking = false)
    result
  }

  def apply[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: Stitcher: ClassTag: (? => CropMethods[V])
  ](rdd: RDD[(K, V)], bufferSizesPerKey: RDD[(K, BufferSizes)]): RDD[(K, BufferedTile[V])] = {
    val surroundingBufferSizes: RDD[(K, Map[SpatialKey, BufferSizes])] = {
      val contributingKeys =
        bufferSizesPerKey
          .flatMap { case (key, bufferSizes) =>
            val spatialKey @ SpatialKey(col, row) = key.spatialComponent
            Seq(
              (key, (spatialKey, bufferSizes)),

              (key.updateSpatialComponent(SpatialKey(col-1, row)), (spatialKey, bufferSizes)),
              (key.updateSpatialComponent(SpatialKey(col+1, row)), (spatialKey, bufferSizes)),
              (key.updateSpatialComponent(SpatialKey(col, row-1)), (spatialKey, bufferSizes)),
              (key.updateSpatialComponent(SpatialKey(col, row+1)), (spatialKey, bufferSizes)),

              (key.updateSpatialComponent(SpatialKey(col-1, row-1)), (spatialKey, bufferSizes)),
              (key.updateSpatialComponent(SpatialKey(col+1, row-1)), (spatialKey, bufferSizes)),
              (key.updateSpatialComponent(SpatialKey(col+1, row+1)), (spatialKey, bufferSizes)),
              (key.updateSpatialComponent(SpatialKey(col-1, row+1)), (spatialKey, bufferSizes))
            )

          }

      val grouped = 
        rdd.partitioner match {
          case Some(partitioner) => contributingKeys.groupByKey(partitioner)
          case None => contributingKeys.groupByKey
        }

      grouped
        .mapValues { _.toMap }
    }

    val tilesAndSlivers =
      rdd
        .join(surroundingBufferSizes)
        .flatMap { case (key, (tile, bufferSizesMap)) => 
          collectWithNeighbors(key, tile, bufferSizesMap.contains _, bufferSizesMap)
        }

    val grouped =
      rdd.partitioner match {
        case Some(partitioner) => tilesAndSlivers.groupByKey(partitioner)
        case None => tilesAndSlivers.groupByKey
      }

    bufferWithNeighbors(grouped)
  }
}
