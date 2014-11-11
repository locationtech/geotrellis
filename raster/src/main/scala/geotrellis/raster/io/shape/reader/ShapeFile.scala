package geotrellis.raster.io.shape.reader

case class ShapeFile(records: Array[ShapeRecord], boundingBox: Array[Double])

case class ShapeIndexFile(offsets: Array[Int], sizes: Array[Int]) {

  val size = sizes.size

}

case class ShapeDatabaseFile(records: Array[String])
