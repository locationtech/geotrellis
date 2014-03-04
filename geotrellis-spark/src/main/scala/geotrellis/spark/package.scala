/*******************************************************************************
 * Copyright (c) 2014 DigitalGlobe.
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
 ******************************************************************************/

package geotrellis
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.rdd.SaveRasterFunctions

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

package object spark {
  
  type TileId = Long
  
  type RasterWritableRDD = RDD[(TileIdWritable, ArgWritable)]
  //type ImageRDD = RDD[(TileId, RasterData)]

  implicit class SavableImage(val image: RasterWritableRDD) {
    def save(path: String) = SaveRasterFunctions.save(image, path)
  }
}