/*
 * Copyright 2016 Azavea
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

package geotrellis.spark.etl.s3

import geotrellis.spark.etl.TypedModule

object S3Module extends TypedModule {
  register(new GeoTiffS3Input)
  register(new TemporalGeoTiffS3Input)
  register(new SpatialS3Output)
  register(new SpaceTimeS3Output)
  register(new MultibandGeoTiffS3Input)
  register(new TemporalMultibandGeoTiffS3Input)
  register(new SpaceTimeMultibandS3Output)
  register(new SpatialMultibandS3Output)
}
