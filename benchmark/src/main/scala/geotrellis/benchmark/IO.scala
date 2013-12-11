package geotrellis.benchmark

import geotrellis._
import geotrellis.process._
import geotrellis.source._
import geotrellis.io._
import geotrellis.data._
import geotrellis.raster.op.local._

import com.google.caliper.Param

object IOBenchmarks extends BenchmarkRunner(classOf[IOBenchmarks])
class IOBenchmarks extends OperationBenchmark {
  data.GeoTiffRasterLayerBuilder.addToCatalog

  @Param(Array("0", "1", "3", "4", "5", "6", "7"))
//  @Param(Array("7"))
  var index = 0

  val layers = 
    Array("SBN_car_share",
      "SBN_co_phila",
      "SBN_farm_mkt",
      "SBN_inc_percap",
      "SBN_recycle",
      "SBN_RR_stops_walk",
      "SBN_street_den_1k",
      "aspect")

  var path:String = ""
  var extent:RasterExtent = null
  var typ:RasterType = TypeFloat

  var targetExtent:RasterExtent = null

  override def setUp() {
    val layer = GeoTrellis.get(LoadRasterLayer(layers(index))).asInstanceOf[ArgFileRasterLayer]
    path = layer.rasterPath
    typ = layer.info.rasterType
    extent = RasterSource(layers(index)).get.rasterExtent
    targetExtent = RasterExtent(extent.extent,600,600)
  }

  def timeLoadRaster(reps:Int) = run(reps)(loadRaster)
  def loadRaster = { GeoTrellis.get(LoadRaster(layers(index))) }

  def timeRasterSource(reps:Int) = run(reps)(rasterSource)
  def rasterSource = { RasterSource(layers(index)).get }

  def timeNewReader(reps:Int) = run(reps)(newReader)
  def newReader = { arg.ArgReader.read(path,typ,extent) }

  def timeOldReader(reps:Int) = run(reps)(oldReader)
  def oldReader =
    new io.ArgReader(path).readPath(typ,extent,None)

  def timeNewReaderWithExtent(reps:Int) = run(reps)(newReaderWithExtent)
  def newReaderWithExtent = { 
    val r = arg.ArgReader.read(path,typ,extent) 
    r.warp(targetExtent)
  }

  def timeOldReaderWithExtent(reps:Int) = run(reps)(oldReaderWithExtent)
  def oldReaderWithExtent =
    new io.ArgReader(path).readPath(typ,extent,Some(targetExtent))
}

object GeoTiffVsArgBenchmarks extends BenchmarkRunner(classOf[GeoTiffVsArgBenchmarks])
class GeoTiffVsArgBenchmarks extends OperationBenchmark {
  def timeRasterSource(reps:Int) = run(reps)(rasterSource)
  def rasterSource = { RasterSource("aspect").get }

  def timeLoadGeoTiff(reps:Int) = run(reps)(loadGeoTiff)
  def loadGeoTiff = { RasterSource("aspect-tif").get }
}

object TileIOBenchmarks extends BenchmarkRunner(classOf[TileIOBenchmarks])
class TileIOBenchmarks extends OperationBenchmark {
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
