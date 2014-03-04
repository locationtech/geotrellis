/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.benchmark

import geotrellis._
import geotrellis.process._
import geotrellis.source._
import geotrellis.io._
import geotrellis.data._
import geotrellis.raster.op.local._

import com.google.caliper.Param

object IOBenchmark extends BenchmarkRunner(classOf[IOBenchmark])
class IOBenchmark extends OperationBenchmark {
  @Param(Array("bit","byte","short","int","float","double"))
//  @Param(Array("float"))
  var rasterType = ""

  var size = 256

  val layers = 
    Map(
      ("bit","wm_DevelopedLand"),
      ("byte", "SBN_car_share"),
      ("short","travelshed-int16"),
      ("int","travelshed-int32"),
      ("float","aspect"), 
      ("double","aspect-double")
    )

  var path:String = ""
  var rasterExtent:RasterExtent = null
  var typ:RasterType = TypeFloat

  var targetExtent:RasterExtent = null

  override def setUp() {
    val id = layers(rasterType)
    val layer = GeoTrellis.get(LoadRasterLayer(id)).asInstanceOf[ArgFileRasterLayer]
    path = layer.rasterPath
    typ = layer.info.rasterType
    rasterExtent = RasterSource(id).get.rasterExtent
    val RasterExtent(Extent(xmin,ymin,xmax,ymax),cw,ch,cols,rows) =
      rasterExtent

    val xdelta = (xmax - xmin) / 1.5
    val ydelta = (ymax - ymin) / 1.5
    val extent = Extent(xmin,ymin,xmin+xdelta,ymin+ydelta)
    targetExtent = RasterExtent(extent,size,size)
  }

  def timeLoadRaster(reps:Int) = run(reps)(loadRaster)
  def loadRaster = { GeoTrellis.get(LoadRaster(layers(rasterType))) }

  def timeRasterSource(reps:Int) = run(reps)(rasterSource)
  def rasterSource = { RasterSource(layers(rasterType)).get }

  def timeLoadRasterWithExtent(reps:Int) = run(reps)(loadRasterWithExtent)
  def loadRasterWithExtent = { GeoTrellis.get(LoadRaster(layers(rasterType),targetExtent)) }

  def timeRasterSourceWithExtent(reps:Int) = run(reps)(rasterSourceWithExtent)
  def rasterSourceWithExtent = { RasterSource(layers(rasterType),targetExtent).get }

  def timeNewReader(reps:Int) = run(reps)(newReader)
  def newReader = { arg.ArgReader.read(path,typ,rasterExtent) }

  def timeOldReader(reps:Int) = run(reps)(oldReader)
  def oldReader =
    new io.ArgReader(path).readPath(typ,rasterExtent,None)

  def timeNewReaderWithExtent(reps:Int) = run(reps)(newReaderWithExtent)
  def newReaderWithExtent = { 
    val r = arg.ArgReader.read(path,typ,rasterExtent,targetExtent)
  }

  def timeOldReaderWithExtent(reps:Int) = run(reps)(oldReaderWithExtent)
  def oldReaderWithExtent =
    new io.ArgReader(path).readPath(typ,rasterExtent,Some(targetExtent))
}

object ReadAndWarpBenchmark extends BenchmarkRunner(classOf[ReadAndWarpBenchmark])
class ReadAndWarpBenchmark extends OperationBenchmark {
  @Param(Array("bit","byte","short","int","float","double"))
  var rasterType = ""

  val layers = 
    Map(
      ("bit","wm_DevelopedLand"),
      ("byte", "SBN_car_share"),
      ("short","travelshed-int16"),
      ("int","travelshed-int32"),
      ("float","aspect"), 
      ("double","aspect-double")
    )

  @Param(Array("256", "512", "979", "1400", "2048", "4096"))
  var size = 0

  var path:String = ""
  var extent:RasterExtent = null
  var typ:RasterType = TypeFloat

  var targetExtent:RasterExtent = null

  override def setUp() {
    val id = layers(rasterType)
    val layer = GeoTrellis.get(LoadRasterLayer(id)).asInstanceOf[ArgFileRasterLayer]
    path = layer.rasterPath
    typ = layer.info.rasterType
    extent = RasterSource(id).get.rasterExtent
    targetExtent = RasterExtent(extent.extent,size,size)
  }

  def timeNewReaderWithExtent(reps:Int) = run(reps)(newReaderWithExtent)
  def newReaderWithExtent = { 
    val r = arg.ArgReader.read(path,typ,extent,targetExtent) 
  }

