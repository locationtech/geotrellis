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

  /**
    * Reads a shape file.
    *
    * Returns a class containing records of the shape file.
    */
  lazy val read: ShapeFile = {
    val p = if (path.endsWith(".shp")) path.substring(0, path.size - 4) else path

    val (spf, sdf) = {
      val shapePointFile =
        ShapePointFileReader(p + ShapePointFileReader.FileExtension).read
      val shapeDBaseFile =
        ShapeDBaseFileReader(p + ShapeDBaseFileReader.FileExtension).read

      val (s1, s2) = (shapePointFile.size, shapeDBaseFile.size)
      if (s1 != s2)
        throw new MalformedShapeFileException("Files has different number of elements.")

      (shapePointFile, shapeDBaseFile)
    }

    val res = Array.ofDim[ShapeRecord](spf.size)
    cfor(0)(_ < spf.size, _ + 1) { i =>
      res(i) = ShapeRecord(spf(i), sdf(i))
    }

    ShapeFile(res)
  }

}
