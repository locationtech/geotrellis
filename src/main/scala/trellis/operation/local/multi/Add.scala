package trellis.operation

import trellis.constant.NODATA
import trellis.raster.IntRaster
import trellis.process._

/**
  * Add the values of each cell in each raster.
  * Local operation.
  *
  * Add(r1, r2, r3)
  */
case class Add(rs:IntRasterOperation*) extends MultiLocal {
  override val identity = 0

  @inline
  def handleCells2(a:Int, b:Int) = a + b

  @inline
  override def handleCells3(a:Int, b:Int, c:Int) = a + b + c

  @inline
  override def handleCells4(a:Int, b:Int, c:Int, d:Int) = a + b + c + d

  @inline
  override def handleCells5(a:Int, b:Int, c:Int, d:Int, e:Int) = a + b + c + d + e
}

/**
  * Add the values of each cell in each raster.
  * Local operation.
  *
  * Add(Array(r1, r2, r3))
  */
case class AddArray(op:Operation[Array[IntRaster]]) extends Operation[IntRaster] {
  def childOperations = List(op)
  def _run(server:Server) = {
    startTime = System.currentTimeMillis
    runAsync(List(op), server)
  }
  val nextSteps:Steps = { case (arr:Array[IntRaster]) :: Nil => step2(arr) }

  @inline
  final def handle(a:Int, b:Int) = a + b

  def step2(rs:Array[IntRaster]) = {
    //println("AddArray: starting step2")

    val output = rs(0).copy
    val outdata = output.data
    var i = 1
    while (i < rs.length) {
      val indata = rs(i).data
      var j = 0
      while (j < indata.length) {
        val z = indata(j)

        if (z == NODATA) {
        } else if (outdata(j) == NODATA) {
          outdata(j) = indata(j)
        } else {
          outdata(j) = handle(outdata(j), indata(j))
        }

        j += 1
      }
      i += 1
    }
    //println("AddArray: returning data")
    endTime = System.currentTimeMillis
    Some(output)
  }
}
