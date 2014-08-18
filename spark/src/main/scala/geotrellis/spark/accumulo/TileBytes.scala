package geotrellis.spark.accumulo

import geotrellis.raster._

object TileBytes {
  val tagRx = """\s*(\w+):(\w+):(\d+)x(\d+)""".r

  def tileTag(tile: Tile):String = {
    s"tile:${tile.cellType.name}:${tile.cols}x${tile.rows}"
  }

  def tileTag(id: String, tile:Tile): String = {
    f"${id}%8s:${tile.cellType.name}:${tile.cols}x${tile.rows}"
  }

  def fromBytes(tileTag: String, bytes: Array[Byte]): Tile = {
    val tagRx(id, cellType, cols, rows) = tileTag
    ArrayTile.fromBytes(bytes, CellType.fromString(cellType), cols.toInt, rows.toInt)
  }

  def fromBytesWithId(tileTag: String, bytes: Array[Byte]): (String, Tile) = {
    val tagRx(id, cellType, cols, rows) = tileTag
    id -> ArrayTile.fromBytes(bytes, CellType.fromString(cellType), cols.toInt, rows.toInt)
  }
}