/*
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
 */

package geotrellis

import geotrellis.spark.formats._
import geotrellis.spark.metadata.Context
import geotrellis.spark.rdd.RasterRDD
import geotrellis.spark.rdd.SaveRasterFunctions

import org.apache.hadoop.fs.Path
import org.apache.spark.rdd.RDD

package object spark {
  implicit class SavableRasterWritable(val raster: RDD[WritableTile]) {
    def save(path: Path): Unit = SaveRasterFunctions.save(raster, path)
    def save(path: String): Unit = save(new Path(path))
  }

  implicit class MakeRasterRDD(val prev: RDD[Tile]) {
    def withContext(ctx: Context) = new RasterRDD(prev, ctx)
  }

  implicit class SavableRasterRDD(val rdd: RasterRDD) {
    def save(path: Path) = SaveRasterFunctions.save(rdd, path)
    def save(path: String): Unit = save(new Path(path))
  }
}
