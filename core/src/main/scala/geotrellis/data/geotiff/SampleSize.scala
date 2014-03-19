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

package geotrellis.data.geotiff

//TODO: should probably not be named Int/Long since Float/Double use those too.

/**
 * SampleSize is used by geotiff.Settings to indicate how wide each sample
 * (i.e. each raster cell) will be. Currently we only support 8-, 16-, and
 * 32-bit samples, although other widths (including 1- and 64-bit samples)
 * could be supported in the future.
 */
sealed abstract class SampleSize(val bits:Int)
case object ByteSample extends SampleSize(8)
case object ShortSample extends SampleSize(16)
case object IntSample extends SampleSize(32)
case object LongSample extends SampleSize(64)
