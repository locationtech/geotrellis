package geotrellis.raster

package object stats {
  implicit class StatsMethodExtensions(val tile: Tile) extends StatsMethods { }
}
