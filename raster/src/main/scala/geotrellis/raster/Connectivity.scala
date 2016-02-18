package geotrellis.raster

abstract sealed trait Connectivity
case object FourNeighbors extends Connectivity
case object EightNeighbors extends Connectivity