/***
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
 ***/

package geotrellis.services

import geotrellis.render._

/** Provides a string keyed map to predefined color ramps
  * to be used for coloring rendered rasters.
  */
object ColorRampMap {
  val rampMap =
    Map(
    "blue-to-orange" -> ColorRamps.BlueToOrange,
    "green-to-orange" -> ColorRamps.LightYellowToOrange,
    "blue-to-red" -> ColorRamps.BlueToRed,
    "green-to-red-orange" -> ColorRamps.GreenToRedOrange,
    "light-to-dark-sunset" -> ColorRamps.LightToDarkSunset,
    "light-to-dark-green" -> ColorRamps.LightToDarkGreen,
    "yellow-to-red-heatmap" -> ColorRamps.HeatmapYellowToRed,
    "blue-to-yellow-to-red-heatmap" -> ColorRamps.HeatmapBlueToYellowToRedSpectrum,
    "dark-red-to-yellow-heatmap" -> ColorRamps.HeatmapDarkRedToYellowWhite,
    "purple-to-dark-purple-to-white-heatmap" -> ColorRamps.HeatmapLightPurpleToDarkPurpleToWhite,
    "bold-land-use-qualitative" -> ColorRamps.ClassificationBoldLandUse,
    "muted-terrain-qualitative" -> ColorRamps.ClassificationMutedTerrain
  )

  def get(s:String) = rampMap.get(s)
  def getOrElse(s:String,cr:ColorRamp) = rampMap.getOrElse(s,cr)

  def getJson = {
    val c = for(key <- rampMap.keys) yield {
      s"""{ "key": "$key", "image": "img/ramps/${key}.png" }"""
    }
    val arr = "[" + c.mkString(",") + "]"
    s"""{ "colors": $arr }"""
  }
}
