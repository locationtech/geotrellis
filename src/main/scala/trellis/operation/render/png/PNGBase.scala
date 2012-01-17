package trellis.operation.render.png

import trellis._
import trellis.operation._

trait PNGBase {
  val r:IntRasterOperation
  val colorBreaks:Array[(Int, Int)]
  val noDataColor:Int
  val transparent:Boolean
  def applyColorMap(z:Int): Int = {
    if (z == NODATA) return this.noDataColor
    colorBreaks.foreach {
      tpl => if (z <= tpl._1) return tpl._2
    }
    return colorBreaks(colorBreaks.length - 1)._2
  }
}
