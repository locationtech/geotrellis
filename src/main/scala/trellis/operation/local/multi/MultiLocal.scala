package trellis.operation

import trellis._
import trellis.process._

// TODO: this class needs love, badly.
// given that we're just using handleCells2 maybe we should remove all
// the other explicit junk? maybe not? either way it needs attention.

/**
 * MULTI OPERATIONS
 */
trait MultiLocal extends LocalOperation {
  val rs:Seq[IntRasterOperation]
  val identity:Int = 0

  def getRasters = rs

  def handleCells2(a:Int, b:Int): Int

  def handleCells3(a:Int, b:Int, c:Int) = {
    handleCells2(a, handleCells2(b, c))
  }
  def handleCells4(a:Int, b:Int, c:Int, d:Int) = {
    handleCells2(a, handleCells3(b, c, d))
  }
  def handleCells5(a:Int, b:Int, c:Int, d:Int, e:Int) = {
    handleCells2(a, handleCells4(b, c, d, e))
  }
  def handleCells(zs:Seq[Int]) = {
    zs.foldLeft(0)((z1, z2) => (handleCells2(z1, z2)))
  }

  def _run(context:Context) = runAsync(rs.toList)

  val nextSteps:Steps = {
    case rasters:List[_] => step2(rasters.asInstanceOf[List[IntRaster]])
  }

  def step2(rasters:List[IntRaster]) = {
    val datas   = rasters.map { raster => raster.data }

    val a = datas(0)
    val out = Array.ofDim[Int](a.length)
  
    val cols   = rasters(0).cols
    val rows   = rasters(0).rows
    val length = datas.length
    val id     = this.identity

    val limit = rasters(0).length

    if (length == 1) {
      var i = 0
      while (i < limit) {
        var z1 = a(i)
        if (z1 == NODATA) z1 = id
        out(i) = z1
        i += 1
      }

    } else if(length == 2) {
      val b = datas(1)
      var i = 0
      while (i < limit) {
        var z1 = a(i)
        var z2 = b(i)
        if (z1 != NODATA || z2 != NODATA) {
          if (z1 == NODATA) { z1 = id }
          else if (z2 == NODATA) { z2 = id }
          out(i) = handleCells2(z1, z2)
        }
        i += 1
      }

    } else if(length == 3) {
      val b = datas(1)
      val c = datas(2)
      var i = 0
      while (i < limit) {
        var z1 = a(i)
        var z2 = b(i)
        var z3 = c(i)
        if (z1 != NODATA || z2 != NODATA || z3 != NODATA) {
          if (z1 == NODATA) { z1 = id }
          if (z2 == NODATA) { z2 = id }
          if (z3 == NODATA) { z3 = id }
          out(i) = handleCells3(z1, z2, z3)
        }
        i += 1
      }

    } else if (length == 4) {
      val b = datas(1)
      val c = datas(2)
      val d = datas(3)
      var i = 0
      while (i < limit) {
        var z1 = a(i)
        var z2 = b(i)
        var z3 = c(i)
        var z4 = d(i)
        if (z1 != NODATA || z2 != NODATA || z3 != NODATA || z4 != NODATA) {
          if (z1 == NODATA) { z1 = id }
          if (z2 == NODATA) { z2 = id }
          if (z3 == NODATA) { z3 = id }
          if (z4 == NODATA) { z4 = id }
          out(i) = handleCells4(z1, z2, z3, z4)
        }
        i += 1
      }

    } else if (length == 5) {
      val b = datas(1)
      val c = datas(2)
      val d = datas(3)
      val e = datas(4)
      var i = 0
      while (i < limit) {
        var z1 = a(i)
        var z2 = b(i)
        var z3 = c(i)
        var z4 = d(i)
        var z5 = e(i)
        if (z1 != NODATA || z2 != NODATA || z3 != NODATA || z4 != NODATA || z5 != NODATA) {
          if (z1 == NODATA) { z1 = id }
          if (z2 == NODATA) { z2 = id }
          if (z3 == NODATA) { z3 = id }
          if (z4 == NODATA) { z4 = id }
          if (z5 == NODATA) { z5 = id }
          out(i) = handleCells4(z1, z2, z3, z4)
        }
        out(i) = handleCells5(z1, z2, z3, z4, z5)
        i += 1
      }

    } else {
      val len = rasters.length
      var i = 0
      while (i < limit) {
        var nodata = true
        val zs = datas.map {
          d => {
            val z = d(i)
            if (z == NODATA) {
              id
            } else {
              nodata = false
              z
            }
          }
        }

        if (nodata) {
          out(i) = NODATA
        } else {
          out(i) = handleCells(zs)
        }

        i += 1
      }
    }

    Result(IntRaster(out, rasters(0).cols, rasters(0).rows, rasters(0).rasterExtent))
  }
}
