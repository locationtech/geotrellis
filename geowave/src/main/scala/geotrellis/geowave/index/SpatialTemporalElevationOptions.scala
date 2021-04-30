/*
 * Copyright 2020 Azavea
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

package geotrellis.geowave.index

import com.beust.jcommander.Parameter
import org.locationtech.geowave.core.geotime.index.SpatialTemporalDimensionalityTypeProvider.Bias
import org.locationtech.geowave.core.geotime.index.dimension.TemporalBinningStrategy
import org.locationtech.geowave.core.geotime.index.dimension.TemporalBinningStrategy.Unit
import org.locationtech.geowave.core.geotime.index.{CommonSpatialOptions, SpatialTemporalDimensionalityTypeProvider}

class SpatialTemporalElevationOptions extends CommonSpatialOptions {
  protected var DEFAULT_TEMPORAL_PERIODICITY = Unit.YEAR
  protected var DEFAULT_MAX_ELEVATION = 32000

  @Parameter(
    names = Array("--maxElevation"),
    required = false,
    description = "The periodicity of the elevation dimension. Because time is continuous, it is binned at this interval.",
    converter = classOf[SpatialTemporalElevationIndexTypeProvider.IntConverter]
  )
  protected var maxElevation: Int = DEFAULT_MAX_ELEVATION

  @Parameter(
    names = Array("--periodTemporal"),
    required = false,
    description = "The periodicity of the temporal dimension.  Because time is continuous, it is binned at this interval.",
    converter = classOf[SpatialTemporalDimensionalityTypeProvider.UnitConverter]
  )
  protected var temporalPeriodicity: TemporalBinningStrategy.Unit = DEFAULT_TEMPORAL_PERIODICITY

  @Parameter(
    names = Array("--bias"),
    required = false,
    description = "The bias of the spatial-temporal index. There can be more precision given to time or space if necessary.",
    converter = classOf[SpatialTemporalDimensionalityTypeProvider.BiasConverter]
  )
  protected var bias = Bias.BALANCED

  @Parameter(
    names = Array("--maxDuplicates"),
    required = false,
    description = "The max number of duplicates per dimension range. The default is 2 per range (for example lines and polygon timestamp data would be up to 4 because its 2 dimensions, and line/poly time range data would be 8)."
  )
  protected var maxDuplicates: Long = -1

  def getBias: SpatialTemporalDimensionalityTypeProvider.Bias = bias

  def setBias(bias: SpatialTemporalDimensionalityTypeProvider.Bias) = this.bias = bias

  def getMaxDuplicates: Long = maxDuplicates

  def setMaxDuplicates(maxDuplicates: Long) = this.maxDuplicates = maxDuplicates

  def getTemporalPerioidicty: TemporalBinningStrategy.Unit = temporalPeriodicity

  def setTemporalPerioidicty(temporalPeriodicity: TemporalBinningStrategy.Unit) = this.temporalPeriodicity = temporalPeriodicity

  def getMaxElevation: Int = maxElevation

  def setMaxElevation(x: Int) = this.maxElevation = x

  override def getCrs: String = crs
}
