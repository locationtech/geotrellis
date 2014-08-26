package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

trait MultiBandTileBuilder {

  val intMultiBand = {
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
    MultiBandTile(Array(tile1, tile2, tile3, tile4))
  }

  val doubleMultiBand = {
    val array5 = Array(0.0, -1.0, 2.5, -3.2,
      4.8, -5.67, 6.90, -7.4,
      8.7, -9.0, 10.3, -11.6,
      12.4, -13.9, 14.5, -15.1)

    val array6 = Array(144.4343, 332.55555, 56.0, -5.0,
      6.7, 14.5, -5.6, -6.7,
      -24.0, 3.0, 4.0, 6.6,
      -4.0, -5.2, -7.6, -8.0)

    val array7 = Array(0.0, 4.0, -5.6, 6.4,
      -1.5, 2.7, -3.8, -7.8,
      12.6, -13.3, 14.5, -15.7,
      8.6, -9.0, 10.5, -11.9)

    val array8 = Array(10.553553, -1.04554, 2.76443, 14.5,
      4.953, -55634.06, 6.645, -75547.2,
      82.3, -944.5445, 10534543.6, -11.9,
      1245.56, -13.9076, 14.5, -15.787887)

    val tile5 = DoubleArrayTile(array5, 4, 4)
    val tile6 = DoubleArrayTile(array6, 4, 4)
    val tile7 = DoubleArrayTile(array7, 4, 4)
    val tile8 = DoubleArrayTile(array8, 4, 4)
    MultiBandTile(Array(tile5, tile6, tile7, tile8))
  }

  def nIntMultiBand = {
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
    MultiBandTile(Array(nTile1, nTile2, nTile3))
  }

  def nFloatMultiBand = {
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
    MultiBandTile(Array(dTile1, dTile2, dTile3))
  }

  def absIntmb = {
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

    MultiBandTile(Array(ati1, ati2, ati3, ati4))
  }

  def absDubmb = {
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

    MultiBandTile(Array(atd1, atd2, atd3, atd4))
  }

  def arcIntMB = {
    val arcArr0 = Array.fill(9)(0)
    val arcArr1 = Array.fill(9)(1)
    val arcArr2 = Array.fill(9)(-1)
    val arcArr3 = Array.fill(9)(2)
    val arcArr4 = Array.fill(9)(-2)
    val arcArr5 = Array.fill(9)(NODATA)

    val t0 = IntArrayTile(arcArr0, 3, 3)
    val t1 = IntArrayTile(arcArr1, 3, 3)
    val t2 = IntArrayTile(arcArr2, 3, 3)
    val t3 = IntArrayTile(arcArr3, 3, 3)
    val t4 = IntArrayTile(arcArr4, 3, 3)
    val t5 = IntArrayTile(arcArr5, 3, 3)

    MultiBandTile(Array(t0, t1, t2, t3, t4, t5))
  }

  def arcDoubleMB = {
    val ar0 = Array.fill(9)(0.0)
    val ar1 = Array.fill(9)(1.0)
    val ar2 = Array.fill(9)(-1.0)
    val ar3 = Array.fill(9)(2.0)
    val ar4 = Array.fill(9)(-2.0)
    val ar5 = Array.fill(9)(Double.NaN)

    val dt0 = DoubleArrayTile(ar0, 3, 3)
    val dt1 = DoubleArrayTile(ar1, 3, 3)
    val dt2 = DoubleArrayTile(ar2, 3, 3)
    val dt3 = DoubleArrayTile(ar3, 3, 3)
    val dt4 = DoubleArrayTile(ar4, 3, 3)
    val dt5 = DoubleArrayTile(ar5, 3, 3)

    MultiBandTile(Array(dt0, dt1, dt2, dt3, dt4, dt5))
  }

  private val ar6 = Array.fill(9)(1)
  private val ar7 = Array.fill(9)(1)
  private val ar8 = Array.fill(9)(1)
  private val ar9 = Array.fill(9)(1)
  private val ar10 = Array.fill(9)(1)
  private val ar11 = Array.fill(9)(1)

  private val it0 = IntArrayTile(ar6, 3, 3)
  private val it1 = IntArrayTile(ar7, 3, 3)
  private val it2 = IntArrayTile(ar8, 3, 3)
  private val it3 = IntArrayTile(ar9, 3, 3)
  private val it4 = IntArrayTile(ar10, 3, 3)
  private val it5 = IntArrayTile(ar11, 3, 3)

  val intConstMB = MultiBandTile(Array(it0, it1, it2, it3, it4, it5))

  def doubleConstMB = {
    val ar12 = Array.fill(9)(1.0)
    val ar13 = Array.fill(9)(1.0)
    val ar14 = Array.fill(9)(1.0)
    val ar15 = Array.fill(9)(1.0)
    val ar16 = Array.fill(9)(1.0)
    val ar17 = Array.fill(9)(1.0)

    val dt6 = DoubleArrayTile(ar12, 3, 3)
    val dt7 = DoubleArrayTile(ar13, 3, 3)
    val dt8 = DoubleArrayTile(ar14, 3, 3)
    val dt9 = DoubleArrayTile(ar15, 3, 3)
    val dt10 = DoubleArrayTile(ar16, 3, 3)
    val dt11 = DoubleArrayTile(ar17, 3, 3)

    MultiBandTile(Array(dt6, dt7, dt8, dt9, dt10, dt11))
  }

