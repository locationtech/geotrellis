package geotrellis.spark

import geotrellis.Raster
import geotrellis.RasterExtent

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.formats._
import geotrellis.spark.cmd.NoDataHandler
import geotrellis.spark.metadata.PyramidMetadata

case class Tile(id: Long, raster: Raster) {
  def tileXY(zoom: Int) =  TmsTiling.tileXY(id, zoom)

  def toWritable() = 
    (TileIdWritable(id), ArgWritable.fromRaster(raster))
}

object Tile {
  implicit def tupleToTile(t:(Long,Raster)): Tile =
    Tile(t._1, t._2)

  def toWritable(tr: Tile): WritableTile =
    (TileIdWritable(tr.id), ArgWritable.fromRasterData(tr.raster.data))
}
