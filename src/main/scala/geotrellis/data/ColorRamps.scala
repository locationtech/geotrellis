package geotrellis.data

import scala.math.round

import geotrellis._

abstract class ColorRamp {
  val colors:Array[Int]
}

object ColorRamps {
  /**
   * A 6 color sequential red-amber-green color ramp. 
   */ 
  case object RedToAmberToGreen extends ColorRamp {
    val colors = Array(0xFA0000, 0xFB3300, 0xFC6600, 0xFD9900, 
                       0xFECC00, 0xCCFF00, 0x99FF00, 0x66FF00, 
                       0x33FF00, 0x00FF00).map(Color.rgbToRgba(_))
  }

  /**
   * A 9 class divergent Blue to Yellow to Red color ramp.
   */
  case object BlueToYellowToRed extends ColorRamp { 
    val colors = Array(0x4575B4,0x74ADD1,0xABD9E9,0xE0F3F8,0xFFFFBF,0xFEE090,0xFDAE61,0xF46D43,0xD73027).map(Color.rgbToRgba(_))
  }

  /**
   * 9 class divergent cold to hot color ramp.
   */
  case object ColdToHot extends ColorRamp {
    val colors = Array(0x3288BD,0x66C2A5,0xABDDA4,0xE6F598,0xFFFFBF,0xFEE08B,0xFDAE61,0xF46D43,0xD53E4F).map(Color.rgbToRgba(_))
  }

  /**
   * 9 class sequential orange to red color ramp.
   */
  case object OrangeToRed extends ColorRamp {
    val colors = Array(0xFFFFCC,0xFFEDA0, 0xFED976,0xFEB24C,0xFD8D3C,0xFC4E2A,0xE31A1C,0xBD0026,0x800026).map(Color.rgbToRgba(_))
  }

  /**
   * 9 class sequential blue to purple color ramp.
   */
  case object BlueToPurple extends ColorRamp {
    val colors = Array(0xFCFBFD, 0xEFEDF5, 0xDADAEB, 0xBCBDDC, 0x9E9AC8, 0x807DBA, 0x6A51A3, 0x54278F,0x3F007D).map(Color.rgbToRgba(_))
  }

  /**
   * 9 class divergent purple to orange color ramp.
   */
  case object PurpleToOrange extends ColorRamp {
    val colors = Array(0xB35806,0xE08214,0xFDB863,0xFEE0B6,0xF7F7F7,0xD8DAEB,0xB2ABD2,0x8073AC,0x542788).map(Color.rgbToRgba(_))
  }
  
}
