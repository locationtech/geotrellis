package geotrellis.raster.op

import geotrellis._
import geotrellis.process._
import geotrellis.raster.op._
import geotrellis.testutil._
import geotrellis.raster.op.local.Add

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AverageLotsOfRastersTest extends FunSuite {
  val e = Extent(0.0, 0.0, 10.0, 10.0)
  val re = RasterExtent(e, 1.0, 1.0, 10, 10)

  val server = TestServer.server

  def r(n:Int) = Raster(Array.fill(100)(n), re)
  def r(n:Double) = Raster(Array.fill(100)(n), re)

  def addInts(ns:Int*) = Add(ns.map(n => Literal(r(n))))
  def addDoubles(ns:Double*) = Add(ns.map(n => r(n)))

  test("average rasters in sequential groups to limit memory usage") {
    val limit = 30
    val ops:Seq[Op[Raster]] = (1.0 until 200.0 by 1.0).map { i:Double => r(i) }
    val count = ops.length
 
    val dividedOps:Seq[Op[Raster]] = ops.map { rOp => local.Divide(rOp, count) }

    val firstRaster:Op[Raster] = dividedOps.head
    val groups = dividedOps.tail.grouped(limit).map { _.toArray }.toList

    val rOps:Op[Raster] = groups.foldLeft (firstRaster) ((oldResult:Op[Raster], newOps:Array[Op[Raster]]) => (logic.WithResult(oldResult)({ oldResult => local.Add( local.Add(newOps), oldResult) })) )
    val s = TestServer.server
    val output = s.run(rOps)
  }

  test("avg double rasters concurrently") {
    val limit = 30
    val ops:Seq[Op[Raster]] = (1.0 until 200.0 by 1.0).map { i:Double => Literal(r(i)) }
    val count = ops.length

    val dividedOps:Seq[Op[Raster]] = ops.map { rOp => local.Divide(rOp, count) }
    val firstRaster:Op[Raster] = dividedOps.head

    val groups = dividedOps.tail.grouped(limit).map(local.Add(_) ) 
    val ops2 = groups.toSeq.flaMapOps(seq => Add(seq))

    val s = TestServer.server
    val output = s.run(ops2)
  }
}