  def timeNewReaderWithWarp(reps:Int) = run(reps)(newReaderWithWarp)
  def newReaderWithWarp = { 
    val r = arg.ArgReader.read(path,typ,extent) 
    r.warp(targetExtent)
  }

  def timeOldReaderWithExtent(reps:Int) = run(reps)(oldReaderWithExtent)
  def oldReaderWithExtent =
    new io.ArgReader(path).readPath(typ,extent,Some(targetExtent))
}

object SmallTileReadAndWarpBenchmark extends BenchmarkRunner(classOf[SmallTileReadAndWarpBenchmark])
class SmallTileReadAndWarpBenchmark extends OperationBenchmark {
  @Param(Array("bit","byte","short","int","float","double"))
  var rasterType = ""

  var size = 256

  val layers = 
    Map(
      ("bit","wm_DevelopedLand"),
      ("byte", "SBN_car_share"),
      ("short","travelshed-int16"),
      ("int","travelshed-int32"),
      ("float","aspect"), 
      ("double","aspect-double")
    )

  var path:String = ""
  var extent:RasterExtent = null
  var typ:RasterType = TypeFloat

  var rasterExtent:RasterExtent = null
  var targetExtent:RasterExtent = null

  override def setUp() {
    val id = layers(rasterType)

    val layer = GeoTrellis.get(LoadRasterLayer(id)).asInstanceOf[ArgFileRasterLayer]
    path = layer.rasterPath
    typ = layer.info.rasterType
    rasterExtent = RasterSource(id).get.rasterExtent
    val RasterExtent(Extent(xmin,ymin,xmax,ymax),cw,ch,cols,rows) =
      rasterExtent

    val extent = Extent(xmin,ymin,(xmin+xmax)/2.0,(ymin+ymax)/2.0)
    targetExtent = RasterExtent(extent,size,size)
  }

  def timeNewReaderWithExtent(reps:Int) = run(reps)(newReaderWithExtent)
  def newReaderWithExtent = { 
    val r = arg.ArgReader.read(path,typ,rasterExtent,targetExtent) 
  }

  def timeNewReaderWithWarp(reps:Int) = run(reps)(newReaderWithWarp)
  def newReaderWithWarp = { 
    val r = arg.ArgReader.read(path,typ,rasterExtent) 
    r.warp(targetExtent)
  }

  def timeOldReaderWithExtent(reps:Int) = run(reps)(oldReaderWithExtent)
  def oldReaderWithExtent =
    new io.ArgReader(path).readPath(typ,rasterExtent,Some(targetExtent))
}


/** Reading the same raster as a .tif (with GeoTools) and as an ARG with GeoTrellis */
object GeoTiffVsArgBenchmark extends BenchmarkRunner(classOf[GeoTiffVsArgBenchmark])
class GeoTiffVsArgBenchmark extends OperationBenchmark {
  def timeRasterSource(reps:Int) = run(reps)(rasterSource)
  def rasterSource = { RasterSource("aspect").get }

  def timeLoadGeoTiff(reps:Int) = run(reps)(loadGeoTiff)
  def loadGeoTiff = { RasterSource("aspect-tif").get }
}

object TileIOBenchmark extends BenchmarkRunner(classOf[TileIOBenchmark])
class TileIOBenchmark extends OperationBenchmark {
  var targetExtent:RasterExtent = null

  override def setUp() {
    val info = RasterSource("mtsthelens_tiled").info.get
    val re = info.rasterExtent
    val Extent(xmin,_,_,ymax) = re.extent
    val te = Extent(xmin,xmin + (re.extent.width / 2.0), ymax - (re.extent.height / 2.0),ymax)
    targetExtent = RasterExtent(te,re.cols / 2, re.rows / 2)
  }

  def timeLoadRaster(reps:Int) = run(reps)(loadRaster)
  def loadRaster = { GeoTrellis.get(LoadRaster("mtsthelens_tiled")) }

  def timeRasterSource(reps:Int) = run(reps)(rasterSource)
  def rasterSource = { RasterSource("mtsthelens_tiled").get }

  def timeLoadRasterWithExtent(reps:Int) = run(reps)(loadRasterWithExtent)
  def loadRasterWithExtent = { GeoTrellis.get(LoadRaster("mtsthelens_tiled",targetExtent)) }

  def timeRasterSourceWithExtent(reps:Int) = run(reps)(rasterSourceWithExtent)
  def rasterSourceWithExtent = { RasterSource("mtsthelens_tiled",targetExtent).get }

  def timeRasterSourceAndThenWarp(reps:Int) = run(reps)(rasterSourceAndThenWarp)
  def rasterSourceAndThenWarp = { RasterSource("mtsthelens_tiled").warp(targetExtent).get }
}
