package geotrellis.testkit

import geotrellis._
import geotrellis.raster.op.local._

import scala.math._

import org.scalatest.matchers._

object AssertAreEqual {
  def apply(r1:Op[Raster], r2:Op[Raster], threshold:Double) = {
    (r1,r2).map(_.dualCombine(_)((z1:Int, z2:Int) => {
        println(s"${z1}")
          if (isNoData(z1)) {
            if(isData(z2))
              sys.error(s"AssertEqual failed: MISMATCH z1 = ${z1}  z2 = ${z2}")
            0
          } else if (isNoData(z2)) {
            sys.error(s"AssertEqual failed: MISMATCH z1 = ${z1}  z2 = ${z2}")
          } else {
            if(abs(z1 - z2) > threshold)
              sys.error(s"AssertEqual failed: MISMATCH z1 = ${z1}  z2 = ${z2}")
            0
          }
    })((z1:Double, z2:Double) => {
        if (isNoData(z1)) {
          if(isData(z2))
            sys.error(s"AssertEqual failed: MISMATCH z1 = ${z1}  z2 = ${z2}")
          0.0
        } else if (isNoData(z2)) {
          sys.error(s"AssertEqual failed: MISMATCH z1 = ${z1}  z2 = ${z2}")
        } else {
          if(abs(z1 - z2) > threshold)
            sys.error(s"AssertEqual: MISMATCH z1 = ${z1}  z2 = ${z2}")
          0.0
        }
      })
    )
  }
}
  

