package geotrellis.raster.op

import geotrellis._

sealed abstract class ColorMapType

case object GreaterThan extends ColorMapType
case object LessThan extends ColorMapType
case object Exact extends ColorMapType

case class ColorMapOptions(
  colorMapType:ColorMapType,
  /** Rgba value for NODATA */
  noDataColor:Int = 0x00000000, 
  /** Rgba value for data that doesn't fit the map */
  noMapColor:Int = 0x00000000,  
  /** Set to true to throw exception on unmappable variables */
  strict:Boolean = false        
)
object ColorMapOptions {
  val Default = ColorMapOptions(LessThan)
}

object ColorMap {
  def apply(r:Op[Raster],
            breaksToColors:Op[Map[Int,Int]]):IntColorMap = 
    IntColorMap(r,breaksToColors)

  def apply(r:Op[Raster],
            breaksToColors:Op[Map[Int,Int]],
            options:ColorMapOptions):IntColorMap = 
    IntColorMap(r,breaksToColors,options)

  def apply(r:Op[Raster],
            breaksToColors:Op[Map[Double,Int]]):DoubleColorMap =
    DoubleColorMap(r,breaksToColors)

  def apply(r:Op[Raster],
            breaksToColors:Op[Map[Double,Int]],
            options:ColorMapOptions):DoubleColorMap = 
    DoubleColorMap(r,breaksToColors,options)
}

case class IntColorMap(r:Op[Raster], 
                    breaksToColors:Op[Map[Int,Int]],
                    options:ColorMapOptions = ColorMapOptions.Default)
    extends Op2(r,breaksToColors)({
  (r,breaksToColors) =>
      val orderedBreaks:Array[Int] = 
        options.colorMapType match {
          case LessThan =>
            breaksToColors.keys.toArray.sorted
          case GreaterThan =>
            breaksToColors.keys.toArray.sorted.reverse
          case Exact =>
            breaksToColors.keys.toArray
        }

      val zCheck:(Int,Int)=>Boolean =
        options.colorMapType match {
          case LessThan =>
            { (z:Int,i:Int) => z > orderedBreaks(i) }
          case GreaterThan =>
            { (z:Int,i:Int) => z < orderedBreaks(i) }
          case Exact =>
            { (z:Int,i:Int) => z != orderedBreaks(i) }
        }

      val len = orderedBreaks.length

      Result({
        r.map { z =>
          if(z == NODATA) { options.noDataColor }
          else {
            var i = 0
            while(i < len && zCheck(z,i)) { i += 1 }
            if(i == len){
              if(options.strict) {
                sys.error(s"Value $z did not have an associated color and break")
              } else {
                options.noMapColor
              }
            } else {
              breaksToColors(orderedBreaks(i))
            }
          }
        }
      })
})

case class DoubleColorMap(r:Op[Raster], 
                          breaksToColors:Op[Map[Double,Int]],
                          options:ColorMapOptions = ColorMapOptions.Default)
    extends Op2(r,breaksToColors)({
  (r,breaksToColors) =>
      val orderedBreaks:Array[Double] = 
        options.colorMapType match {
          case LessThan =>
            breaksToColors.keys.toArray.sorted
          case GreaterThan =>
            breaksToColors.keys.toArray.sorted.reverse
          case Exact =>
            breaksToColors.keys.toArray
        }

      val zCheck:(Double,Int)=>Boolean =
        options.colorMapType match {
          case LessThan =>
            { (z:Double,i:Int) => z > orderedBreaks(i) }
          case GreaterThan =>
            { (z:Double,i:Int) => z < orderedBreaks(i) }
          case Exact =>
            { (z:Double,i:Int) => z != orderedBreaks(i) }
        }

      val len = orderedBreaks.length

      Result(
        r.mapDouble { z =>
          if(java.lang.Double.isNaN(z)) { options.noDataColor }
          else {
            var i = 0
            while(i < len && zCheck(z,i)) { i += 1 }

            if(i == len){
              if(options.strict) {
                sys.error(s"Value $z did not have an associated color and break")
              } else {
                options.noMapColor
              }
            } else {
              breaksToColors(orderedBreaks(i))
            }
          }
        }
      )
})
