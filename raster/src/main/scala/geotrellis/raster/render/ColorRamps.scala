/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.render

object ColorRamps {

  /** Blue to orange color ramp. */
  final def BlueToOrange =
    ColorRamp(
      0x2586ABFF, 0x4EA3C8FF, 0x7FB8D4FF, 0xADD8EAFF,
      0xC8E1E7FF, 0xEDECEAFF, 0xF0E7BBFF, 0xF5CF7DFF,
      0xF9B737FF, 0xE68F2DFF, 0xD76B27FF
    )

  /** Light yellow to orange color ramp. */
  final def LightYellowToOrange =
    ColorRamp(
      0x118C8CFF, 0x429D91FF, 0x61AF96FF, 0x75C59BFF,
      0xA2CF9FFF, 0xC5DAA3FF, 0xE6E5A7FF, 0xE3D28FFF,
      0xE0C078FF, 0xDDAD62FF, 0xD29953FF, 0xCA8746FF,
      0xC2773BFF
    )

  /** Blue to red color ramp. */
  final def BlueToRed =
    ColorRamp(
      0x2791C3FF, 0x5DA1CAFF, 0x83B2D1FF, 0xA8C5D8FF,
      0xCCDBE0FF, 0xE9D3C1FF, 0xDCAD92FF, 0xD08B6CFF,
      0xC66E4BFF, 0xBD4E2EFF
    )

  /** Green to red-orange color ramp. */
  final def GreenToRedOrange =
    ColorRamp(
      0x569543FF, 0x9EBD4DFF, 0xBBCA7AFF, 0xD9E2B2FF,
      0xE4E7C4FF, 0xE6D6BEFF, 0xE3C193FF, 0xDFAC6CFF,
      0xDB9842FF, 0xB96230FF
    )

  /** Light to dark (sunset) color ramp. */
  final def LightToDarkSunset =
    ColorRamp(
      0xFFFFFFFF, 0xFBEDD1FF, 0xF7E0A9FF, 0xEFD299FF,
      0xE8C58BFF, 0xE0B97EFF, 0xF2924DFF, 0xC97877FF,
      0x946196FF, 0x2AB7D6FF, 0x474040FF
    )

  /** Light to dark (green) color ramp. */
  final def LightToDarkGreen =
    ColorRamp(
      0xE8EDDBFF, 0xDCE8D4FF, 0xBEDBADFF, 0xA0CF88FF,
      0x81C561FF, 0x4BAF48FF, 0x1CA049FF, 0x3A6D35FF
    )

  /** Yellow to red color ramp, for use in heatmaps. */
  final def HeatmapYellowToRed =
    ColorRamp(
      0xF7DA22FF, 0xECBE1DFF, 0xE77124FF, 0xD54927FF,
      0xCF3A27FF, 0xA33936FF, 0x7F182AFF, 0x68101AFF
    )

  /** Blue to yellow to red spectrum color ramp, for use in heatmaps. */
  final def HeatmapBlueToYellowToRedSpectrum =
    ColorRamp(
      0x2A2E7FFF, 0x3D5AA9FF, 0x4698D3FF, 0x39C6F0FF,
      0x76C9B3FF, 0xA8D050FF, 0xF6EB14FF, 0xFCB017FF,
      0xF16022FF, 0xEE2C24FF, 0x7D1416FF
    )

  /** Dark red to yellow white color ramp, for use in heatmaps. */
  final def HeatmapDarkRedToYellowWhite =
    ColorRamp(
      0x68101AFF, 0x7F182AFF, 0xA33936FF, 0xCF3A27FF,
      0xD54927FF, 0xE77124FF, 0xECBE1DFF, 0xF7DA22FF,
      0xF6EDB1FF, 0xFFFFFFFF
    )

  /** Light purple to dark purple to white, for use in heatmaps. */
  final def HeatmapLightPurpleToDarkPurpleToWhite =
    ColorRamp(
      0xA52278FF, 0x993086FF, 0x8C3C97FF, 0x6D328AFF,
      0x4E2B81FF, 0x3B264BFF, 0x180B11FF, 0xFFFFFFFF
    )

  /** Bold Land Use color ramp, for use in land use classification. */
  final def ClassificationBoldLandUse =
    ColorRamp(
      0xB29CC3FF, 0x4F8EBBFF, 0x8F9238FF, 0xC18437FF,
      0xB5D6B1FF, 0xD378A6FF, 0xD4563CFF, 0xF9BE47FF
    )

  /** Muted terrain color ramp, for use in classification. */
  final def ClassificationMutedTerrain =
    ColorRamp(
      0xCEE1E8FF, 0x7CBCB5FF, 0x82B36DFF, 0x94C279FF,
      0xD1DE8DFF, 0xEDECC3FF, 0xCCAFB4FF, 0xC99884FF
    )
}
