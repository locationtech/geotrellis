package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster.op.local._
import geotrellis.process._
import geotrellis.raster.op._

import geotrellis.testutil._

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.matchers._
import org.scalatest.junit.JUnitRunner

import scala.math._

@RunWith(classOf[JUnitRunner])
class MaxSpec extends FunSpec with ShouldMatchers 
                              with TestServer 
                              with RasterBuilders {
  describe("Max") {
    it("should agree with GRASS computed raster") {
      val threshold = 0.1
      // Expected data, from GRASS
      val grassComputedMax = io.LoadRaster("elevation-focalmax")

      val elevationOp = io.LoadRaster("elevation")
      val maxOp = focal.Max(elevationOp,Square(1))

      var mx = run(maxOp)
      var el = run(elevationOp)
      var gm = run(grassComputedMax)
      
      println(s"mx row ${mx.rows} col ${mx.cols}")
      println(s"el row ${el.rows} col ${el.cols}")
      println(s"gm row ${gm.rows} col ${gm.cols}")

      var x = 0
      var y = 0
      while(y < mx.rows) {
        while(x < mx.cols) {
          val v = mx.get(x,y)
          val gv = gm.get(x,y)
          if(v != Double.NaN) { print(".") }
          if(v == Double.NaN) {
            println()
            println(s" FOUND NaN at (${x},${y})")
            println(" Neighborhood:")
            println("    %f   %f   %f ".format(el.get(x-1,y-1),el.get(x,y-1),el.get(x+1,y-1)))
            println("    %f   %f   %f ".format(el.get(x-1,y),el.get(x,y),el.get(x+1,y)))
            println("    %f   %f   %f ".format(el.get(x-1,y+1),el.get(x,y+1),el.get(x+1,y+1)))
          }
          if(abs(v - gv) > 2) {
            println()
            println(s" FOUND difference at (${x},${y}):  ours: ${v}  theirs: ${gv}  diff:${abs(v-gv)}")
            println(" Neighborhood:")
            if(x == 0) {
              if(y > 0) { println("    X   %f   %f ".format(el.get(x,y-1),el.get(x+1,y-1))) }
                        else { println("    X    X    X") }
              println("    X   %f   %f ".format(el.get(x,y),el.get(x+1,y)))
              if(y < mx.rows-1) { println("    X   %f   %f ".format(el.get(x,y+1),el.get(x+1,y+1))) }
                        else { println("    X    X    X") }
            } else if(x == mx.cols - 1) {
              if(y > 0) { println("    %f   %f   X ".format(el.get(x-1,y-1),el.get(x,y-1))) }
                        else { println("    X    X    X") }
              println("    %f   %f   X ".format(el.get(x-1,y),el.get(x,y)))
              if(y < mx.rows-1) { println("    %f   %f   X ".format(el.get(x-1,y+1),el.get(x,y+1))) }
                        else { println("    X    X    X") }
            }
            else {
              if(y > 0) { println(s"    ${el.get(x-1,y-1)}   ${el.get(x,y-1)}   ${el.get(x+1,y-1)}") }
                        else { println("    X    X    X") }
              println(s"    ${el.get(x-1,y)}   ${el.get(x,y)}   ${el.get(x+1,y)}")
              if(y < mx.rows-1) { println(s"    ${el.get(x-1,y+1)}   ${el.get(x,y+1)}   ${el.get(x+1,y+1)}") }
                        else { println("    X    X    X") }
              if(y < mx.rows-2) { println(s"    ${el.get(x-1,y+2)}   ${el.get(x,y+2)}   ${el.get(x+1,y+2)}") }
                        else { println("    X    X    X") }
            }

          }
          x += 1
        }
        y += 1
      }
      println()

      //Compare the two's values
      run(AssertAreEqual(maxOp, grassComputedMax,10.0))
    }

    it("should agree with a manually worked out example") {
      val r = createRaster(Array[Int](1,1,1,1,
                                      2,2,2,2,
                                      3,3,3,3,
                                      1,1,4,4))

      val maxOp = focal.Max(r,Square(1))
      assertEqual(maxOp, Array[Int](2,2,2,2,
                                    3,3,3,3,
                                    3,4,4,4,
                                    3,4,4,4))
    }
  }
}

