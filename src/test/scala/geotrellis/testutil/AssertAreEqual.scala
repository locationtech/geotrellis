package geotrellis.testutil

import geotrellis._
import geotrellis.raster.op.local._

import org.scalatest.matchers._

case class AssertAreEqual(r1:Op[Raster], r2:Op[Raster],threshold:Double) extends BinaryLocal with ShouldMatchers {
  def handle(z1:Int, z2:Int) = {
    if (z1 == NODATA) { z2 should equal (NODATA) ; 0 }
    else if (z2 == NODATA) { z1 should equal (NODATA) ; 0 }
    else { (z1 - z2) should be <= (threshold.toInt) ; 0 }
  }

  def handleDouble(z1:Double, z2:Double) = {
    if (java.lang.Double.isNaN(z1)) { println("z1 has NaN") ; z2 should equal (Double.NaN) ; 0 }
    else if (java.lang.Double.isNaN(z2)) { println("z2 has NaN") ;z1 should equal (Double.NaN) ; 0 }
    else { (z1 - z2) should be <= (threshold) ; 0 }
  }
}

