package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.process._
import geotrellis.statistics._

case class MoransI(r:Op[Raster]) extends Op1(r)({
  r => Result(MoransIndex.RooksAlgorithmScalar.run(r))
})

case class LocalMoransI(r:Op[Raster]) extends Op1(r)({
  r => Result(MoransIndex.RooksAlgorithmRaster.run(r))
})

case class QueensMoransI(r:Op[Raster]) extends Op1(r)({
  r => Result(MoransIndex.QueensAlgorithmScalar.run(r))
})

case class LocalQueensMoransI(r:Op[Raster]) extends Op1(r)({
  r => Result(MoransIndex.QueensAlgorithmRaster.run(r))
})

object MoransIndex {
  
  sealed trait MoranContext
  case class CountContext(var count:Double, var ws:Int) extends MoranContext
  case class RasterContext(data:MutableRasterData) extends MoranContext
  
  trait MoransAlgorithm[A] {
    type Context <: MoranContext
  
    def run(raster:Raster):A
    def createContext(r:Raster): Context
    def store(c:Context, w:Int, col:Int, row:Int, z:Double):Unit
  
    // interior
    def handleInterior(context:Context, x:Int, y:Int, r:Raster, v:Double): Unit
  
    // edges
    def handleNorthEdge(context:Context, x:Int, y:Int, r:Raster, v:Double): Unit
    def handleEastEdge(context:Context, x:Int, y:Int, r:Raster, v:Double): Unit
    def handleSouthEdge(context:Context, x:Int, y:Int, r:Raster, v:Double): Unit
    def handleWestEdge(context:Context, x:Int, y:Int, r:Raster, v:Double): Unit
  
    // corners
    def handleNwCorner(context:Context, x:Int, y:Int, r:Raster, v:Double): Unit
    def handleNeCorner(context:Context, x:Int, y:Int, r:Raster, v:Double): Unit
    def handleSeCorner(context:Context, x:Int, y:Int, r:Raster, v:Double): Unit
    def handleSwCorner(context:Context, x:Int, y:Int, r:Raster, v:Double): Unit
  
    def calculate(raster:Raster): Context = {
  
      // create the statistics that we need
      val h = FastMapHistogram.fromRaster(raster)
      val Statistics(mean, _, _, stddev, _, _) = h.generateStatistics
      val v:Double = stddev * stddev
  
      // force our raster, since we'll want fast access to individual cells
      val r1 = raster.convert(TypeDouble).force.mapDouble(_ - mean)
  
      val cols = r1.cols
      val rows = r1.rows
      val context = createContext(r1)
  
      val lastcol = cols - 1
      val lastrow = rows - 1
  
      var x = 1
      var y = 1
      while (y < lastrow) {
        x = 1
        while (x < lastcol) {
          handleInterior(context, x, y, r1, v)
          x += 1
        }
        y += 1
      }
  
      x = 1
      while (x < lastcol) {
        handleEastEdge(context, x, 0, r1, v)
        handleWestEdge(context, x, lastrow, r1, v)
        x += 1
      }
  
      y = 1
      while (y < lastrow) {
        handleNorthEdge(context, 0, y, r1, v)
        handleSouthEdge(context, lastcol, y, r1, v)
        y += 1
      }
  
      handleNwCorner(context, 0, 0, r1, v)
      handleNeCorner(context, lastcol, 0, r1, v)
      handleSwCorner(context, 0, lastrow, r1, v)
      handleSeCorner(context, lastcol, lastrow, r1, v)
  
      context
    }
  }
  
  trait QueensAlgorithm[A] extends MoransAlgorithm[A] {
    def handleInterior(context:Context, x:Int, y:Int, r:Raster, v:Double) {
      store(context, 8, x, y, r.getDouble(x, y) / v * (r.getDouble(x - 1, y - 1) +
                                                       r.getDouble(x - 1, y    ) +
                                                       r.getDouble(x - 1, y + 1) +
                                                       r.getDouble(x,     y - 1) +
                                                       r.getDouble(x,     y + 1) +
                                                       r.getDouble(x + 1, y - 1) +
                                                       r.getDouble(x + 1, y    ) +
                                                       r.getDouble(x + 1, y + 1)))
    }
  
