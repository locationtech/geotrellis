package geotrellis.raster.op

import geotrellis._
import geotrellis.process._
import geotrellis.raster.op._

import geotrellis.raster.op.local.AddArray
import geotrellis.{op => liftOp}

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AverageLotsOfRastersTest extends FunSuite {
  val e = Extent(0.0, 0.0, 10.0, 10.0)
  val re = RasterExtent(e, 1.0, 1.0, 10, 10)

  val server = TestServer()

  def r(n:Int) = Raster(Array.fill(100)(n), re)
  def r(n:Double) = Raster(Array.fill(100)(n), re)

  def addInts(ns:Int*) = AddArray(ns.map(n => r(n)).toArray.asInstanceOf[Array[Raster]])
  def addDoubles(ns:Double*) = AddArray(ns.map(n => r(n)).toArray.asInstanceOf[Array[Raster]])

  test("average rasters in sequential groups to limit memory usage") {
    val limit = 30
    val ops:Seq[Op[Raster]] = (1.0 until 200.0 by 1.0).map { i:Double => Literal(r(i)) }
    val count = ops.length
 
    val dividedOps:Seq[Op[Raster]] = ops.map { rOp => local.Divide(rOp, count) }

    val firstRaster:Op[Raster] = dividedOps.head
    val groups = dividedOps.tail.grouped(limit).map { _.toArray }.toList

    val rOps:Op[Raster] = groups.foldLeft (firstRaster) ((oldResult:Op[Raster], newOps:Array[Op[Raster]]) => (logic.WithResult(oldResult)({ oldResult => local.Add( local.AddRasters(newOps:_*), oldResult) })) )
    val s = process.TestServer()
    val output = s.run(rOps)
  }

  test("avg double rasters concurrently") {
    val limit = 30
    val ops:Seq[Op[Raster]] = (1.0 until 200.0 by 1.0).map { i:Double => Literal(r(i)) }
    val count = ops.length

    val dividedOps:Seq[Op[Raster]] = ops.map { rOp => local.Divide(rOp, count) }
    val firstRaster:Op[Raster] = dividedOps.head

    val groups = dividedOps.tail.grouped(limit).map { _.toArray }.map(local.AddRasters( _:_* ) ) 
    val ops2 = local.AddArray( logic.CollectArray( groups.toArray ) )

    val s = process.TestServer()
    val output = s.run(ops2)
  }
}


