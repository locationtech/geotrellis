/**************************************************************************
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
 **************************************************************************/

package geotrellis.raster.op.global

import geotrellis._

/**
 * Converts a raster to an integer array.
 */
case class AsArray(r:Op[Raster]) extends Op1(r)({ 
  r => 
    Result(r.toArray)
})

/**
 * Converts a raster to a double array.
 */
case class AsArrayDouble(r:Op[Raster]) extends Op1(r)({
  r =>  
    Result(r.toArrayDouble)
})
