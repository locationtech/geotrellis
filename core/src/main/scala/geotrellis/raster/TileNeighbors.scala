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
    def n:Option[Op[Raster]] = None 
    def ne:Option[Op[Raster]] = None 
    def e:Option[Op[Raster]] = None 
    def se:Option[Op[Raster]] = None 
    def s:Option[Op[Raster]] = None 
    def sw:Option[Op[Raster]] = None 
    def w:Option[Op[Raster]] = None 
    def nw:Option[Op[Raster]] = None 

    def getNeighbors:Op[Seq[Option[Raster]]] = Literal(Seq[Option[Raster]]())
  }
}

trait TileNeighbors {
  /** North */
  def n:Option[Op[Raster]]
  /** NorthEast */
  def ne:Option[Op[Raster]]
  /** East */
  def e:Option[Op[Raster]]
  /** SoutEast */
  def se:Option[Op[Raster]]
  /** South */
  def s:Option[Op[Raster]]
  /** SouthWest */
  def sw:Option[Op[Raster]]
  /** West */
  def w:Option[Op[Raster]]
  /** NorthWest */
  def nw:Option[Op[Raster]]

  def getNeighbors:Op[Seq[Option[Raster]]]
}

/** 
 * Tile Neighbors that are represented by a sequence of neighboring tiles,
 * in the order (n,ne,e,se,s,sw,w,nw)
 */
case class SeqTileNeighbors(seq:Seq[Option[Op[Raster]]]) extends TileNeighbors {
  def n = seq(0)
  def ne = seq(1)
  def e = seq(2)
  def se = seq(3)
  def s = seq(4)
  def sw = seq(5)
  def w = seq(6)
  def nw = seq(7)

  def getNeighbors =
    if(seq.flatten.isEmpty) { Literal(Seq[Option[Raster]]()) }
    else {
      logic.Collect(
        seq.map {
          case Some(op) => op.map(Some(_))
          case None => Literal(None)
        }
      )
    }
}