    def handleNorthEdge(context:Context, x:Int, y:Int, r:Raster, v:Double) {
      store(context, 5, 0, y, r.getDouble(0, y) / v * (r.getDouble(0, y - 1) +
                                                       r.getDouble(1, y - 1) +
                                                       r.getDouble(1, y    ) +
                                                       r.getDouble(0, y + 1) +
                                                       r.getDouble(1, y + 1)))
    }
  
    def handleEastEdge(context:Context, x:Int, y:Int, r:Raster, v:Double) {
      store(context, 5, x, 0, r.getDouble(x, 0) / v * (r.getDouble(x - 1, 0) +
                                                       r.getDouble(x - 1, 1) +
                                                       r.getDouble(x,     1) +
                                                       r.getDouble(x + 1, 0) +
                                                       r.getDouble(x + 1, 1)))
    }
  
    def handleSouthEdge(context:Context, x:Int, y:Int, r:Raster, v:Double) {
      store(context, 5, x, y, r.getDouble(x, y) / v * (r.getDouble(x,     y - 1) +
                                                       r.getDouble(x - 1, y - 1) +
                                                       r.getDouble(x - 1, y    ) +
                                                       r.getDouble(x,     y + 1) +
                                                       r.getDouble(x - 1, y + 1)))
    }
  
    def handleWestEdge(context:Context, x:Int, y:Int, r:Raster, v:Double) {
      store(context, 5, x, y, r.getDouble(x, y) / v * (r.getDouble(x - 1, y    ) +
                                                       r.getDouble(x - 1, y - 1) +
                                                       r.getDouble(x,     y - 1) +
                                                       r.getDouble(x + 1, y    ) +
                                                       r.getDouble(x + 1, y - 1)))
    }
  
    def handleNwCorner(context:Context, x:Int, y:Int, r:Raster, v:Double) {
      store(context, 3, 0, 0, r.getDouble(0, 0) / v * (r.getDouble(0, 1) +
                                                       r.getDouble(1, 0) +
                                                       r.getDouble(1, 1)))
    }
  
    def handleNeCorner(context:Context, x:Int, y:Int, r:Raster, v:Double) {
      store(context, 3, x, 0, r.getDouble(x, 0) / v * (r.getDouble(x, 1) +
                                                       r.getDouble(x - 1, 0) +
                                                       r.getDouble(x - 1, 1)))
    }
  
    def handleSeCorner(context:Context, x:Int, y:Int, r:Raster, v:Double) {
      store(context, 3, x, y, r.getDouble(x, y) / v * (r.getDouble(x - 1, y) +
                                                       r.getDouble(x,     y - 1) +
                                                       r.getDouble(x - 1, y - 1)))
    }
  
    def handleSwCorner(context:Context, x:Int, y:Int, r:Raster, v:Double) {
      store(context, 3, 0, y, r.getDouble(0, y) / v * (r.getDouble(1, y) +
                                                       r.getDouble(0, y - 1) +
                                                       r.getDouble(1, y - 1)))
    }
  }
  
  trait RooksAlgorithm[A] extends MoransAlgorithm[A] {
    def handleInterior(context:Context, x:Int, y:Int, r:Raster, v:Double) {
      store(context, 4, x, y, r.getDouble(x, y) / v * (r.getDouble(x - 1, y    ) +
                                                       r.getDouble(x,     y - 1) +
                                                       r.getDouble(x,     y + 1) +
                                                       r.getDouble(x + 1, y    )))
    }
  
    def handleNorthEdge(context:Context, x:Int, y:Int, r:Raster, v:Double) {
      store(context, 3, 0, y, r.getDouble(0, y) / v * (r.getDouble(0, y - 1) +
                                                       r.getDouble(1, y    ) +
                                                       r.getDouble(0, y + 1)))
    }
  
