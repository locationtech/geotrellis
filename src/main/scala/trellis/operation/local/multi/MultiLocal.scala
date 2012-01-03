package trellis.operation

import trellis.raster.IntRaster
import trellis.constant.NODATA
import trellis.process._


/**
 * MULTI OPERATIONS
 */
trait MultiLocal extends LocalOperation {
  val rs:Seq[IntRasterOperation]
  val identity:Int = 0

  def getRasters = { rs }

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
  var childStart:Long = 0

  def _run(server:Server)(implicit t:Timer) = {
    startTime = System.currentTimeMillis
    runAsync(rs.toList, server)
  }

  val nextSteps:Steps = {
    case rasters:List[_] => step2(rasters.asInstanceOf[List[IntRaster]])
  }
  def step2(rasters:List[IntRaster]) = {
    val datas   = rasters.map { raster => raster.data }

    //childTime = System.currentTimeMillis() - childStart

    val a = datas(0)
  
    val cols   = rasters(0).cols
    val rows   = rasters(0).rows
    val length = datas.length
    val id     = this.identity

    if (length == 1) {
    } else if(length == 2) {
      val b = datas(1)
      var i = 0
      val limit = rasters(0).length
      while (i < limit) {
        var z1 = a(i)
        var z2 = b(i)
        if (z1 != NODATA || z2 != NODATA) {
          if (z1 == NODATA) { z1 = id }
          else if (z2 == NODATA) { z2 = id }
          a(i) = handleCells2(z1, z2)
        }
        i += 1
      }

    } else if(length == 3) {
      val b = datas(1)
      val c = datas(2)
      var i = 0
      val limit = rasters(0).length
      while (i < limit) {
        var z1 = a(i)
        var z2 = b(i)
        var z3 = c(i)
        if (z1 != NODATA || z2 != NODATA || z3 != NODATA) {
          if (z1 == NODATA) { z1 = id }
          if (z2 == NODATA) { z2 = id }
          if (z3 == NODATA) { z3 = id }
          a(i) = handleCells3(z1, z2, z3)
        }
        i += 1
      }

    } else if (length == 4) {
      val b = datas(1)
      val c = datas(2)
      val d = datas(3)
      var i = 0
      val limit = rasters(0).length
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
          a(i) = handleCells4(z1, z2, z3, z4)
        }
        i += 1
      }

    } else if (length == 5) {
      val b = datas(1)
      val c = datas(2)
      val d = datas(3)
      val e = datas(4)
      var i = 0
      val limit = rasters(0).length
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
          a(i) = handleCells4(z1, z2, z3, z4)
        }
        a(i) = handleCells5(z1, z2, z3, z4, z5)
        i += 1
      }

    } else {
      val len = rasters.length
      var i = 0
      val limit = rasters(0).length
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
          a(i) = NODATA
        } else {
          a(i) = handleCells(zs)
        }

        i += 1
      }
    }
    endTime = System.currentTimeMillis
    StepResult(rasters(0))
  }
}
