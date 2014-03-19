/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.op

import geotrellis._
import geotrellis.process._
import geotrellis.raster.op._
import geotrellis.testkit._
import geotrellis.raster.op.local.Add

import org.scalatest.FunSuite

class AverageLotsOfRastersTest extends FunSuite 
                                  with TestServer {
  val e = Extent(0.0, 0.0, 10.0, 10.0)
  val re = RasterExtent(e, 1.0, 1.0, 10, 10)

  def r(n:Int) = Raster(Array.fill(100)(n), re)
  def r(n:Double) = Raster(Array.fill(100)(n), re)

  def addInts(ns:Int*) = Add(ns.map(n => Literal(r(n))))
  def addDoubles(ns:Double*) = Add(ns.map(n => r(n)))

  test("average rasters in sequential groups to limit memory usage") {
    val limit = 30
    val ops:Seq[Op[Raster]] = (1.0 until 200.0 by 1.0).map { i:Double => Literal(r(i)) }
    val count = ops.length
 
    val dividedOps:Seq[Op[Raster]] = ops.map { rOp => local.Divide(rOp, count) }

    val firstRaster:Op[Raster] = dividedOps.head
    val groups = dividedOps.tail.grouped(limit).map { _.toArray }.toList

    val rOps:Op[Raster] = groups.foldLeft (firstRaster) ((oldResult:Op[Raster], newOps:Array[Op[Raster]]) => (logic.WithResult(oldResult)({ oldResult => local.Add( local.Add(newOps), oldResult) })) )
    val output = get(rOps)
  }

  test("avg double rasters concurrently") {
    val limit = 30
    val ops:Seq[Op[Raster]] = (1.0 until 200.0 by 1.0).map { i:Double => Literal(r(i)) }
    val count = ops.length

    val dividedOps:Seq[Op[Raster]] = ops.map { rOp => local.Divide(rOp, count) }
    val firstRaster:Op[Raster] = dividedOps.head

    val groups = dividedOps.tail.grouped(limit).map(local.Add(_) ) 
    val ops2 = groups.toSeq.flaMapOps(seq => Add(seq))

    val output = get(ops2)
  }
}
