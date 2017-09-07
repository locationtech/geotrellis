package geotrellis.raster.render.ascii

import geotrellis.raster.render.ascii.AsciiArtEncoder.Settings
import geotrellis.raster.testkit.{RasterMatchers, TileBuilders}
import org.scalatest.{FunSuite, Inspectors, Matchers}

/**
 *
 * @since 9/6/17
 */
class RenderAsciiTests extends FunSuite with Matchers with TileBuilders with Inspectors {

  test("generate ASCII art from tile") {
    val palettes = Seq(
      AsciiArtEncoder.Palette.WIDE,
      AsciiArtEncoder.Palette.NARROW,
      AsciiArtEncoder.Palette.HATCHING,
      AsciiArtEncoder.Palette.FILLED,
      AsciiArtEncoder.Palette.STIPLED,
      AsciiArtEncoder.Palette.BINARY
    )
    forEvery(palettes) { palette ⇒
      val sample = createConsecutiveTile(palette.length)
      println("-" * 80)
      println(sample.renderAscii(Settings(palette)))
      println("-" * 80)
    }
  }
}
