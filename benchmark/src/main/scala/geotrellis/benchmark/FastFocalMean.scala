package geotrellis.benchmark

import geotrellis._
import geotrellis.raster.op.focal.Square
import geotrellis.raster.op._
import geotrellis.process._
import geotrellis.raster._

case class FastFocalMean(r:Op[Raster], n:Int) extends Op1(r)({
  r => new CalcFastFocalMean(r, n).calc
})

/**
 * This class encompasses an attempt to create a fast sequential focal mean
 * operation, using whatever tricks we can to eke out some speed.
 *
 * We require the raster to contain array data, which we will look up via apply 
 * instead of get. The kernel is a square, and the length of a side is twice
 * the radius plus one. Thus, radius=1 means a 3x3 square kernel (9 cells), and
 * radius=3 means a 7x7 square kernel (49 cells).
 */
final class CalcFastFocalMean(r:Raster, n:Int) {
  // get an array-like interface to the data
  final val data = r.toArrayRaster.data

  // raster data used to store the results
  final val out = data.alloc(r.cols, r.rows)

  // this is the length of a side of the square kernel
  final val radius = n
  final val diameter = 2 * radius + 1

  // we store one diameter's worth of row sums, and counts, for each cell.
  //
  // it's worth noting that we'll be overwriting old rows with new ones, and
  // will wrap around this data structure. to find the "right" set of sums for
  // a given row, use sumsByCol(row % diameter).
  //
  // note that we actually store things in (column, row) form, reversed from
  // the usual way. this is because our inner loops must be in terms of rows.
  final val sumsByCol = Array.ofDim[Int](r.cols, diameter)
  final val countsByCol = Array.ofDim[Int](r.cols, diameter)

  /**
   * This is the function that manages the whole calculation.
   */
  final def calc = {
    // dereference rows for faster access
    val rows = r.rows

    // at this point all we're doing is priming the row sums. until we reach
    // the radius row, we don't have enough data to call sumall() yet. "pre" is
    // the index for the "lower edge" of the calculation.
    var pre = 0
    while (pre < radius) {
      precalc(pre)
      pre += 1
    }

    // now we are starting to perform calculations as we continue to compute
    // more row sums. "row" is the row whose focal means are being computed.
    var row = 0
    while (row < radius) {
      precalc(pre)
      sumall(row)
      pre += 1
      row += 1
    }

    // this is the same case as above; the distinguishing thing is that
    // precalc() will be doing work to overwrite previous sums. we may want to
    // distinguish these two cases at some point.
    while (pre < rows) {
      precalc(pre)
      sumall(row)
      row += 1
      pre += 1
    }

    // we've hit the point where all row sums have been generated, and we
    // now need to start removing them using zero().
    while (row < rows) {
      zero(row - radius - 1)
      sumall(row)
      row += 1
    }

    // we have all our data, so let's return it!
    Result(Raster(out, r.rasterExtent))
  }

  /**
   * For a particular row, we want to create all the horizontal sums for each
   * cell in that row, and store those sums in the appropriate "line".
   *
   * At the start (where the kernel goes off the left side of the raster)
   * line(0) will be the sum of the first (radius + 1) cells, line(1) will be
   * the sum of the first (radius + 2) cells, and so on. By the middle of the
   * raster each sum will involve diameter cells, and then at the right we will
   * taper back to down (radius + 1) cells.
   */
  final def precalc(row:Int) {
    // dereference cols, rows and line for faster access
    val cols = r.cols
    val rows = r.rows
    val q = row % diameter

    // keep a running sum total, which we'll add and subtract to as appropriate
    var sum = 0
    var count = 0

    // initialize the sum for the first radius cells. we are not yet assigning
    // anything into line, this is just prep work. "i" is index of the leading
    // edge of our process, i.e. the right edge of the kernel.
    var i = row * cols 
    while (i < radius) {
      val z = data(i)
      if (z != NODATA) {
        sum += data(i)
        count += 1
      }
      i += 1
    }

    // now we start looping through the "left" edge of the row, that is, the
    // part where we don't have to start taking cells out of the sum (the space
    // between column 0 and column radius+1). "j" is the index of the cell
    // whose kernel we're processing, and "i" is still the leading edge.
    var j = 0
    while (j <= radius) {
      val z = data(i)
      if (z != NODATA) {
        sum += data(i)
        count += 1
      }
      sumsByCol(j)(q) = sum
      countsByCol(j)(q) = count
      i += 1
      j += 1
    }

    // now we are in the middle of the raster, where shifting the kernel means
    // adding a new cell on the right to the sum, and subtracting an old cell
    // on the left that's no longer part of the sum. "k" is the index of the
    // trailing edge of our process, i.e. the cell which we need to be removing
    // from the sum.
    var k = i - diameter
    val limit = cols - radius
    while (j < limit) {
      val z1 = data(i)
      if (z1 != NODATA) {
        sum += data(i)
        count += 1
      }
      val z2 = data(k)
      if (z2 != NODATA) {
        sum -= data(k)
        count -= 1
      }
      sumsByCol(j)(q) = sum
      countsByCol(j)(q) = count
      i += 1
      j += 1
      k += 1
    }

    // finally, we loop through the "right" edge of the raster. we no longer
    // have new cells to add, and instead we're just removing cells as the sum
    // slowly shrinks.
    while (j < cols) {
      val z = data(k)
      if (z != NODATA) {
        sum -= z
        count -= 1
      }
      sumsByCol(j)(q) = sum
      countsByCol(j)(q) = count
      j += 1
      k += 1
    }

    // at this point, each entry in line should be set to the sum of the
    // cells from this row participating in its kernel.
  }

  /**
   * For a particular row, sum all the available "row sums" and record the
   * results in the out. After we've called sumall() on all rows, the
   * calculation will be complete.
   */
  final def sumall(row:Int) {
    val cols = r.cols
    val rows = r.rows
    val span = row * cols

    // loop over every cell in this row. for each of them, sum the appropriate
    // row sums from all the rows in the kernel.
    //
    // note that we always loop over all the rows in sumsByCol/countsByCol.
    // this means that it's important that these structures start out zero, and
    // are zero'd appropriately as we move off the bottom raster edge.
    var x = 0
    while (x < cols) {
      var sum = 0
      var count = 0
      val sums = sumsByCol(x)
      val counts = countsByCol(x)
      var y = 0
      while (y < diameter) {
        sum += sums(y)
        count += counts(y)
        y += 1
      }
      if (count != 0) out(span + x) = sum / count
      x += 1
    }
  }

  /**
   * Initialize a particular line to zeros. We need to do this once we're on
   * the "right edge" of the raster and don't have new data to use.
   */
  final def zero(row:Int) {
    var x = 0
    val y = row % diameter
    val cols = r.cols
    while (x < cols) {
      sumsByCol(x)(y) = 0
      countsByCol(x)(y) = 0
      x += 1
    }
  }
}
