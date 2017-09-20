/*
 * Copyright 2017 Astraea, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package geotrellis.raster.render.ascii

import geotrellis.raster.io.geotiff.{GeoTiffTestUtils, SinglebandGeoTiff}
import geotrellis.raster.render.ascii.AsciiArtEncoder.Palette
import geotrellis.raster.testkit.TileBuilders
import org.scalatest.{FunSuite, Inspectors, Matchers}

/**
 * Simple tests to check basic ASCII art rendering.
 *
 * @since 9/6/17
 */
class RenderAsciiTests extends FunSuite with Matchers with TileBuilders with Inspectors {

  test("generate ASCII art from graident tile") {
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
      val render = sample.renderAscii(palette)
      forAll(palette.values) { c ⇒
        assert(render.contains(c))
      }
    }
  }

  test("generate ASCII art from image")(new GeoTiffTestUtils {
    val tiff = SinglebandGeoTiff(geoTiffPath("alaska-polar-3572.tif"))
    // format: off
    val expected =
     """∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘████  ∘∘∘∘∘∘∘∘∘∘∘∘
       |∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘█████  ████    ∘∘∘∘∘∘∘∘∘∘∘
       |∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘████████████▜   █▜     ∘∘∘∘∘∘∘∘∘∘∘
       |∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘█████▖▖▖▖█████▖▖  █           ∘∘∘∘∘∘∘∘∘∘
       |∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘██████████▜▖ ▖██▖█▖  ▖██             ∘∘∘∘∘∘∘∘∘∘
       |∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘████████████▜▖▖▖▖▖███▖▖▖▖▖ █▖ █            ∘∘∘∘∘∘∘∘∘
       |∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘██████████████████████▖ █████   ▜█ ██            ∘∘∘∘∘∘∘∘∘
       |∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘████████████████████▜▜███▖▜█████                      ∘∘∘∘∘∘∘∘
       |∘∘∘∘∘∘∘∘∘∘∘∘∘████████████████████████▜█▜███████                         ∘∘∘∘∘∘∘∘
       |∘∘∘∘∘∘∘∘████████████████████████████▜▖▖██████▖                           ∘∘∘∘∘∘∘
       |∘∘∘∘██████████████████████████████▖▖▖    ▜▜▖▖  ▖ ▖    ▖                  ∘∘∘∘∘∘∘
       |█████████████████████████████▜██▜▖▜▜▖                 ███                 ∘∘∘∘∘∘
       |████████████████████████████▜█▖█▜▚▜▜▖▖ ▖   ▖           ▖▖     ▖           ∘∘∘∘∘∘
       |██████████████████████████████▜▚▜▚▜█▖▖▖▖▖▖▖▖▖  ▖▖▖▖▖▖▖▖▖▖   ▖▖ ▖▖          ∘∘∘∘∘
       |∘█████████████████████████████▜▚▚▚▚▖▖▚▚▚▚▖▖█▖  ▖▖▖▖▚█▜█▜▜▖▖       ▖        ∘∘∘∘∘
       |∘████████████▜█████████████████▚▚▜▜▚▜▜▜▖▚▚▖▖▖▖▚▚▚█████████▜▖▖▖▖▖            ∘∘∘∘
       |∘█████████████▖████████████████▚▚▜▜▜▚▚▚▚▚▚▚▚▚▖▜████████████████▖▖  ▖  ▖     ∘∘∘∘
       |∘∘█████████████████████████████▚▚▚▜▚▚▚▚▜▜▜▜▜▚▚████████████████▖▖▖   ▖▖▖▖▖▖   ∘∘∘
       |∘∘███████████████████████████▜█▚▚▚▚▚▜▜▜▜▜▜▜▜▜▚█████████▜▜▜▜▖▖▖▖▖▖  ▖▖█▜▜█▖   ∘∘∘
       |∘∘▖▖████████████████████████▚▚▜▜▚▚▚▚▜▜▜▜▜▜▜▜▜▜▜█████▜▜▜▜▚▚▚▚▚▚▚▖▖▚▚▖▜██▖▖ ▖  ∘∘∘
       |∘∘∘█████████████████████████▜▚▚▚▚▚▚▚▜▜▜▜▜▜▜▜▜▚██▜▜██▜▜▜▜▜▜▜█▖▚▚▖▖██████▖▖▖▖   ∘∘
       |∘∘∘▖███████████████████████▚▚▚▚▜▚▚▚▚▜▜▜▜▜▜▜▜▚▚▚███▚▜▜▜▜▜█████▜▚▚▚██████▖▜█▜▜  ∘∘
       |∘∘∘█████████████████████████▚▜▚▚▚▚▚▚▜▜▜▜▜▚▚▚▚▚▚▜▜▜██▚▜██▚▜▜█▜▜▜████████████▖  ∘∘
       |∘∘∘∘ ▖██████████████████████▚▜▜▚▚▚▚▚▚▚▚▚▚▖▚▚▚▚▚▚▚▚▚▜█▚█▚▜▜▜▚▚▚█████████████▖   ∘
       |∘∘∘∘  ▖████████▖▖████████████▜▚▜▚▚▚▚▚▚▚▜▚▖▚▚▚▚▚▚▚▚▜▚▜▚▜▜▚▖▖▜▜▜▜▚▜▚███████████  ∘
       |∘∘∘∘∘  ▖███████▜▚▚▖▖██████████▜▜▚▜▚▚▜▚▚▚▜▜▚▚▚▜▜▜█▚▚▚▚▚▚██▜█▚▜▜▚▚▚▚▚▚▜████████▜ ∘
       |∘∘∘∘∘     ▖▖▖▖█▜▖▖▖▖▖█████████▚▜▜▜▜▚▚▚▚▚▚▚▜▜▜▚▜█▜████▚▜████▚▚▜▜▚▚█████████ ██▜ ▜
       |∘∘∘∘∘  ▜▖▖▜▜▖▖▖▖▖▖▖▖▖███████████▚▚▚▜▚▚▚▚▚▚▚▜▜▜▚█▚▚██▜██████▜▚▚▚█████████▖█▜█████
       |∘∘∘∘∘∘  ▖█▖▖▖▖▖▖▖ ▖▖▖▖▖▖█████████▜▜▚▚▚▚▖▚▚▚▚▚▚▚▚▜████████████████████▖▖▖██▖█████
       |∘∘∘∘∘∘     ▖      ▖▜████▜█████████▜▚▚███████▜█████▜██████████████████▖█▜▖████∘∘∘
       |∘∘∘∘∘∘∘          ▖███▜█▖  ▖████▚▖██▚▚▜███████████████▚████████▜▖█████████∘∘∘∘∘∘∘
       |∘∘∘∘∘∘∘                        ▖▖▖▚▖▜▜███████████████████████████████∘∘∘∘∘∘∘∘∘∘∘
       |∘∘∘∘∘∘∘∘                       ▖▖▖▖▜████████████████████████████∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘
       |∘∘∘∘∘∘∘∘                        ▖▖▖▖██████▜████████████████∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘
       |∘∘∘∘∘∘∘∘∘                        ▖▖▖▖▜██▖    ▖▖▜▜█████∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘
       |∘∘∘∘∘∘∘∘∘∘                         ▖▜▖ ▖        ∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘
       |∘∘∘∘∘∘∘∘∘∘                               ∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘
       |∘∘∘∘∘∘∘∘∘∘∘            ▖           ∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘
       |∘∘∘∘∘∘∘∘∘∘∘                ∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘
       |∘∘∘∘∘∘∘∘∘∘∘∘      ∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘∘"""
       .stripMargin
    // format: on
    val scaled = tiff.tile.resample(80, 40)
    val palette = Palette.STIPLED
    val render = scaled.renderAscii(palette)

    println(render)

    forEvery(render.toCharArray.distinct.filter(_ != '\n'))(c ⇒
      assert(c == palette.nodata || palette.values.contains(c))
    )

    assert(render === expected)
  })
}
