package trellis.data

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ColorSpec extends Spec with MustMatchers {
  describe("LinearColorRangeChooser(FF0000,0000FF)") {
    val c = new LinearColorRangeChooser(0xFF0000, 0x0000FF)
    val c2 = new MultiColorRangeChooser(Array(0xFF0000, 0x0000FF))

    it("should provide 1 color") {
      c.getColorString(1) must be === "FF0000"
      c2.getColorString(1) must be === "FF0000"
    }

    it("should provide 2 colors") {
      c.getColorString(2) must be === "FF0000,0000FF"
      c2.getColorString(2) must be === "FF0000,0000FF"
    }

    it("should provide 3 colors") {
      c.getColorString(3) must be === "FF0000,80007F,0000FF"
      c2.getColorString(3) must be === "FF0000,80007F,0000FF"
    }

    it("should provide 9 colors") {
      c.getColorString(9) must be === "FF0000,E0001F,C0003F,A0005F,80007F,60009F,4000BF,2000DF,0000FF"
      c2.getColorString(9) must be === "FF0000,E0001F,C0003F,A0005F,80007F,60009F,4000BF,2000DF,0000FF"
    }

    it("should unzip colors") {
      val (r, g, b) = Color.unzipRGB(0xFF9900)
      r must be === 0xFF
      g must be === 0x99
      b must be === 0x00
    }
  }

  describe("MultiColorRangeChooser()") {
    it("should work 1") {
      val baseColors = Array(0x0000FF)
      val chooser = new MultiColorRangeChooser(baseColors)
      val colors = chooser.getColors(baseColors.length)
      colors.toList must be === baseColors.toList
    }

    it("should work 2") {
      val baseColors = Array(0x0000FF, 0xFF0000)
      val chooser = new MultiColorRangeChooser(baseColors)
      val colors = chooser.getColors(baseColors.length)
      colors.toList must be === baseColors.toList
    }

    it("should work 3") {
      val baseColors = Array(0x0000FF, 0x00FF00, 0xFF0000)
      val chooser = new MultiColorRangeChooser(baseColors)
      val colors = chooser.getColors(baseColors.length)
      colors.map{ "%06x".format(_) }.toList must be === baseColors.map { "%06x".format(_) }.toList
    }

    it("should work 6") {
      val baseColors = Array(0x0000FF, 0x0080FF, 0x00FF80, 0xFFFF00, 0xFF8000, 0xFF0000)
      val chooser = new MultiColorRangeChooser(baseColors)
      val colors = chooser.getColors(baseColors.length)
      colors.toList must be === baseColors.toList
    }
  }

  describe("ColorBreaks") {
    it("should map breaks to colors") {
      val a = Array((2, 0xFF0000), (4, 0x00FF00), (6, 0x0000FF))
      val cb = ColorBreaks(a)
      cb.breaks must be === a
    }
  }
}
