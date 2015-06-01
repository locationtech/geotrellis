package geotrellis.raster.interpolation

import geotrellis.engine.RasterSource
import geotrellis.raster._
//import geotrellis.testkit.{TestEngine, TileBuilders}
import geotrellis.testkit._
import geotrellis.vector._
import geotrellis.vector.io.json.JsonFeatureCollection
import geotrellis.vector.io.json._

import geotrellis.raster._
import geotrellis.vector.Extent

import org.scalatest._

class KrigingInterpolationSpec extends FunSpec
                                  with Matchers
                                  with TestEngine
                                  with TileBuilders{

  val B = 5 // value returned when interpolating

  val tile = ArrayTile(Array[Int](100, 100, 100, 100), 2, 2)
  val extent = Extent(1, 1, 2, 2)

  val re = RasterExtent(Extent(0,0,9,10),1,1,9,10)
  val points = Seq[PointFeature[Int]](
    PointFeature(Point(0.0,0.0),10),
    PointFeature(Point(0.0,0.0),16),
    PointFeature(Point(1.0,0.0),20),
    PointFeature(Point(0.0,1.0),24),
    PointFeature(Point(2.0,2.0),50)
  )

  val radius = Some(6)
  val lag = 2
  val chunkSize = 100
  val pointPredict = Point(1,1)
  val krigingObject = new KrigingSimpleInterpolation(KrigingSimple, points, re, radius, chunkSize, lag, Linear) {}

  def checkType(obj: Any) = obj match{
    case n: Tile => true
    case _ => false
  }

  describe("Kriging Simple Interpolation") {

    it("Checking the structure of the Simple Kriging model") {
      //krigingObject.interpolate(Point(0.0,0.0)).getClass.getSimpleName should be (Tile.getClass.getSimplename)
      assert(checkType(krigingObject.interpolate(pointPredict)))
    }
    /*
    ignore("Kriging Simple Interpolation w/ parameters") {
    }
    */
  }
}
