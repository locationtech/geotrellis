package geotrellis.raster.io.geotiff.tags

object Pixel3D {

  def fromArray(v: Array[Double]): Pixel3D =
    if (v.size == 3) Pixel3D(v(0), v(1), v(2))
    else throw new IllegalArgumentException(
      "3D pixel needs vector with size 3 (x, y ,z)"
    )

}

case class Pixel3D(x: Double, y: Double, z: Double)
