/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster

import geotrellis._
import scala.collection.concurrent.Map
import java.util.concurrent.atomic.AtomicInteger

import scala.collection.mutable

object TileNeighbors {
  val NONE = new TileNeighbors {
    def n: Option[Op[Tile]] = None 
    def ne: Option[Op[Tile]] = None 
    def e: Option[Op[Tile]] = None 
    def se: Option[Op[Tile]] = None 
    def s: Option[Op[Tile]] = None 
    def sw: Option[Op[Tile]] = None 
    def w: Option[Op[Tile]] = None 
    def nw: Option[Op[Tile]] = None 

    def getNeighbors: Op[Seq[Option[Tile]]] = Literal(Seq[Option[Tile]]())
  }
}

trait TileNeighbors {
  /** North */
  def n: Option[Op[Tile]]
  /** NorthEast */
  def ne: Option[Op[Tile]]
  /** East */
  def e: Option[Op[Tile]]
  /** SoutEast */
  def se: Option[Op[Tile]]
  /** South */
  def s: Option[Op[Tile]]
  /** SouthWest */
  def sw: Option[Op[Tile]]
  /** West */
  def w: Option[Op[Tile]]
  /** NorthWest */
  def nw: Option[Op[Tile]]

  def getNeighbors: Op[Seq[Option[Tile]]]
}

/** 
 * Tile Neighbors that are represented by a sequence of neighboring tiles,
 * in the order (n, ne, e, se, s, sw, w, nw)
 */
case class SeqTileNeighbors(seq: Seq[Option[Op[Tile]]]) extends TileNeighbors {
  def n = seq(0)
  def ne = seq(1)
  def e = seq(2)
  def se = seq(3)
  def s = seq(4)
  def sw = seq(5)
  def w = seq(6)
  def nw = seq(7)

  def getNeighbors =
    if(seq.flatten.isEmpty) { Literal(Seq[Option[Tile]]()) }
    else {
      logic.Collect(
        seq.map {
          case Some(op) => op.map(Some(_))
          case None => Literal(None)
        }
      )
    }
}
