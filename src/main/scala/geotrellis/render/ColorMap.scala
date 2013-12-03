package geotrellis.render

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

  def apply(nd:Int):ColorMapOptions =
    ColorMapOptions(LessThan,nd)
}

object ColorMap {
  def apply(breaksToColors:Map[Int,Int]):IntColorMap = 
    IntColorMap(breaksToColors)

  def apply(breaksToColors:Map[Int,Int],
            options:ColorMapOptions):IntColorMap = 
    IntColorMap(breaksToColors,options)

  def apply(breaksToColors:Map[Double,Int]):DoubleColorMap =
    DoubleColorMap(breaksToColors)

  def apply(breaksToColors:Map[Double,Int], options:ColorMapOptions):DoubleColorMap = 
    DoubleColorMap(breaksToColors,options)

  def apply(breaks:Array[Int],color:Array[Int]):IntColorMap =
    apply(breaks,color,ColorMapOptions.Default)

  def apply(breaks:Array[Int],colors:Array[Int],options:ColorMapOptions):IntColorMap = {
    val breaksToColors:Map[Int,Int] =
      breaks.zip(colors)
            .toMap

    apply(breaksToColors,options)
  }
}

trait ColorMap {
  def colors:Seq[Int]
  val options:ColorMapOptions

  var _opaque = true
  var _grey = true
  var _colorsChecked = false

  private def checkColors() = {
    var opaque = true
    var grey = true
    var i = 0
    while (i < colors.length) {
      val c = colors(i)
      opaque &&= Color.isOpaque(c)
      grey &&= Color.isGrey(c)
      i += 1
    }
  }

  def opaque = 
    if(_colorsChecked) { _opaque }
    else {
      checkColors()
      _opaque
    }

  def grey = 
    if(_colorsChecked) { _grey }
    else {
      checkColors()
      _grey
    }

  def render(r:Raster):Raster
}

case class IntColorMap(breaksToColors:Map[Int,Int],
                      options:ColorMapOptions = ColorMapOptions.Default) 
    extends ColorMap with Function[Int,Int] {
  lazy val colors = breaksToColors.values.toList
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

  def apply(z:Int) = {
    if(isNoData(z)) { options.noDataColor }
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

  def render(r:Raster) = 
    r.convert(TypeByte).map(apply)
}

case class DoubleColorMap(breaksToColors:Map[Double,Int],
                          options:ColorMapOptions = ColorMapOptions.Default) 
    extends ColorMap with Function1[Double,Int] {
  lazy val colors = breaksToColors.values.toList
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

  def apply(z:Double) = {
    if(isNoData(z)) { options.noDataColor }
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

  def render(r:Raster) =
    r.convert(TypeByte).mapDouble(apply)
}
