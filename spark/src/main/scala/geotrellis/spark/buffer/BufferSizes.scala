package geotrellis.spark.buffer

case class BufferSizes(left: Int, right: Int, bottom: Int, top: Int) {
  def totalWidth = left + right
  def totalHeight = top + bottom
}
