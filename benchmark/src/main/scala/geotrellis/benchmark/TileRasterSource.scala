package geotrellis.benchmark

import geotrellis._
import geotrellis.raster.op.local._

import com.google.caliper.Param

object TiledRasterSource extends BenchmarkRunner(classOf[TiledRasterSource])
class TiledRasterSource extends OperationBenchmark {
  // @Param(Array("64", "128", "256", "512", "1024", "2048", "4096", "10000"))
  // var size:Int = 0

  // var op:Op[Raster] = null

  // override def setUp() {
  //   val r:Raster = loadRaster("SBN_farm_mkt", size, size)
  //   op = Add(r, 13)
  // }

  // def timeConstantAdd(reps:Int) = run(reps)(constantAdd)
  // def constantAdd = get(op)

  // TODO

  // BigMin
}

// class BigMinTiled extends OperationBenchmark{ 
//   var tiledMinOp:Op[Int] = null
//   var tiledHistogramOp:Op[Histogram] = null

//   var tileN = 10

//   override def setUp() {
//     val size = tileN * 2000
//     println("Setting up raster of size %d x %d." format (size, size))
//     val layout = TileLayout(tileN,tileN,2000,2000)
//     val e = Extent(0.0, 0.0, (tileN * 2000.0), (tileN * 2000.0))
//     val re = RasterExtent(e, 1.0, 1.0, tileN * 2000, tileN * 2000)

//     val layer = new TileSetRasterLayer(RasterLayerInfo(LayerId("bit"),
//                                                        TypeByte,
//                                                        re,
//                                                        0,0,0),
//                                        "/tmp",
//                                        layout)

//     // val trd = layer.getData()
//     // val tileSetRD = TileSetRasterData("/tmp", "big", TypeByte, layout, layer.getTileLoader)
//     // val raster = Raster(tileSetRD, re)
//     // tiledMinOp = BTileMin(Add(Add(raster,2), raster))
//     // tiledHistogramOp = BTileHistogram(raster)
//   }

//   def timeMin(reps:Int) = run(reps)(min)
//   def min = { 
//     val min = get(tiledMinOp)
//     println("Found min: %d" format (min))
//   }
  
//   def timeHistogram(reps:Int) = run(reps)(histogram)
//   def histogram = {
//     val h = get(tiledHistogramOp)
//     println("Found histogram: %s" format (h.toJSON))
//   }
// }

// object BigMinTiled {
//   def main(args:Array[String]) = {
//     val tileN = args(0).toInt
//     val test = new BigMinTiled
//     test.tileN = tileN
//     val runs = 2
//     println("Starting setup.")
//     test.setUp()
//     for (i <- 0 until 3) {
//       println("Starting test.")
//       val start = System.currentTimeMillis
//       //test.min
//       test.histogram
//       val elapsed = System.currentTimeMillis - start
//       println("Test complete: %d millis" format (elapsed))
//       val cells = tileN * 2000L * tileN * 2000L
//       val rate = cells / elapsed / 1000L
//       println("Rate: %d k/ms" format (rate) )
//     }
//   }
// }


// object MinTiled extends BenchmarkRunner(classOf[MinTiled])
// class MinTiled extends OperationBenchmark {
//   @Param(Array("4096"))
//   var size:Int = 0

//   //@Param(Array("256", "512"))
//   var pixels:Int = 1024

//   var tiledOp:Op[Int] = null
//   var tiledArrayOp:Op[Int] = null
//   var tiledLazyOp:Op[Int] = null
//   var rawOp:Op[Raster] = null

//   var normalUntiledOp:Op[Int] = null
//   var normalTiledOp:Op[Int] = null 

//   //def makeOp(r:Op[Raster]):Operation[Raster] = r
//   def makeOp(r:Op[Raster]) = Add(Add(r, 5),r)

//   override def setUp() {
//     val r:Raster = loadRaster("SBN_farm_mkt", size, size)
// //    val (tiledRaster, tiledArrayRaster, lazyRaster) = createTiledRaster(r, pixels, pixels)

//     // tiledOp = BTileMin(makeOp(tiledRaster))
//     // tiledArrayOp = BTileMin(makeOp(tiledArrayRaster))
//     // tiledLazyOp = stat.Min(makeOp(lazyRaster))

//     // run on a normal raster
//     // normalUntiledOp = UntiledMin(makeOp(r))
//     // normalTiledOp = BTileMin(makeOp(r))

