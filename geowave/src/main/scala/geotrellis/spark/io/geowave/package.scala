package geotrellis.spark.io

import geotrellis.raster._


package object geowave {

  /* http://stackoverflow.com/questions/3508077/how-to-define-type-disjunction-union-types */
  sealed class TileOrMultibandTile[T]
  object TileOrMultibandTile {
    implicit object TileWitness extends TileOrMultibandTile[Tile]
    implicit object MultibandTileWitness extends TileOrMultibandTile[MultibandTile]
  }
}
