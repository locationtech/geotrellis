package geotrellis.spark.op.focal

import geotrellis.raster._
import geotrellis.spark.SpatialKey

trait TileNeighbors extends Serializable {

  /** North */
  def n: Option[Tile]
  /** NorthEast */
  def ne: Option[Tile]
  /** East */
  def e: Option[Tile]
  /** SoutEast */
  def se: Option[Tile]
  /** South */
  def s: Option[Tile]
  /** SouthWest */
  def sw: Option[Tile]
  /** West */
  def w: Option[Tile]
  /** NorthWest */
  def nw: Option[Tile]

  def getNeighbors: Seq[Option[Tile]]

}

case class SeqTileNeighbors(seq: Seq[Option[Tile]]) extends TileNeighbors {

  def n = seq(0)
  def ne = seq(1)
  def e = seq(2)
  def se = seq(3)
  def s = seq(4)
  def sw = seq(5)
  def w = seq(6)
  def nw = seq(7)

  def getNeighbors: Seq[Option[Tile]] = seq

}

object SeqTileNeighbors {
  def fromKeys(center: SpatialKey, pairs: TraversableOnce[(SpatialKey, Tile)]) = {
    val seq = Array.fill[Option[Tile]](8)(None)
    pairs.foreach { case (SpatialKey(col, row), tile) =>
      val dCol = col - center.col
      val dRow = row - center.row
      if (dRow == -1 && dCol ==  0) seq(0) = Some(tile)
      else if (dRow == -1 && dCol ==  1) seq(1) = Some(tile)
      else if (dRow ==  0 && dCol ==  1) seq(2) = Some(tile)
      else if (dRow ==  1 && dCol ==  1) seq(3) = Some(tile)
      else if (dRow ==  1 && dCol ==  0) seq(4) = Some(tile)
      else if (dRow ==  1 && dCol == -1) seq(5) = Some(tile)
      else if (dRow ==  0 && dCol == -1) seq(6) = Some(tile)
      else if (dRow == -1 && dCol == -1) seq(7) = Some(tile)
    }
    SeqTileNeighbors(seq)
  }  
}