//     rawOp = makeOp(r)
//   }

//   //def timeTiledMin(reps:Int) = run(reps)(tiledMin)
//   def tiledMin = get(tiledOp)
 
//   def timeTiledArrayMinOp(reps:Int) = run(reps)(tiledArrayMin)
//   def tiledArrayMin = get(tiledArrayOp)

//   //def timeTiledLazyOp(reps:Int) = run(reps)(tiledLazyMin)
//   // def tiledLazyMin = get(tiledLazyOp)

//   def timeNormalUntiledOp(reps:Int) = run(reps)(runNormalUntiledOp)
//   def runNormalUntiledOp = get(normalUntiledOp)

//   def timeRawOp(reps:Int) = run(reps)(runRawOp)
//   def runRawOp = get(rawOp)

//   def timeNormalTiledOp(reps:Int) = run(reps)(runNormalTiledOp)
//   def runNormalTiledOp = get(normalTiledOp)
// }


// // TODO
// // object HistogramTiled extends BenchmarkRunner(classOf[HistogramTiled])
// // class HistogramTiled extends OperationBenchmark {
// //   @Param(Array("6000"))
// //   var size:Int = 0

// //   //@Param(Array("256", "512"))
// //   var pixels:Int = 1500

// //   var tiledOp:Op[Histogram] = null
// //   var tiledArrayOp:Op[Histogram] = null
// //   var tiledArrayForceOp:Op[Histogram] = null

// //   var tiledLazyOp:Op[Histogram] = null
// //   var rawOp:Op[Raster] = null

// //   // var normalUntiledOp:Op[Histogram] = null
// //   // var normalUntiledLazyOp:Op[Histogram] = null

// //   // var normalTiledOp:Op[Histogram] = null 

// //   //def makeOp(r:Raster) = Add(Add(r, 5), r)
// //   def makeOp(r:Raster) = Add(r, r)
// //   //def makeOp(r:Raster) = Add(r, 5)
// //   //def makeOp(r:Raster) = Literal(r)

// //   override def setUp() {
// //     val r:Raster = loadRaster("SBN_farm_mkt", size, size)
// //     val (tiledRaster, tiledArrayRaster, lazyRaster) = createTiledRaster(r, pixels, pixels)

// //     tiledOp = BTileHistogram(makeOp(tiledRaster))
// //     tiledArrayOp = BTileHistogram(makeOp(tiledArrayRaster))
// //     tiledArrayForceOp = BTileForceHistogram(makeOp(tiledArrayRaster))
// //     tiledLazyOp = stat.GetHistogram(makeOp(lazyRaster))

// //     // run on a normal raster
// //     // normalUntiledOp = BUntiledHistogram(makeOp(r))
// //     // normalUntiledLazyOp = BUntiledHistogram(makeOp(r.d))
    
// //     // normalTiledOp = BTileHistogram(makeOp(r.defer))

// //     rawOp = Force(makeOp(r))
// //   }

// //   def timeTiledHistogram(reps:Int) = run(reps)(tiledHistogram)
// //   def tiledHistogram = get(tiledOp)
  
// //   def timeRawOp(reps:Int) = run(reps)(runRawOp)
// //   def runRawOp = get(rawOp)
  
// //   def timeNormalUntiledOp(reps:Int) = run(reps)(runNormalUntiledOp)
// //   def runNormalUntiledOp = get(normalUntiledOp)
  
// //   def timeNormalUntiledLazyOp(reps:Int) = run(reps)(runNormalUntiledLazyOp)
// //   def runNormalUntiledLazyOp = get(normalUntiledLazyOp)
  
// //   def timeTiledArrayHistogramOp(reps:Int) = run(reps)(tiledArrayHistogram)
// //   def tiledArrayHistogram = get(tiledArrayOp)

// //   def timeTiledArrayForce(reps:Int) = run(reps)(tiledArrayForce)
// //   def tiledArrayForce = get(tiledArrayForceOp)

// //   def timeTiledLazyOp(reps:Int) = run(reps)(tiledLazyHistogram)
// //   def tiledLazyHistogram = get(tiledLazyOp)
  
// //   def timeNormalTiledOp(reps:Int) = run(reps)(runNormalTiledOp)
// //   def runNormalTiledOp = get(normalTiledOp)
// // }

