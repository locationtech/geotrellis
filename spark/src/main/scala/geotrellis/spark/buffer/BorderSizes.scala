package geotrellis.spark.buffer

case class BorderSizes(left: Int, right: Int, bottom: Int, top: Int) {
  def totalWidth = left + right
  def totalHeight = top + bottom
}
