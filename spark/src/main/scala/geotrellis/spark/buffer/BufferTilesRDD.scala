package geotrellis.spark.buffer

import geotrellis.tiling._
import geotrellis.raster
import geotrellis.raster._
import geotrellis.raster.buffer.{BufferSizes, BufferedTile}
import geotrellis.raster.buffer.Direction._
import geotrellis.raster.crop._
import geotrellis.raster.stitch._
import geotrellis.layers.TileLayerMetadata
import geotrellis.layers.buffer.BufferTiles
import geotrellis.spark._
import geotrellis.util._

import org.apache.log4j.Logger
import org.apache.spark.rdd.RDD
import org.apache.spark.Partitioner

import scala.reflect.ClassTag


object BufferTilesRDD extends BufferTiles {
  override val logger = Logger.getLogger(BufferTilesRDD.getClass)

  def bufferWithNeighbors[
    K: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: Stitcher: ClassTag
  ](rdd: RDD[(K, Iterable[(raster.buffer.Direction, V)])]): RDD[(K, BufferedTile[V])] = {
    val r = rdd
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
    }
    r
  }

  /** Buffer the tiles of type V by a constant buffer size.
    *
    * This function will return each of the tiles with a buffer added to them by the contributions of adjacent, abutting tiles.
    *
    * @tparam         K                 The key of this tile set RDD, requiring a spatial component.
    * @tparam         V                 The tile type, requires a Stitcher[V] and implicit conversion to CropMethods[V]
    *
    * @param          rdd               The keyed tile rdd.
    * @param          bufferSize        Number of pixels to buffer the tile with. The tile will only be buffered by this amount on
    *                                   any side if there is an adjacent, abutting tile to contribute the border pixels.
    */
  def apply[
    K: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: Stitcher: ClassTag: (? => CropMethods[V])
  ](
    rdd: RDD[(K, V)],
    bufferSize: Int
  ): RDD[(K, BufferedTile[V])] =
    apply(rdd, { _: K => BufferSizes(bufferSize, bufferSize, bufferSize, bufferSize) }, None)

  /** Buffer the tiles of type V by a constant buffer size.
    *
    * This function will return each of the tiles with a buffer added to them by the contributions of adjacent, abutting tiles.
    *
    * @tparam         K                 The key of this tile set RDD, requiring a spatial component.
    * @tparam         V                 The tile type, requires a Stitcher[V] and implicit conversion to CropMethods[V]
    *
    * @param          rdd               The keyed tile rdd.
    * @param          bufferSize        Number of pixels to buffer the tile with. The tile will only be buffered by this amount on
    *                                   any side if there is an adjacent, abutting tile to contribute the border pixels.
    * @param          partitioner       The Partitioner to use when buffering over the tiles. If None, then the parent's Partitioner
    *                                   will be used. If that is also None then the resulting RDD will have a HashPartitioner.
    */
  def apply[
    K: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: Stitcher: ClassTag: (? => CropMethods[V])
  ](
    rdd: RDD[(K, V)],
    bufferSize: Int,
    partitioner: Option[Partitioner]
  ): RDD[(K, BufferedTile[V])] =
    apply(rdd, { _: K => BufferSizes(bufferSize, bufferSize, bufferSize, bufferSize) }, partitioner)

  /** Buffer the tiles of type V by a constant buffer size.
    *
    * This function will return each of the tiles with a buffer added to them by the contributions of adjacent, abutting tiles.
    *
    * @tparam         K                 The key of this tile set RDD, requiring a spatial component.
    * @tparam         V                 The tile type, requires a Stitcher[V] and implicit conversion to CropMethods[V]
    *
    * @param          rdd               The keyed tile rdd.
    * @param          bufferSize        Number of pixels to buffer the tile with. The tile will only be buffered by this amount on
    *                                   any side if there is an adjacent, abutting tile to contribute the border pixels.
    * @param          layerBounds       The boundries of the layer to consider for border pixel contribution. This avoids creating
    *                                   border cells from valid tiles that would be used by keys outside of the bounds (and therefore
    *                                   unused).
    */
  def apply[
    K: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: Stitcher: ClassTag: (? => CropMethods[V])
  ](
    rdd: RDD[(K, V)],
    bufferSize: Int,
    layerBounds: TileBounds
  ): RDD[(K, BufferedTile[V])] =
    apply(rdd, bufferSize, layerBounds, None)

  /** Buffer the tiles of type V by a constant buffer size.
    *
    * This function will return each of the tiles with a buffer added to them by the contributions of adjacent, abutting tiles.
    *
    * @tparam         K                 The key of this tile set RDD, requiring a spatial component.
    * @tparam         V                 The tile type, requires a Stitcher[V] and implicit conversion to CropMethods[V]
    *
    * @param          rdd               The keyed tile rdd.
    * @param          bufferSize        Number of pixels to buffer the tile with. The tile will only be buffered by this amount on
    *                                   any side if there is an adjacent, abutting tile to contribute the border pixels.
    * @param          layerBounds       The boundries of the layer to consider for border pixel contribution. This avoids creating
    *                                   border cells from valid tiles that would be used by keys outside of the bounds (and therefore
    *                                   unused).
    *                                   any side if there is an adjacent, abutting tile to contribute the border pixels.
    *
    * @param          partitioner       The Partitioner to use when buffering over the tiles. If None, then the parent's Partitioner
    *                                   will be used. If that is also None then the resulting RDD will have a HashPartitioner.
    */
  def apply[
    K: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: Stitcher: ClassTag: (? => CropMethods[V])
  ](
    rdd: RDD[(K, V)],
    bufferSize: Int,
    layerBounds: TileBounds,
    partitioner: Option[Partitioner]
  ): RDD[(K, BufferedTile[V])] =
    apply(
      rdd,
      { key: K =>
        val k = key.getComponent[SpatialKey]()
        layerBounds.contains(k.col, k.row)
      },
      { _: K => BufferSizes(bufferSize, bufferSize, bufferSize, bufferSize) },
      partitioner
    )

  /** Buffer the tiles of type V by a constant buffer size.
    *
    * This function will return each of the tiles with a buffer added to them by the contributions of adjacent, abutting tiles.
    *
    * @tparam         K                 The key of this tile set RDD, requiring a spatial component.
    * @tparam         V                 The tile type, requires a Stitcher[V] and implicit conversion to CropMethods[V]
    *
    * @param          rdd               The keyed tile rdd.
    * @param          getBufferSizes    A function indicating the number of pixels to buffer the tile with. The tile will only
    *                                   be buffered by this amount on any side if there is an adjacent, abutting tile to
    *                                   contribute the border pixels.
    */
  def apply[
    K: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: Stitcher: (? => CropMethods[V])
  ](
    layer: RDD[(K, V)],
    getBufferSizes: K => BufferSizes
  ): RDD[(K, BufferedTile[V])] =
    apply(layer, getBufferSizes, None)

  /** Buffer the tiles of type V by a constant buffer size.
    *
    * This function will return each of the tiles with a buffer added to them by the contributions of adjacent, abutting tiles.
    *
    * @tparam         K                 The key of this tile set RDD, requiring a spatial component.
    * @tparam         V                 The tile type, requires a Stitcher[V] and implicit conversion to CropMethods[V]
    *
    * @param          rdd               The keyed tile rdd.
    * @param          getBufferSizes    A function indicating the number of pixels to buffer the tile with. The tile will only
    *                                   be buffered by this amount on any side if there is an adjacent, abutting tile to
    *                                   contribute the border pixels.
    *
    * @param          partitioner       The Partitioner to use when buffering over the tiles. If None, then the parent's Partitioner
    *                                   will be used. If that is also None then the resulting RDD will have a HashPartitioner.
    */
  def apply[
    K: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: Stitcher: (? => CropMethods[V])
  ](
    layer: RDD[(K, V)],
    getBufferSizes: K => BufferSizes,
    partitioner: Option[Partitioner]
  ): RDD[(K, BufferedTile[V])] =
    apply(layer, { _: K => true }, getBufferSizes, partitioner)

  /** Buffer the tiles of type V by a constant buffer size.
    *
    * This function will return each of the tiles with a buffer added to them by the contributions of adjacent, abutting tiles.
    *
    * @tparam         K                 The key of this tile set RDD, requiring a spatial component.
    * @tparam         V                 The tile type, requires a Stitcher[V] and implicit conversion to CropMethods[V]
    *
    * @param          rdd               The keyed tile rdd.
    * @param          includeKey        A function indicating whether the tile corresponding to a key should be included in the
    *                                   output.
    * @param          getBufferSizes    A function indicating the number of pixels to buffer the tile with. The tile will only
    *                                   be buffered by this amount on any side if there is an adjacent, abutting tile to
    *                                   contribute the border pixels.
    */
  def apply[
    K: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: Stitcher: (? => CropMethods[V])
  ](layer: RDD[(K, V)],
    includeKey: K => Boolean,
    getBufferSizes: K => BufferSizes
  ): RDD[(K, BufferedTile[V])] =
    apply(layer, includeKey, getBufferSizes, None)

  /** Buffer the tiles of type V by a constant buffer size.
    *
    * This function will return each of the tiles with a buffer added to them by the contributions of adjacent, abutting tiles.
    *
    * @tparam         K                 The key of this tile set RDD, requiring a spatial component.
    * @tparam         V                 The tile type, requires a Stitcher[V] and implicit conversion to CropMethods[V]
    *
    * @param          rdd               The keyed tile rdd.
    * @param          includeKey        A function indicating whether the tile corresponding to a key should be included in the
    *                                   output.
    * @param          getBufferSizes    A function indicating the number of pixels to buffer the tile with. The tile will only
    *                                   be buffered by this amount on any side if there is an adjacent, abutting tile to
    *                                   contribute the border pixels.
    *
    * @param          partitioner       The Partitioner to use when buffering over the tiles. If None, then the parent's Partitioner
    *                                   will be used. If that is also None then the resulting RDD will have a HashPartitioner.
    */
  def apply[
    K: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: Stitcher: (? => CropMethods[V])
  ](layer: RDD[(K, V)],
    includeKey: K => Boolean,
    getBufferSizes: K => BufferSizes,
    partitioner: Option[Partitioner]
  ): RDD[(K, BufferedTile[V])] = {

    val leftDirs = Seq(TopLeft, Left, BottomLeft)
    val hmidDirs = Seq(Top, Center, Bottom)
    val rightDirs = Seq(TopRight, Right, BottomRight)
    val topDirs = Seq(TopLeft, Top, TopRight)
    val vmidDirs = Seq(Left, Center, Right)
    val botDirs = Seq(BottomLeft, Bottom, BottomRight)

    val targetPartitioner =
      (layer.partitioner, partitioner) match {
        case (_, Some(_)) => partitioner
        case (Some(_), None) => layer.partitioner
        case (None, None) => None
      }

    val sliced = layer
      .flatMap{ case (key, tile) => {
        val SpatialKey(x, y) = key.getComponent[SpatialKey]
        val cols = tile.cols
        val rows = tile.rows

        def genSection(neighbor: raster.buffer.Direction): (K, (K, raster.buffer.Direction, V)) = {
          val (xOfs, yOfs) = DirectionOp.offsetOf(neighbor)
          val targetKey = key.setComponent(SpatialKey(x + xOfs, y + yOfs))
          val targetDir = DirectionOp.opp(neighbor)
          val bs = getBufferSizes(targetKey)
          val (l, r) = (leftDirs contains neighbor, hmidDirs contains neighbor, rightDirs contains neighbor) match {
            case (true, _, _) => (0, bs.right - 1)
            case (_, true, _) => (0, cols - 1)
            case (_, _, true) => (cols - bs.left, cols - 1)
            case _ => throw new IllegalStateException("Unreachable state")
          }
          val (t, b) = (topDirs contains neighbor, vmidDirs contains neighbor, botDirs contains neighbor) match {
            case (true, _, _) => (0, bs.bottom - 1)
            case (_, true, _) => (0, rows - 1)
            case (_, _, true) => (rows - bs.top, rows - 1)
            case _ => throw new IllegalStateException("Unreachable state")
          }
          logger.debug(s"Generating buffer for $targetKey -> $targetDir from $key over span [${(l, t)}, ${(r, b)}]")
          val slice = tile.crop(l, t, r, b, Crop.Options(force = true))
          targetKey -> (targetKey, targetDir, slice)
        }

        if (includeKey(key))
          Seq(Center, Left, Right, Bottom, Top, TopLeft, TopRight, BottomLeft, BottomRight).map(genSection): Seq[(K, (K, raster.buffer.Direction, V))]
        else
          Seq(Left, Right, Bottom, Top, TopLeft, TopRight, BottomLeft, BottomRight).map(genSection): Seq[(K, (K, raster.buffer.Direction, V))]
      }}

    val grouped =
      targetPartitioner match {
        case Some(p) => sliced.groupByKey(p)
        case None => sliced.groupByKey
      }

    grouped
      .flatMapValues{ iter =>
        val pieces = iter.map{ case (_, dir, tile) => (dir, tile) }.toMap

        if (pieces contains Center) {
          val lefts =
            Seq(leftDirs, hmidDirs, rightDirs)
              .map(_.map{ x => pieces.get(x).map(_.cols).getOrElse(0) }.max)
              .foldLeft(Seq(0)){ (acc, x) => acc :+ (acc.last + x) }
          val tops =
              Seq(topDirs, vmidDirs, botDirs)
                .map(_.map{ x => pieces.get(x).map(_.rows).getOrElse(0) }.max)
                .foldLeft(Seq(0)){ (acc, x) => acc :+ (acc.last + x) }

          val totalWidth = lefts.last
          val totalHeight = tops.last

          def loc(dir: raster.buffer.Direction) = dir match {
            case TopLeft     => (lefts(0), tops(0))
            case Top         => (lefts(1), tops(0))
            case TopRight    => (lefts(2), tops(0))
            case Left        => (lefts(0), tops(1))
            case Center      => (lefts(1), tops(1))
            case Right       => (lefts(2), tops(1))
            case BottomLeft  => (lefts(0), tops(2))
            case Bottom      => (lefts(1), tops(2))
            case BottomRight => (lefts(2), tops(2))
          }

          val toStitch = pieces.toSeq.map{ case (dir, tile) => (tile, loc(dir)) }

          val stitcher = implicitly[Stitcher[V]]
          val stitched = stitcher.stitch(toStitch, totalWidth, totalHeight)

          Some(BufferedTile(stitched, GridBounds(lefts(1), tops(1), lefts(2) - 1, tops(2) - 1)))
        } else {
          None
        }
      }
  }
}
