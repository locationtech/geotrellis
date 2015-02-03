package geotrellis.vector.io.shape.reader

case class ShapeFile(records: Array[ShapeRecord]) {
  val size = records.size

  def apply(i: Int): ShapeRecord = records(i)
}

case class ShapePointFile(records: Array[ShapePointRecord], boundingBox: Array[Double]) {
  val size = records.size

  def apply(i: Int): ShapePointRecord = records(i)
}

case class ShapeIndexFile(offsets: Array[Int], sizes: Array[Int]) {
  val size = sizes.size
}

case class ShapeDBaseFile(records: Array[Map[String, ShapeDBaseRecord]]) {
  val size = records.size

  def apply(i: Int): Map[String, ShapeDBaseRecord] = records(i)
}
