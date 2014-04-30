package geotrellis.proj4

import org.osgeo.proj4j._

object Transform {
  def transform(x: Double, y: Double, srcCrs: String, destCrs: String): (Double, Double) = {
    val f = new CRSFactory
    val src = f.createFromName(srcCrs)
    val dest = f.createFromName(destCrs)
    val tf = new CoordinateTransformFactory
    val t = tf.createTransform(src, dest)
    val srcP = new ProjCoordinate(x, y)
    val destP = new ProjCoordinate
    t.transform(srcP, destP)
    (destP.x, destP.y)
  }
}
