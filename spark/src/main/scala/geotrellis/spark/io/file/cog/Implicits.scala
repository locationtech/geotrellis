package geotrellis.spark.io.file.cog

import geotrellis.raster._
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.spark.io.cog.TiffMethods

trait Implicits extends FileTiffMethods with Serializable {
  implicit val fileSinglebandCOGRDDReader: FileCOGRDDReader[Tile] =
    new FileCOGRDDReader[Tile] {
      implicit val tileMergeMethods: Tile => TileMergeMethods[Tile] =
        tile => withTileMethods(tile)

      implicit val tilePrototypeMethods: Tile => TilePrototypeMethods[Tile] =
        tile => withTileMethods(tile)

      val tiffMethods: TiffMethods[Tile] with FileCOGBackend = fileTiffMethods
    }

  implicit val fileMultibandCOGRDDReader: FileCOGRDDReader[MultibandTile] =
    new FileCOGRDDReader[MultibandTile] {
      implicit val tileMergeMethods: MultibandTile => TileMergeMethods[MultibandTile] =
        tile => withMultibandTileMethods(tile)

      implicit val tilePrototypeMethods: MultibandTile => TilePrototypeMethods[MultibandTile] =
        tile => withMultibandTileMethods(tile)

      val tiffMethods: TiffMethods[MultibandTile] with FileCOGBackend = fileMultibandTiffMethods
    }
}
