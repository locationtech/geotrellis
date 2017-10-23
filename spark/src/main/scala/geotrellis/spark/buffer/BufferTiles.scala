/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.buffer

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.prototype._
import geotrellis.raster.stitch._
import geotrellis.util._

import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel

import scala.reflect.ClassTag
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

import Direction._

object BufferTiles {

  /** Collects tile neighbors by slicing the neighboring tiles to the given
    * buffer size
    */
  def collectWithTileNeighbors[K: SpatialComponent, V <: CellGrid: (? => CropMethods[V])](
    key: K,
    tile: V,
    includeKey: SpatialKey => Boolean,
    getBufferSizes: SpatialKey => BufferSizes
  ): Seq[(K, (Direction, V))] = {
    val SpatialKey(col, row) = key.getComponent[SpatialKey]
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
    K: SpatialComponent: ClassTag,
    V <: CellGrid: Stitcher: ClassTag
  ](rdd: RDD[(K, Iterable[(Direction, V)])]): RDD[(K, BufferedTile[V])] = {
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

          BufferedTile(stitched, GridBounds(bufferSizes.left, bufferSizes.top, cols - bufferSizes.right - 1, rows - bufferSizes.bottom - 1))
        }
    }
    r
  }

  def bufferWithNeighbors[
    K: SpatialComponent,
    V <: CellGrid: Stitcher
  ](seq: Seq[(K, Seq[(Direction, V)])]): Seq[(K, BufferedTile[V])] = {
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

          BufferedTile(stitched, GridBounds(bufferSizes.left, bufferSizes.top, cols - bufferSizes.right - 1, rows - bufferSizes.bottom - 1))
        }

        if(opt.isEmpty) None else Some(key -> opt.get)
      }
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
    V <: CellGrid: Stitcher: ClassTag: (? => CropMethods[V])
  ](rdd: RDD[(K, V)], bufferSize: Int): RDD[(K, BufferedTile[V])] =
    apply(rdd, bufferSize, GridBounds(Int.MinValue, Int.MinValue, Int.MaxValue, Int.MaxValue))

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
    V <: CellGrid: Stitcher: (? => CropMethods[V])
  ](rdd: Seq[(K, V)], bufferSize: Int): Seq[(K, BufferedTile[V])] =
  apply(rdd, bufferSize, GridBounds(Int.MinValue, Int.MinValue, Int.MaxValue, Int.MaxValue))

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
    V <: CellGrid: Stitcher: ClassTag: (? => CropMethods[V])
  ](rdd: RDD[(K, V)], bufferSize: Int, layerBounds: GridBounds): RDD[(K, BufferedTile[V])] = {
    val bufferSizes = BufferSizes(bufferSize, bufferSize, bufferSize, bufferSize)
    val tilesAndSlivers =
      rdd
        .flatMap { case (key, tile) =>
          collectWithTileNeighbors(key, tile, { key => layerBounds.contains(key.col, key.row) }, { key => bufferSizes })
        }

    val grouped =
      rdd.partitioner match {
        case Some(partitioner) => tilesAndSlivers.groupByKey(partitioner)
        case None => tilesAndSlivers.groupByKey
      }

    bufferWithNeighbors(grouped)
  }

  /** Buffer the tiles of type V by a dynamic buffer size.
    *
    * This function will return each of the tiles with a buffer added to them by the contributions of adjacent, abutting tiles.
    *
    * @tparam         K                 The key of this tile set RDD, requiring a spatial component.
    * @tparam         V                 The tile type, requires a Stitcher[V] and implicit conversion to CropMethods[V]
    *
    * @param          rdd               The keyed tile rdd.
    * @param          getBufferSizes    A function which returns the BufferSizes that should be used for a tile at this Key.
    */
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
    V <: CellGrid: Stitcher: (? => CropMethods[V])
  ](seq: Seq[(K, V)], getBufferSizes: K => BufferSizes): Seq[(K, BufferedTile[V])] =
    apply(seq, seq.map { case (key, _) =>  key -> getBufferSizes(key) })

  /** Buffer the tiles of type V by a dynamic buffer size.
    *
    * This function will return each of the tiles with a buffer added to them by the contributions of adjacent, abutting tiles.
    *
    * @tparam         K                        The key of this tile set RDD, requiring a spatial component.
    * @tparam         V                        The tile type, requires a Stitcher[V] and implicit conversion to CropMethods[V]
    *
    * @param          rdd                      The keyed tile rdd.
    * @param          bufferSizesPerKey        An RDD that holds the BufferSizes to use for each key.
    */
  def apply[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: Stitcher: ClassTag: (? => CropMethods[V])
  ](rdd: RDD[(K, V)], bufferSizesPerKey: RDD[(K, BufferSizes)]): RDD[(K, BufferedTile[V])] = {
    val surroundingBufferSizes: RDD[(K, Map[SpatialKey, BufferSizes])] = {
      val contributingKeys: RDD[(K, (SpatialKey, BufferSizes))] =
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

      val grouped: RDD[(K, Iterable[(SpatialKey, BufferSizes)])] =
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
          collectWithTileNeighbors(key, tile, bufferSizesMap.contains _, bufferSizesMap)
        }

    val grouped =
      rdd.partitioner match {
        case Some(partitioner) => tilesAndSlivers.groupByKey(partitioner)
        case None => tilesAndSlivers.groupByKey
      }

    bufferWithNeighbors(grouped)
  }

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
    V <: CellGrid: Stitcher: (? => CropMethods[V])
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

    val grouped: Seq[(K, Seq[(Direction, V)])] =
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
  V <: CellGrid: Stitcher: (? => CropMethods[V])
  ](seq: Seq[(K, V)], bufferSize: Int, layerBounds: GridBounds): Seq[(K, BufferedTile[V])] = {
    val bufferSizes = BufferSizes(bufferSize, bufferSize, bufferSize, bufferSize)
    val grouped: Seq[(K, Seq[(Direction, V)])] =
      seq
        .flatMap { case (key, tile) =>
          collectWithTileNeighbors(key, tile, { key => layerBounds.contains(key.col, key.row) }, { key => bufferSizes })
        }.groupBy(_._1).mapValues { _.map(_._2) }.toSeq

    bufferWithNeighbors(grouped)
  }

  def apply[
    K: SpatialComponent: Boundable: ClassTag,
    V <: CellGrid: Stitcher: (? => CropMethods[V]): (? => TilePrototypeMethods[V]),
    M: GetComponent[?, LayoutDefinition]: GetComponent[?, Bounds[K]]
  ](layer: RDD[(K, V)] with Metadata[M], getBufferSizes: K => Option[BufferSizes]): RDD[(K, BufferedTile[V])] = {
    def dirToOffset(dir: Direction, xOfs: Int, yOfs: Int) = {
      val (x, y) = dir match {
        case TopLeft     => (0,0)
        case Top         => (1,0)
        case TopRight    => (2,0)
        case Left        => (0,1)
        case Center      => (1,1)
        case Right       => (2,1)
        case BottomLeft  => (0,2)
        case Bottom      => (1,2)
        case BottomRight => (2,2)
      }
      (x + xOfs, y + yOfs)
    }

    layer
      .flatMap{ case (key, tile) => {
        val SpatialKey(x, y) = key.getComponent[SpatialKey]
        val cols = tile.cols
        val rows = tile.rows

        def bufferForNeighbor(dir: Direction): Option[(K, (K, Direction, V))] = dir match {
          case Center => Some(key -> (key, Center, tile))
          case Left => 
            val k = key.setComponent(SpatialKey(x-1, y))
            for ( bs <- getBufferSizes(k) ;
                  slice <- Some(tile.crop(0, 0, bs.right - 1, rows - 1, Crop.Options(force = true))) ;
                  res <- Some( k -> (k, Right, slice) )
                ) yield res
          case Right => 
            val k = key.setComponent(SpatialKey(x+1, y))
            for ( bs <- getBufferSizes(k) ;
                  slice <- Some(tile.crop(cols - bs.left, 0, cols - 1, rows - 1, Crop.Options(force = true))) ;
                  res <- Some( k -> (k, Left, slice) )
                ) yield res
          case Top => 
            val k = key.setComponent(SpatialKey(x, y-1))
            for ( bs <- getBufferSizes(k) ;
                  slice <- Some(tile.crop(0, 0, cols - 1, bs.bottom - 1, Crop.Options(force = true))) ;
                  res <- Some( k -> (k, Bottom, slice) )
                ) yield res
          case Bottom => 
            val k = key.setComponent(SpatialKey(x, y+1))
            for ( bs <- getBufferSizes(k) ;
                  slice <- Some(tile.crop(0, rows - bs.top, cols - 1, rows - 1, Crop.Options(force = true))) ;
                  res <- Some( k -> (k, Top, slice) )
                ) yield res
          case TopLeft => 
            val k = key.setComponent(SpatialKey(x-1, y-1))
            for ( bs <- getBufferSizes(k) ;
                  slice <- Some(tile.crop(0, 0, bs.right - 1, bs.bottom - 1, Crop.Options(force = true))) ;
                  res <- Some( k -> (k, BottomRight, slice) )
                ) yield res
          case TopRight => 
            val k = key.setComponent(SpatialKey(x+1, y-1))
            for ( bs <- getBufferSizes(k) ;
                  slice <- Some(tile.crop(cols - bs.left, 0, cols - 1, bs.bottom - 1, Crop.Options(force = true))) ;
                  res <- Some( k -> (k, BottomLeft, slice) )
                ) yield res
          case BottomLeft => 
            val k = key.setComponent(SpatialKey(x-1, y+1))
            for ( bs <- getBufferSizes(k) ;
                  slice <- Some(tile.crop(0, rows - bs.top, bs.right - 1, rows - 1, Crop.Options(force = true))) ;
                  res <- Some( k -> (k, TopRight, slice) )
                ) yield res
          case BottomRight => 
            val k = key.setComponent(SpatialKey(x+1, y+1))
            for ( bs <- getBufferSizes(k) ;
                  slice <- Some(tile.crop(cols - bs.left, rows - bs.top, cols - 1, rows - 1, Crop.Options(force = true))) ;
                  res <- Some( k -> (k, TopLeft, slice) )
                ) yield res
        }

        Seq(Center, Left, Right, Bottom, Top, TopLeft, TopRight, BottomLeft, BottomRight).flatMap(bufferForNeighbor): Seq[(K, (K, Direction, V))]
      }}
      // .combineByKey(
      //   { case tup => 
      //     val (key, dir, tile) = tup
      //     val bs = getBufferSizes(key)
      //     val newtile = tile.prototype(tile.cols + bs.left + bs.right, tile.rows + bs.top + bs.bottom)
      //     val ex = Extent(-bs.left, -bs.bottom, cols + bs.right, rows + bs.top, cols + bs.right)
      //     (ex, newtile.merge(ex, dirToExtent(dir, bs, tile.cols, tile.rows), tile))
      //   },
      //   { case (combined, tup) =>
      //     val (ex, whole) = combined
      //     val (key, dir, tile) = tup
      //     (ex, whole.merge(ex, dirToExtent(dir, getBufferSizes(key), ti
      //   },
      //   { (t1, t2) => t1.merge(t2) }
      // )
      // .mapValues(_._2)
      .groupByKey
      .mapValues{ iter =>
        val stitcher = implicitly[Stitcher[V]]
        val seq = iter.toSeq
        val pieces = seq.map{ case (_, dir, tile) => (dir, tile) }.toMap
        val key = seq.head._1

        val lefts = 
          Seq(Seq(TopLeft, Left, BottomLeft), Seq(Top, Center, Bottom), Seq(TopRight, Right, BottomRight))
            .map(_.map{ x => pieces.get(x).map(_.cols).getOrElse(0) }.max)
            .foldLeft(Seq(0)){ (acc, x) => acc :+ (acc.last + x) }
        val tops = 
          Seq(Seq(TopLeft, Top, TopRight), Seq(Left, Center, Right), Seq(BottomLeft, Bottom, BottomRight))
            .map(_.map{ x => pieces.get(x).map(_.rows).getOrElse(0) }.max)
            .foldLeft(Seq(0)){ (acc, x) => acc :+ (acc.last + x) }

        val totalWidth = lefts.last
        val totalHeight = tops.last

        def loc(dir: Direction) = dir match {
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

        val toStitch = pieces.map{ case (dir, tile) => (tile, loc(dir)) }

        println(s"For $key: stitching $pieces into $totalWidth x $totalHeight tile")

        val stitched = stitcher.stitch(toStitch, totalWidth, totalHeight)

        val bufferSizes = getBufferSizes(key).get
        BufferedTile(stitched, GridBounds(bufferSizes.left, bufferSizes.top, pieces(Center).cols - bufferSizes.right - 1, pieces(Center).rows - bufferSizes.bottom - 1))
      }
  }
}
