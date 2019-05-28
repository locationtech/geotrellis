package geotrellis.layers.buffer

import geotrellis.tiling._
import geotrellis.raster
import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.stitch._
import geotrellis.raster.buffer.{BufferedTile, BufferSizes}
import geotrellis.raster.buffer.Direction._
import geotrellis.util._

import org.apache.log4j.Logger

import scala.reflect.ClassTag
import scala.collection.mutable.ArrayBuffer

object BufferTiles extends BufferTiles

trait BufferTiles {
  val logger = Logger.getLogger(BufferTiles.getClass)

  /** Collects tile neighbors by slicing the neighboring tiles to the given
    * buffer size
    */
  def collectWithTileNeighbors[K: SpatialComponent, V <: CellGrid[Int]: (? => CropMethods[V])](
    key: K,
    tile: V,
    includeKey: SpatialKey => Boolean,
    getBufferSizes: SpatialKey => BufferSizes
  ): Seq[(K, (raster.buffer.Direction, V))] = {
    val SpatialKey(col, row) = key.getComponent[SpatialKey]
    val parts = new ArrayBuffer[(K, (raster.buffer.Direction, V))](9)

    val cols = tile.cols
    val rows = tile.rows

    // ex: adding "TopLeft" corner of this tile to contribute to "TopLeft" tile at key
    def addSlice(spatialKey: SpatialKey, direction: => raster.buffer.Direction) {
      if(includeKey(spatialKey)) {
        val bufferSizes = getBufferSizes(spatialKey)

        val part: V =
          direction match {
            case Center => tile
            case Right => tile.crop(0, 0, bufferSizes.right - 1, rows - 1, Crop.Options(force = true))
            case Left => tile.crop(cols - bufferSizes.left, 0, cols - 1, rows - 1, Crop.Options(force = true))
            case Top => tile.crop(0, rows - bufferSizes.top, cols - 1, rows - 1, Crop.Options(force = true))
            case Bottom => tile.crop(0, 0, cols - 1, bufferSizes.bottom - 1, Crop.Options(force = true))
            case TopLeft => tile.crop(cols - bufferSizes.left, rows - bufferSizes.top, cols - 1, rows - 1, Crop.Options(force = true))
            case TopRight => tile.crop(0, rows - bufferSizes.top, bufferSizes.right - 1, rows - 1, Crop.Options(force = true))
            case BottomLeft => tile.crop(cols - bufferSizes.left, 0, cols - 1, bufferSizes.bottom - 1, Crop.Options(force = true))
            case BottomRight => tile.crop(0, 0, bufferSizes.right - 1, bufferSizes.bottom - 1, Crop.Options(force = true))
          }

        parts += ( (key.setComponent(spatialKey), (direction, part)) )
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
    K: SpatialComponent,
    V <: CellGrid[Int]: Stitcher
  ](seq: Seq[(K, Seq[(raster.buffer.Direction, V)])]): Seq[(K, BufferedTile[V])] = {
    seq
      .flatMap { case (key, neighbors) =>
        val opt = neighbors.find(_._1 == Center).map { case (_, centerTile) =>

          val bufferSizes =
            neighbors.foldLeft(BufferSizes(0, 0, 0, 0)) { (acc, tup) =>
              val (direction, slice) = tup
              direction match {
                case Left        => acc.copy(left = slice.cols)
                case Right       => acc.copy(right = slice.cols)
                case Top        => acc.copy(top = slice.rows)
                case Bottom      => acc.copy(bottom = slice.rows)
                case BottomRight => acc.copy(bottom = slice.rows, right = slice.cols)
                case BottomLeft  => acc.copy(bottom = slice.rows, left = slice.cols)
                case TopRight    => acc.copy(top = slice.rows, right = slice.cols)
                case TopLeft     => acc.copy(top = slice.rows, left = slice.cols)
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

          raster.buffer.BufferedTile(stitched, GridBounds(bufferSizes.left, bufferSizes.top, cols - bufferSizes.right - 1, rows - bufferSizes.bottom - 1))
        }

        if(opt.isEmpty) None else Some(key -> opt.get)
      }
  }

  /** Buffer the tiles of type V by a constant buffer size.
    *
    * This function will return each of the tiles with a buffer added to them by the contributions of adjacent, abutting tiles.
    *
    * @tparam         K                 The key of this tile set Collection, requiring a spatial component.
    * @tparam         V                 The tile type, requires a Stitcher[V] and implicit conversion to CropMethods[V]
    *
    * @param          rdd               The keyed tile rdd.
    * @param          bufferSize        Number of pixels to buffer the tile with. The tile will only be buffered by this amount on
    *                                   any side if there is an adjacent, abutting tile to contribute the border pixels.
    */
  def apply[
    K: SpatialComponent,
    V <: CellGrid[Int]: Stitcher: (? => CropMethods[V])
  ](seq: Seq[(K, V)], bufferSize: Int): Seq[(K, BufferedTile[V])] =
    apply(seq, bufferSize, GridBounds(Int.MinValue, Int.MinValue, Int.MaxValue, Int.MaxValue))

  /** Buffer the tiles of type V by a dynamic buffer size.
    *
    * This function will return each of the tiles with a buffer added to them by the contributions of adjacent, abutting tiles.
    *
    * @tparam         K                 The key of this tile set Collection, requiring a spatial component.
    * @tparam         V                 The tile type, requires a Stitcher[V] and implicit conversion to CropMethods[V]
    *
    * @param          seq               The keyed tile rdd.
    * @param          getBufferSizes    A function which returns the BufferSizes that should be used for a tile at this Key.
    */
  def apply[
    K: SpatialComponent,
    V <: CellGrid[Int]: Stitcher: (? => CropMethods[V])
  ](seq: Seq[(K, V)], getBufferSizes: K => BufferSizes): Seq[(K, BufferedTile[V])] =
    apply(seq, seq.map { case (key, _) =>  key -> getBufferSizes(key) })

  /** Buffer the tiles of type V by a dynamic buffer size.
    *
    * This function will return each of the tiles with a buffer added to them by the contributions of adjacent, abutting tiles.
    *
    * @tparam         K                        The key of this tile set Collection, requiring a spatial component.
    * @tparam         V                        The tile type, requires a Stitcher[V] and implicit conversion to CropMethods[V]
    *
    * @param          seq                      The keyed tile rdd.
    * @param          bufferSizesPerKey        A Collection that holds the BufferSizes to use for each key.
    */
  def apply[
    K: SpatialComponent,
    V <: CellGrid[Int]: Stitcher: (? => CropMethods[V])
  ](seq: Seq[(K, V)], bufferSizesPerKey: Seq[(K, BufferSizes)]): Seq[(K, BufferedTile[V])] = {
    val surroundingBufferSizes: Seq[(K, Map[SpatialKey, BufferSizes])] = {
      val contributingKeys: Seq[(K, (SpatialKey, BufferSizes))] =
        bufferSizesPerKey
          .flatMap { case (key, bufferSizes) =>
            val spatialKey @ SpatialKey(col, row) = key.getComponent[SpatialKey]
            Seq(
              (key, (spatialKey, bufferSizes)),

              (key.setComponent(SpatialKey(col-1, row)), (spatialKey, bufferSizes)),
              (key.setComponent(SpatialKey(col+1, row)), (spatialKey, bufferSizes)),
              (key.setComponent(SpatialKey(col, row-1)), (spatialKey, bufferSizes)),
              (key.setComponent(SpatialKey(col, row+1)), (spatialKey, bufferSizes)),

              (key.setComponent(SpatialKey(col-1, row-1)), (spatialKey, bufferSizes)),
              (key.setComponent(SpatialKey(col+1, row-1)), (spatialKey, bufferSizes)),
              (key.setComponent(SpatialKey(col+1, row+1)), (spatialKey, bufferSizes)),
              (key.setComponent(SpatialKey(col-1, row+1)), (spatialKey, bufferSizes))
            )

          }

      contributingKeys.groupBy(_._1).mapValues { _.map(_._2).toMap }.toSeq
    }

    val grouped: Seq[(K, Seq[(raster.buffer.Direction, V)])] =
      seq.zip(surroundingBufferSizes).flatMap { case ((key, tile), (k2, bufferSizesMap)) =>
        collectWithTileNeighbors(key, tile, bufferSizesMap.contains _, bufferSizesMap)
      }.groupBy(_._1).mapValues(_.map(_._2)).toSeq

    bufferWithNeighbors(grouped)
  }

  /** Buffer the tiles of type V by a constant buffer size.
    *
    * This function will return each of the tiles with a buffer added to them by the contributions of adjacent, abutting tiles.
    *
    * @tparam         K                 The key of this tile set RDD, requiring a spatial component.
    * @tparam         V                 The tile type, requires a Stitcher[V] and implicit conversion to CropMethods[V]
    *
    * @param          seq               The keyed tile collection.
    * @param          bufferSize        Number of pixels to buffer the tile with. The tile will only be buffered by this amount on
    *                                   any side if there is an adjacent, abutting tile to contribute the border pixels.
    * @param          layerBounds       The boundries of the layer to consider for border pixel contribution. This avoids creating
    *                                   border cells from valid tiles that would be used by keys outside of the bounds (and therefore
    *                                   unused).
    */
  def apply[
    K: SpatialComponent,
    V <: CellGrid[Int]: Stitcher: (? => CropMethods[V])
  ](seq: Seq[(K, V)], bufferSize: Int, layerBounds: TileBounds): Seq[(K, BufferedTile[V])] = {
    val bufferSizes = BufferSizes(bufferSize, bufferSize, bufferSize, bufferSize)
    val grouped: Seq[(K, Seq[(raster.buffer.Direction, V)])] =
      seq
        .flatMap { case (key, tile) =>
          collectWithTileNeighbors(key, tile, { key => layerBounds.contains(key.col, key.row) }, { key => bufferSizes })
        }.groupBy(_._1).mapValues { _.map(_._2) }.toSeq

    bufferWithNeighbors(grouped)
  }
}
