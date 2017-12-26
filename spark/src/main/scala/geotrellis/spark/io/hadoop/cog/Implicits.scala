package geotrellis.spark.io.hadoop.cog

import geotrellis.raster._
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.spark.io.cog.TiffMethods

trait Implicits extends HadoopTiffMethods with Serializable {
  implicit val hadoopSinglebandCOGRDDReader: HadoopCOGRDDReader[Tile] =
    new HadoopCOGRDDReader[Tile] {
      implicit val tileMergeMethods: Tile => TileMergeMethods[Tile] =
        tile => withTileMethods(tile)

      implicit val tilePrototypeMethods: Tile => TilePrototypeMethods[Tile] =
        tile => withTileMethods(tile)

      val tiffMethods: TiffMethods[Tile] with HadoopCOGBackend = hadoopTiffMethods
    }

  implicit val hadoopMultibandCOGRDDReader: HadoopCOGRDDReader[MultibandTile] =
    new HadoopCOGRDDReader[MultibandTile] {
      implicit val tileMergeMethods: MultibandTile => TileMergeMethods[MultibandTile] =
        implicitly[MultibandTile => TileMergeMethods[MultibandTile]]

      implicit val tilePrototypeMethods: MultibandTile => TilePrototypeMethods[MultibandTile] =
        implicitly[MultibandTile => TilePrototypeMethods[MultibandTile]]

      val tiffMethods: TiffMethods[MultibandTile] with HadoopCOGBackend = hadoopMultibandTiffMethods
    }
}
