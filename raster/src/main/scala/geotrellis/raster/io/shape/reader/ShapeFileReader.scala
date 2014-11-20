package geotrellis.raster.io.shape.reader

import spire.syntax.cfor._

object ShapeFileReader {

  /**
    * Takes a path to a file, but without any extensions. An example would be:
    *
    * "/Users/iamthewalrus/Documents/my-shape-file"
    *
    * There should then be three files:
    * <ul>
    * <li>"my-shape-file.shp"</li>
    * <li>"my-shape-file.shx"</li>
    * <li>"my-shape-file.dbf"</li>
    * </ul>
    *
    * The DBase file (.dbf) should be of DBase version III.
    */
  def apply(path: String): ShapeFileReader = new ShapeFileReader(path)

}

case class MalformedShapeFileException(msg: String) extends RuntimeException(msg)

class ShapeFileReader(path: String) {

  private val ShapePointFileExtension = ".shp"

  private val ShapeIndexFileExtension = ".shx"

  private val CodePageFileExtension = ".cpg"

  private val ShapeDBaseFileExtension = ".dbf"

  /**
    * Reads a shape file.
    *
    * Returns a class containing records of the shape file.
    */
  lazy val read: ShapeFile = {
    val (spf, sif, sdf) = {
      val shapePointFile =
        ShapePointFileReader(path + ShapePointFileExtension).read
      val shapeIndexFile =
        ShapeIndexFileReader(path + ShapeIndexFileExtension).read
      val charset =
        CodePageFileReader(path + CodePageFileExtension).read
      val shapeDBaseFile =
        ShapeDBaseFileReader(path + ShapeDBaseFileExtension, charset).read

      val (s1, s2, s3) = (shapePointFile.size, shapeIndexFile.size, shapeDBaseFile.size)
      if (s1 != s2 || s1 != s3 || s2 != s3)
        throw new MalformedShapeFileException("Files has different number of elements.")

      (shapePointFile, shapeIndexFile, shapeDBaseFile)
    }

    val res = Array.ofDim[ShapeRecord](spf.size)
    cfor(0)(_ < spf.size, _ + 1) { i =>
      res(i) = ShapeRecord(spf(i), sdf(i))
    }

    ShapeFile(res)
  }

}
