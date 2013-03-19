package geotrellis.admin

import geotrellis.data.ColorRamps

object Colors {
  val rampMap = Map(
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
    "muted-terrain-qualitative" -> ColorRamps.ClassificationMutedTerrain)
}