  val intCeilMB = {
    val carr = it0
    carr.set(0, 0, NODATA)
    carr.set(0, 1, NODATA)
    carr.set(2, 2, NODATA)
    MultiBandTile(Array(carr, it1, it2, it3, it4, it5))
  }

  def doubleCeilMB = {
    val carr0 = Array.fill(9)(3.4)
    val carr1 = Array.fill(9)(3.4)
    val carr2 = Array.fill(9)(3.4)
    val carr3 = Array.fill(9)(3.4)

    val ctile0 = DoubleArrayTile(carr0, 3, 3)
    val ctile1 = DoubleArrayTile(carr1, 3, 3)
    val ctile2 = DoubleArrayTile(carr2, 3, 3)
    val ctile3 = DoubleArrayTile(carr3, 3, 3)

    ctile0.setDouble(0, 0, Double.NaN)
    ctile1.setDouble(2, 2, Double.NaN)
    ctile2.setDouble(1, 1, Double.NaN)

    MultiBandTile(Array(ctile0, ctile1, ctile2, ctile3))
  }

  /**
   * 9x10 raster of 90 numbers between 1 - 100 in random order,
   * with NoData values in every even column.
   */

  def positiveIntNoDataMB = {
    val ND = NODATA
    val pa0 = Array(54, ND, 44, ND, 21, ND, 13, ND, 41)
    val pa1 = Array(66, ND, 63, ND, 28, ND, 45, ND, 46)
    val pa2 = Array(38, ND, 74, ND, 4, ND, 64, ND, 32)
    val pa3 = Array(81, ND, 80, ND, 7, ND, 37, ND, 3)
    val pa4 = Array(42, ND, 40, ND, 73, ND, 68, ND, 91)
    val pa5 = Array(98, ND, 79, ND, 8, ND, 96, ND, 85)
    val pa6 = Array(76, ND, 90, ND, 83, ND, 19, ND, 22)
    val pa7 = Array(33, ND, 29, ND, 39, ND, 49, ND, 25)
    val pa8 = Array(20, ND, 65, ND, 61, ND, 87, ND, 52)
    val pa9 = Array(11, ND, 30, ND, 27, ND, 97, ND, 14)

    val pt0 = IntArrayTile(pa0, 3, 3)
    val pt1 = IntArrayTile(pa1, 3, 3)
    val pt2 = IntArrayTile(pa2, 3, 3)
    val pt3 = IntArrayTile(pa3, 3, 3)
    val pt4 = IntArrayTile(pa4, 3, 3)
    val pt5 = IntArrayTile(pa5, 3, 3)
    val pt6 = IntArrayTile(pa6, 3, 3)
    val pt7 = IntArrayTile(pa7, 3, 3)
    val pt8 = IntArrayTile(pa8, 3, 3)
    val pt9 = IntArrayTile(pa9, 3, 3)

    MultiBandTile(Array(pt0, pt1, pt2, pt3, pt4, pt5, pt6, pt7, pt8, pt9))
  }

  /**
   * 9x10 TypeDouble raster with values between 0 and 1, exclusive,
   * with Double.NaN values in every even column.
   */

  def positiveDoubleNoDataMB = {
    val n = Double.NaN

    val pa0 = Array(0.69, n, 0.72, n, 0.64, n, 0.32, n, 0.04)
    val pa1 = Array(0.65, n, 0.26, n, 0.34, n, 0.05, n, 0.91)
    val pa2 = Array(0.52, n, 0.58, n, 0.11, n, 0.30, n, 0.90)
    val pa3 = Array(0.59, n, 0.60, n, 0.70, n, 0.86, n, 0.84)
    val pa4 = Array(0.61, n, 0.94, n, 0.14, n, 0.99, n, 0.73)
    val pa5 = Array(0.85, n, 0.31, n, 0.47, n, 0.97, n, 0.25)
    val pa6 = Array(0.08, n, 0.96, n, 0.40, n, 0.20, n, 0.13)
    val pa7 = Array(0.09, n, 0.02, n, 0.54, n, 0.62, n, 0.53)
    val pa8 = Array(0.98, n, 0.93, n, 0.42, n, 0.55, n, 0.01)
    val pa9 = Array(0.74, n, 0.75, n, 0.33, n, 0.79, n, 0.78)

    val pt0 = DoubleArrayTile(pa0, 3, 3)
    val pt1 = DoubleArrayTile(pa1, 3, 3)
    val pt2 = DoubleArrayTile(pa2, 3, 3)
    val pt3 = DoubleArrayTile(pa3, 3, 3)
    val pt4 = DoubleArrayTile(pa4, 3, 3)
    val pt5 = DoubleArrayTile(pa5, 3, 3)
    val pt6 = DoubleArrayTile(pa6, 3, 3)
    val pt7 = DoubleArrayTile(pa7, 3, 3)
    val pt8 = DoubleArrayTile(pa8, 3, 3)
    val pt9 = DoubleArrayTile(pa9, 3, 3)

    MultiBandTile(Array(pt0, pt1, pt2, pt3, pt4, pt5, pt6, pt7, pt8, pt9))
  }
}
