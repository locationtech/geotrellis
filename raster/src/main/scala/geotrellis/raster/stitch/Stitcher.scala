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

package geotrellis.raster.stitch

import geotrellis.raster._
import cats.Semigroup

/**
  * The Stitcher base trait.
  */
trait Stitcher[T <: CellGrid] extends Serializable {

  /**
    * Stitch an Iterable of tile, corner pairs into a new tile with
    * the given number of columns and rows.
    *
    * The parameter 'pieces' is an iterable of (T, (Int, Int)) tuples,
    * were the first item in the tuple is a tile and the second one is
    * the position that the tile should occupy (given by smallest
    * column and smallest row).
    *
    * @param   pieces  An iterable of tile, corner pairs
    * @param   cols    The number of columns in the result tile
    * @param   rows    The number of rows in the result tile
    * @return          The result tile
    */
  def stitch(pieces: Iterable[(T, (Int, Int))], cols: Int, rows: Int): T
}

/**
  * Stitcher object.
  */
object Stitcher {

  /**
    * Implicit object holding [[Tile]] stitcher function.
    */
  implicit object TileStitcher extends Stitcher[Tile] {

  /**
    * Stitch an Iterable of [[Tile]], corner pairs into a new Tile
    * with the given number of columns and rows.
    *
    * The parameter 'pieces' is an iterable of (T, (Int, Int)) tuples,
    * were the first item in the tuple is a Tile and the second one is
    * the position that the Tile should occupy (given by smallest
    * column and smallest row).
    *
    * @param   pieces  An iterable of Tile, corner pairs
    * @param   cols    The number of columns in the result Tile
    * @param   rows    The number of rows in the result Tile
    * @return          The result Tile
    */
    def stitch(pieces: Iterable[(Tile, (Int, Int))], cols: Int, rows: Int): Tile = {
      val result = ArrayTile.empty(pieces.head._1.cellType, cols, rows)
      for((tile, (updateColMin, updateRowMin)) <- pieces) {
        result.update(updateColMin, updateRowMin, tile)
      }
      result
    }
  }

  /**
    * Implicit object holding [[MultibandTile]] stitcher function.
    */
  implicit object MultibandTileStitcher extends Stitcher[MultibandTile] {

  /**
    * Stitch an Iterable of [[MultibandTile]], corner pairs into a new
    * MultibandTile with the given number of columns and rows.
    *
    * The parameter 'pieces' is an iterable of (T, (Int, Int)) tuples,
    * were the first item in the tuple is a MultibandTile and the
    * second one is the position that the MultibandTile should occupy
    * (given by smallest column and smallest row).
    *
    * @param   pieces  An iterable of MultibandTile, corner pairs
    * @param   cols    The number of columns in the result MultibandTile
    * @param   rows    The number of rows in the result MultibandTile
    * @return          The result MultibandTile
    */
    def stitch(pieces: Iterable[(MultibandTile, (Int, Int))], cols: Int, rows: Int): MultibandTile = {
      val headTile = pieces.head._1
      val bands = Array.fill[MutableArrayTile](headTile.bandCount)(ArrayTile.empty(headTile.cellType, cols, rows))

      for ((tile, (updateColMin, updateRowMin)) <- pieces) {
        for(b <- 0 until headTile.bandCount) {
          bands(b).update(updateColMin, updateRowMin, tile.band(b))
        }
      }

      ArrayMultibandTile(bands)
    }
  }

  implicit class TileFeatureStitcher[
    T <: CellGrid: Stitcher,
    D : Semigroup
  ](val self: TileFeature[T, D]) extends Stitcher[TileFeature[T, D]] {
    def stitch(pieces: Iterable[(TileFeature[T, D], (Int, Int))], cols: Int, rows: Int): TileFeature[T, D] = {
      val newPieces = pieces.map{ piece ⇒ (piece._1.tile, piece._2) }
      val newData = pieces.map(_._1.data).reduce(Semigroup[D].combine)
      TileFeature(implicitly[Stitcher[T]].stitch(newPieces, cols, rows), newData)
    }
  }
}
