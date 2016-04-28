package geotrellis.spark.kernel.density

import scala.util.Random
import scala.math.{max,min}

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.stitch._

import geotrellis.vector._
import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.raster.mapalgebra.focal.Kernel._
import geotrellis.raster.VectorToRaster._

import org.scalatest._

class KernelDensitySpec extends FunSpec
    with Matchers
    with TestEnvironment
    with RasterMatchers {

  describe("Kernel Density Operation on RDD of features") {
    it("should produce the same result as local process") {
      // Generate points (random?)
      def randomPointFeature(extent: Extent) : PointFeature[Double] = {
        def randInRange (low : Double, high : Double) : Double = {
          val x = Random.nextDouble
          low * (1-x) + high * x
        }
        PointFeature(Point(randInRange(extent.xmin,extent.xmax),
			   randInRange(extent.ymin,extent.ymax)), 
		     Random.nextInt % 50 + 50)
      }

      val extent = Extent.fromString("-109,37,-102,41") // Colorado (is rect!)
      val pts = PointFeature(Point(-108,37.5),100.0) :: 
        (for (i <- 1 to 1000) yield randomPointFeature(extent)).toList

      assert(pts.forall (extent.contains(_)))

      // stamp kernels for points to local raster
      
      val krnwdth = 9.0
      val kern = circle(krnwdth.toInt,0,4)
      val trans = (_.toFloat.round.toInt) : Double => Int
      val full = kernelDensity (pts,trans,kern,RasterExtent(extent,700,400))

      // partition points into tiles

      val tl = TileLayout(7,4,100,100)
      val ld = LayoutDefinition(extent,tl)

      def ptfToExtent[D](ptf: PointFeature[D]) : Extent = {
        val p = ptf.geom
        Extent(p.x - krnwdth * ld.cellwidth / 2,
               p.y - krnwdth * ld.cellheight / 2,
               p.x + krnwdth * ld.cellwidth / 2,
               p.y + krnwdth * ld.cellheight / 2)
      }

      def ptfToSpatialKey[D](ptf: PointFeature[D]) :
          Seq[(SpatialKey,PointFeature[D])] = {
        val ptextent = ptfToExtent(ptf)
        val gridBounds = ld.mapTransform(ptextent)
        for ((c,r) <- gridBounds.coords;
             if r < tl.totalRows;
             if c < tl.totalCols) yield (SpatialKey(c,r), ptf)
      }

      val keyfeatures = pts.flatMap(ptfToSpatialKey).groupBy(_._1).map {
        case (sk,v) => (sk,v.unzip._2)
      }: Map[SpatialKey,List[PointFeature[Double]]]

      // stamp kernel to tiled raster

      val keytiles = keyfeatures.map { 
        case (sk,pfs) => (sk,kernelDensity (pfs,trans,kern,
                                        RasterExtent(ld.mapTransform(sk),
                                                     tl.tileDimensions._1,
                                                     tl.tileDimensions._2))) 
      }

      val tileList = 
        for { r <- 0 until ld.layoutRows
            ; c <- 0 until ld.layoutCols
            } yield {
              val k = SpatialKey(c,r)
              (k, keytiles.getOrElse(k, IntArrayTile.empty(tl.tileCols, 
                                                           tl.tileRows)))
            }
      val stitched = TileLayoutStitcher.stitch(tileList)._1

      // compare results

      assertEqual(stitched, full)
    }
  }

}
