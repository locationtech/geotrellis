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

package geotrellis.raster.op.transform

import geotrellis._
import geotrellis.feature.Extent
import geotrellis.raster._

/**
 * Create a new raster from the data in a sub-extent of an existing raster.
 * 
 * @param      r         Raster to crop
 * @param      extent    Subextent of r that will be the resulting cropped raster's extent.
 */
case class Crop(r: Op[Raster], extent: Extent) 
     extends Op2(r, extent)({
       (r,extent) => Result(CroppedRaster(r, extent))
})
