/**************************************************************************
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
 **************************************************************************/

package geotrellis.spark.old

import geotrellis.spark._
import geotrellis.spark.formats._
import geotrellis.spark.cmd.CommandArguments
import geotrellis.spark.rdd.RasterRDD
import geotrellis.spark.utils.HdfsUtils
import geotrellis.spark.utils.SparkUtils
import org.apache.spark.Logging
import org.apache.spark.SparkContext._
import com.quantifind.sumac.ArgMain
import org.apache.hadoop.fs.Path

object InputOutputDriver extends ArgMain[CommandArguments] with Logging {
  def main(args: CommandArguments) {
    val sparkMaster = args.sparkMaster // "spark://host:7077"
    val inputRasterPath = args.input // /geotrellis/images/argtest
    val outputRasterPath = args.output // /geotrellis/images/argtestout
    val sc = SparkUtils.createSparkContext(sparkMaster, "InputOutputRaster")
    val awtestRdd = RasterRDD(inputRasterPath, sc)

    def printTileWithPartition(idx: Int, itr: Iterator[Tile]) = {
      itr.foreach { case Tile(id, _) => logInfo(s"Tile ${id} partition $idx") }
      itr
    }

    logInfo(s"dfs.block.size=${HdfsUtils.blockSize(sc.hadoopConfiguration)}")
    logInfo("sc defaultMinSplits/defaultParallelism = %d/%d".format(sc.defaultMinSplits, sc.defaultParallelism))
    logInfo("# of partitions = " + awtestRdd.partitions.length)
    logInfo("# of rows = " + awtestRdd.mapPartitionsWithIndex(printTileWithPartition, true).count)

    val firstTileId = awtestRdd.first.id
    logInfo(s"Test lookup of first tile $firstTileId")
    //Do we need this? Should lookup be on RasterRDD?
    //logInfo("# of matching rows = " + awtestRdd.lookup(firstTileId).length)

    awtestRdd.save(new Path(outputRasterPath))

    sc.stop
  }
}  
