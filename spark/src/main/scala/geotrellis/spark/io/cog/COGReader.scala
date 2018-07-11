package geotrellis.spark.io.cog


import geotrellis.raster.Tile
import geotrellis.spark.io.Reader


trait COGReader[K, V] extends Reader[K, V] {
  def readSubsetBands(key: K, bands: Int*)(implicit d: DummyImplicit): Array[Option[Tile]] =
    readSubsetBands(key, bands.toSeq)

  def readSubsetBands(key: K, bands: Seq[Int]): Array[Option[Tile]]
}
