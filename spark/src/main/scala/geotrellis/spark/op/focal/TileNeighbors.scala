package geotrellis.spark.op.focal

import geotrellis.raster._

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
