package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

trait MultiBandTileBuilder {

  val array1 = Array(0, -1, 2, -3,
    4, -5, 6, -7,
    8, -9, 10, -11,
    12, -13, 14, -15)

  val array2 = Array(0, 4, -5, 6,
    -1, 2, -3, -7,
    12, -13, 14, -15,
    8, -9, 10, -11)

  val array3 = Array(0, 0, 2, -3,
    4, -5, 6, -7,
    8, 0, 10, 0,
    12, -13, 14, -15)

  val array4 = Array(10, -1, 2, -3,
    4, -5, 6, -7,
    8, -9, 10, -11,
    12, -13, 14, -15)

  val tile1 = IntArrayTile(array1, 4, 4)
  val tile2 = IntArrayTile(array2, 4, 4)
  val tile3 = IntArrayTile(array3, 4, 4)
  val tile4 = IntArrayTile(array4, 4, 4)

  val intMultiBand = MultiBandTile(Array(tile1, tile2, tile3, tile4))

  val arr1 = Array(NODATA, -1, 2, -3,
    4, -5, 6, NODATA,
    8, NODATA, 10, -11,
    NODATA, -13, 14, -15)

  val arr2 = Array(NODATA, 4, -5, 6,
    -1, NODATA, -3, -7,
    NODATA, -13, 14, -15,
    8, -9, 10, NODATA)

  val arr3 = Array(NODATA, NODATA, 2, -3,
    4, -5, 6, -7,
    8, NODATA, 10, NODATA,
    12, -13, NODATA, -15)

  val nTile1 = IntArrayTile(arr1, 4, 4)
  val nTile2 = IntArrayTile(arr2, 4, 4)
  val nTile3 = IntArrayTile(arr3, 4, 4)

  val nIntMultiBand = MultiBandTile(Array(nTile1, nTile2, nTile3))

  val n = Float.NaN
  val arr4 = Array(n, -1.0f, 2.0f, -3.0f,
    4.0f, -5.0f, 6.0f, n,
    8.0f, n, 10.0f, -11.0f,
    n, -13.0f, 14.0f, -15.0f)

  val arr5 = Array(n, 4.0f, -5.0f, 6.0f,
    -1.0f, n, -3.0f, -7.0f,
    n, -13.0f, 14.0f, -15.0f,
    8.0f, -9.0f, 10.0f, n)

  val arr6 = Array(n, n, 2.0f, -3.0f,
    4.0f, -5.0f, 6.0f, -7.0f,
    8.0f, n, 10.0f, n,
    12.0f, -13.0f, n, -15.0f)

  val dTile1 = FloatArrayTile(arr4, 4, 4)
  val dTile2 = FloatArrayTile(arr5, 4, 4)
  val dTile3 = FloatArrayTile(arr6, 4, 4)

  val nFloatMultiBand = MultiBandTile(Array(dTile1, dTile2, dTile3))

  val arr7 = Array(-1, -1, 1, 1,
    1, NODATA, -1, 1,
    1, -1, -1, 1,
    1, NODATA, NODATA, 1)

  val arr8 = Array(NODATA, -2, -2, -2,
    2, 2, NODATA, 2,
    2, -2, -2, -2,
    -2, NODATA, 2, 2)

  val arr9 = Array(NODATA, -3, NODATA, NODATA,
    3, NODATA, -3, -3,
    3, -3, -3, -3,
    NODATA, NODATA, NODATA, 3)

  val arr10 = Array(-4, NODATA, -4, 4,
    4, 4, NODATA, NODATA,
    NODATA, -4, -4, -4,
    -4, -4, -4, 4)

  val ati1 = IntArrayTile(arr7, 4, 4)
  val ati2 = IntArrayTile(arr8, 4, 4)
  val ati3 = IntArrayTile(arr9, 4, 4)
  val ati4 = IntArrayTile(arr10, 4, 4)

  val absIntmb = MultiBandTile(Array(ati1, ati2, ati3, ati4))

  val nd = Double.NaN

  val arr11 = Array(-1.0, -1.0, -1.0, -1.0,
    -1.0, -1.0, -1.0, -1.0,
    -1.0, -1.0, -1.0, -1.0,
    -1.0, 1.0, -1.0, 1.0)

  val arr12 = Array(2.0, 2.0, -2.0, nd,
    -2.0, -2.0, -2.0, 2.0,
    2.0, nd, nd, nd,
    nd, -2.0, nd, nd)

  val arr13 = Array(nd, nd, -3.0, -3.0,
    nd, nd, nd, -3.0,
    -3.0, -3.0, -3.0, nd,
    -3.0, 3.0, 3.0, -3.0)

  val arr14 = Array(4.0, nd, nd, nd,
    nd, nd, nd, -4.0,
    -4.0, nd, -4.0, -4.0,
    -4.0, -4.0, -4.0, -4.0)

  val atd1 = DoubleArrayTile(arr11, 4, 4)
  val atd2 = DoubleArrayTile(arr12, 4, 4)
  val atd3 = DoubleArrayTile(arr13, 4, 4)
  val atd4 = DoubleArrayTile(arr14, 4, 4)

  val absDubmb = MultiBandTile(Array(atd1, atd2, atd3, atd4))
  
  val arcArr0 = Array(0, 0, 0, 0, 0, 0, 0, 0, 0)
      val arcArr1 = Array(1, 1, 1, 1, 1, 1, 1, 1, 1)
      val arcArr2 = Array(-1, -1, -1, -1, -1, -1, -1, -1, -1)
      val arcArr3 = Array(2, 2, 2, 2, 2, 2, 2, 2, 2)
      val arcArr4 = Array(-2, -2, -2, -2, -2, -2, -2, -2, -2)
      val arcArr5 = Array(NODATA, NODATA, NODATA, NODATA, NODATA, NODATA, NODATA, NODATA, NODATA)

      val t0 = IntArrayTile(arcArr0, 3, 3)
      val t1 = IntArrayTile(arcArr1, 3, 3)
      val t2 = IntArrayTile(arcArr2, 3, 3)
      val t3 = IntArrayTile(arcArr3, 3, 3)
      val t4 = IntArrayTile(arcArr4, 3, 3)
      val t5 = IntArrayTile(arcArr5, 3, 3)

      val arcMB = MultiBandTile(Array(t0, t1, t2, t3, t4, t5))
}
