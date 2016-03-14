package geotrellis.raster

trait SimpleMultibandTile {
  def bands(bandSequence: Seq[Int]): MultibandTile

  def bands(bandSequence: Int*)(implicit d: DummyImplicit): MultibandTile
}
