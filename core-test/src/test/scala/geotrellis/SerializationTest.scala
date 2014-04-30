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

package geotrellis

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import java.io._


import geotrellis._
import geotrellis.testkit._
import geotrellis.raster.op._
import geotrellis.statistics._
import geotrellis.feature._

class SerializationTest extends FunSuite 
                        with ShouldMatchers 
                        with RasterBuilders 
                        with TestServer {

  // Operations and data objects that may be sent remotely must be serializable.
  test("Operation and data object serialization test") {
    pickle(Literal(1))
    pickle(byteRaster)
    val addOp = local.Add(byteRaster, 1)
    pickle(addOp)
    pickle(local.Add(addOp, 2))
    pickle(FastMapHistogram())
    pickle(Statistics(0,0,0,0,0,0))
    pickle(Point(0,0))
    pickle(Polygon( Line(Point(1,9) :: Point(1,6) :: Point(4,6) :: Point(4,9) :: Point(1,9) :: Nil)))
  }

  test("Tile Rasters are serializable") {
    pickle(run(io.LoadRaster("mtsthelens_tiled")))
  }

  def pickle(o:AnyRef) = {
    val stream = new ObjectOutputStream(new ByteArrayOutputStream())
    stream.writeObject(o)
  } 
}
