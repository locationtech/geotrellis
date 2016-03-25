package geotrellis.raster


/**
  * An encoding of the connectivity of vector tiles.
  */
abstract sealed trait Connectivity

/**
  * Four-way connectivity.
  */
case object FourNeighbors extends Connectivity

/**
  * Eight-way connectivity.
  */
case object EightNeighbors extends Connectivity
