package geotrellis.raster

package object statistics {
  implicit class StatsMethodExtensions(tile: Tile) extends StatsMethods { }
}
