/*
 * Copyright 2019 Azavea
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

package geotrellis.layer

import geotrellis.raster.{RasterMetadata, SourceName}

import java.time.ZonedDateTime

trait KeyExtractor[K] extends Serializable {
  type M
  def getMetadata(rs: RasterMetadata): M
  def getKey(metadata: M, spatialKey: SpatialKey): K
}

object KeyExtractor {
  /** Aux pattern is used to help compiler proving that the M = RealWorld metadata */
  type Aux[K, M0] = KeyExtractor[K] { type M = M0 }

  val spatialKeyExtractor: KeyExtractor.Aux[SpatialKey, Unit] = new KeyExtractor[SpatialKey] {
    type M = Unit
    def getMetadata(rs: RasterMetadata): Unit = ()
    def getKey(metadata: Unit, spatialKey: SpatialKey): SpatialKey = spatialKey
  }
}

trait TemporalKeyExtractor extends KeyExtractor[SpaceTimeKey] {
  type M = ZonedDateTime
  def getMetadata(rs: RasterMetadata): M
  def getKey(metadata: ZonedDateTime, spatialKey: SpatialKey): SpaceTimeKey = SpaceTimeKey(spatialKey, TemporalKey(metadata.toInstant.toEpochMilli))
}

object TemporalKeyExtractor {
  def fromPath(parseTime: SourceName => ZonedDateTime): KeyExtractor.Aux[SpaceTimeKey, ZonedDateTime] = new TemporalKeyExtractor {
    def getMetadata(rs: RasterMetadata): ZonedDateTime = parseTime(rs.name)
  }
}
