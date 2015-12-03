package geotrellis.spark.io

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.spark._

package object slippy {
  implicit object TileTileByteReader extends TileByteReader[Tile] {
    def read(zoom: Int, key: SpatialKey, bytes: Array[Byte]): Tile =
      SingleBandGeoTiff(bytes).tile
  }

  implicit object MultiBandTileTileByteReader extends TileByteReader[MultiBandTile] {
    def read(zoom: Int, key: SpatialKey, bytes: Array[Byte]): MultiBandTile =
      MultiBandGeoTiff(bytes).tile
  }

  implicit def tileTileByteWriter: TileByteWriter[Tile] = 
    new TileByteWriter[Tile] {
      def extension = "tif"
      def write(zoom: Int, key: SpatialKey, value: Tile): Array[Byte] = ???
    }

  implicit def multiBandTileTileByteWriter: TileByteWriter[MultiBandTile] = 
    new TileByteWriter[MultiBandTile] {
      def extension = "tif"
      def write(zoom: Int, key: SpatialKey, value: MultiBandTile): Array[Byte] = ???
    }
}
