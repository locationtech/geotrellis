package geotrellis.raster.io.geotiff.tags.codes

/**
  * The Orientations are named as such as the first position is where
  * the rows start and the second where the columns start.
  *
  * For example TopLeft means that the the 0th row is the top of the image
  * and the 0th column is the left of the image.
  */
object Orientations {

  val TopLeft = 1
  val TopRight = 2
  val BottomRight = 3
  val BottomLeft = 4
  val LeftTop = 5
  val RightTop = 6
  val RightBottom = 7
  val LeftBottom = 8

}