    def handleEastEdge(context:Context, x:Int, y:Int, r:Raster, v:Double) {
      store(context, 3, x, 0, r.getDouble(x, 0) / v * (r.getDouble(x - 1, 0) +
                                                       r.getDouble(x,     1) +
                                                       r.getDouble(x + 1, 0)))
    }
  
    def handleSouthEdge(context:Context, x:Int, y:Int, r:Raster, v:Double) {
      store(context, 3, x, y, r.getDouble(x, y) / v * (r.getDouble(x,     y - 1) +
                                                       r.getDouble(x - 1, y    ) +
                                                       r.getDouble(x,     y + 1)))
    }
  
    def handleWestEdge(context:Context, x:Int, y:Int, r:Raster, v:Double) {
      store(context, 3, x, y, r.getDouble(x, y) / v * (r.getDouble(x - 1, y    ) +
                                                       r.getDouble(x,     y - 1) +
                                                       r.getDouble(x + 1, y    )))
    }
  
    def handleNwCorner(context:Context, x:Int, y:Int, r:Raster, v:Double) {
      store(context, 2, 0, 0, r.getDouble(0, 0) / v * (r.getDouble(0, 1) +
                                                       r.getDouble(1, 0)))
    }
  
    def handleNeCorner(context:Context, x:Int, y:Int, r:Raster, v:Double) {
      store(context, 2, x, 0, r.getDouble(x, 0) / v * (r.getDouble(x, 1) +
                                                       r.getDouble(x - 1, 0)))
    }
  
    def handleSeCorner(context:Context, x:Int, y:Int, r:Raster, v:Double) {
      store(context, 2, x, y, r.getDouble(x, y) / v * (r.getDouble(x - 1, y) +
                                                       r.getDouble(x,     y - 1)))
    }
  
    def handleSwCorner(context:Context, x:Int, y:Int, r:Raster, v:Double) {
      store(context, 2, 0, y, r.getDouble(0, y) / v * (r.getDouble(1, y) +
                                                       r.getDouble(0, y - 1)))
    }
  }
  
  object QueensAlgorithmScalar extends QueensAlgorithm[Double] {
    type Context = CountContext
    def createContext(r:Raster) = CountContext(0.0, 0)
    def store(c:Context, w:Int, col:Int, row:Int, z:Double) {
      c.count += z
      c.ws += w
    }
    def run(r:Raster) = {
      val c = calculate(r)
      c.count / c.ws
    }
  }
  
  object QueensAlgorithmRaster extends QueensAlgorithm[Raster] {
    type Context = RasterContext
    def createContext(r:Raster) = RasterContext(DoubleArrayRasterData.ofDim(r.cols, r.rows))
    def store(c:Context, w:Int, col:Int, row:Int, z:Double) {
      c.data.setDouble(col, row, z / w)
    }
    def run(r:Raster) = Raster(calculate(r).data, r.rasterExtent)
  }
  
  object RooksAlgorithmScalar extends RooksAlgorithm[Double] {
    type Context = CountContext
    def createContext(r:Raster) = CountContext(0.0, 0)
    def store(c:Context, w:Int, col:Int, row:Int, z:Double) {
      c.count += z
      c.ws += w
    }
    def run(r:Raster) = {
      val c = calculate(r)
      c.count / c.ws
    }
  }
  
  object RooksAlgorithmRaster extends RooksAlgorithm[Raster] {
    type Context = RasterContext
    def createContext(r:Raster) = {
      RasterContext(DoubleArrayRasterData.ofDim(r.cols, r.rows))
    }
    def store(c:Context, w:Int, col:Int, row:Int, z:Double) {
      c.data.setDouble(col, row, z / w)
    }
    def run(r:Raster) = Raster(calculate(r).data, r.rasterExtent)
  }
}
