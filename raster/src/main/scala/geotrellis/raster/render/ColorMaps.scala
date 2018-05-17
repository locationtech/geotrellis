/*
 * Copyright 2017 Azavea
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

package geotrellis.raster.render

/**
 * Library of color maps.
 *
 * @author sfitch 
 * @since 3/15/17
 */
object ColorMaps extends ClassificationColorMaps

trait ClassificationColorMaps {

  /**
   * IGBP Land Cover Classes
   * Source: https://lpdaac.usgs.gov/about/news_archive/modisterra_land_cover_types_yearly_l3_global_005deg_cmg_mod12c1
   */
  object IGBP extends IntColorMap(Map(
    // @formatter:off
    255 -> 0x000000FF,    // Fill Value
    0 -> 0x000080FF,      // Water
    1 -> 0x008000FF,      // Evergreen Needleleaf Forest
    2 -> 0x00FF00FF,      // Evergreen Broadleaf Forest
    3 -> 0x99CC00FF,      // Deciduous Needleleaf Forest
    4 -> 0x99FF99FF,      // Deciduous Broadleaf Forest
    5 -> 0x339966FF,      // Mixed Forest
    6 -> 0x993366FF,      // Closed Shrubland
    7 -> 0xFFCC99FF,      // Open Shrubland
    8 -> 0xCCFFCCFF,      // Woody Savannas
    9 -> 0xFFCC00FF,      // Savannas
    10 -> 0xFF9900FF,     // Grasslands
    11 -> 0x006699FF,     // Permanent Wetlands
    12 -> 0xFFFF00FF,     // Croplands
    13 -> 0xFF0000FF,     // Urban and Built-Up
    14 -> 0x999966FF,     // Cropland/Natural Vegetation Mosaic
    15 -> 0xFFFFFFFF,     // Snow and Ice
    16 -> 0x808080FF,     // Barren or Sparsely Vegetated
    254 -> 0x000000FF     // Unclassified
    // @formatter:on
  ),
    defaultOptions
  )

  /**
   * NLCD 92 Land Cover Classes
   * Source: https://landcover.usgs.gov/classes.php
   */
  object NLCD extends IntColorMap(Map(
    // @formatter:off
    11 -> 0x526095FF,     // Open Water
    12 -> 0xFFFFFFFF,     // Perennial Ice/Snow
    21 -> 0xD28170FF,     // Low Intensity Residential
    22 -> 0xEE0006FF,     // High Intensity Residential
    23 -> 0x990009FF,     // Commercial/Industrial/Transportation
    31 -> 0xBFB8B1FF,     // Bare Rock/Sand/Clay
    32 -> 0x969798FF,     // Quarries/Strip Mines/Gravel Pits
    33 -> 0x382959FF,     // Transitional
    41 -> 0x579D57FF,     // Deciduous Forest
    42 -> 0x2A6B3DFF,     // Evergreen Forest
    43 -> 0xA6BF7BFF,     // Mixed Forest
    51 -> 0xBAA65CFF,     // Shrubland
    61 -> 0x45511FFF,     // Orchards/Vineyards/Other
    71 -> 0xD0CFAAFF,     // Grasslands/Herbaceous
    81 -> 0xCCC82FFF,     // Pasture/Hay
    82 -> 0x9D5D1DFF,     // Row Crops
    83 -> 0xCD9747FF,     // Small Grains
    84 -> 0xA7AB9FFF,     // Fallow
    85 -> 0xE68A2AFF,     // Urban/Recreational Grasses
    91 -> 0xB6D8F5FF,     // Woody Wetlands
    92 -> 0xB6D8F5FF      // Emergent Herbaceous Wetlands
    // @formatter:on
  ),
    defaultOptions
  )

  private val defaultOptions = ColorMap.Options(
    classBoundaryType = Exact,
    fallbackColor = 0x000000FF
  )
}

