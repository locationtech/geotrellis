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

package geotrellis.spark.io.hadoop

import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.points.ProjectedExtent3D
import geotrellis.spark.points.json._
import io.pdal._
import spray.json._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.InputFormat
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

/**
  * Allows for reading point data files using PDAL as RDD[(ProjectedPackedPointsBounds, PackedPoints)]s through Hadoop FileSystem API.
  */
object HadoopPackedPointsRDD {

  /**
    * This case class contains the various parameters one can set when reading RDDs from Hadoop using Spark.
    */

  case class Options(
    filesExtensions: Seq[String] = Seq(".las", "laz")
  )

  object Options {
    def DEFAULT = Options()
  }

  /**
    * Create Configuration for [[PackedPointsInputFormat]] based on parameters and options.
    *
    * @param path     Hdfs point data files path.
    * @param options  An instance of [[Options]] that contains any user defined or default settings.
    */
  private def configuration(path: Path, options: HadoopPackedPointsRDD.Options)(implicit sc: SparkContext): Configuration = {
    val conf = sc.hadoopConfiguration.withInputDirectory(path, options.filesExtensions)
    conf
  }


  /**
    * Creates a RDD[(ProjectedPackedPointsBounds, PackedPoints)] whose K depends on the type of the point data file that is going to be read in.
    *
    * @param path     Hdfs point data files path.
    * @param options  An instance of [[Options]] that contains any user defined or default settings.
    */
  def apply(path: Path, options: Options = Options.DEFAULT)(implicit sc: SparkContext): RDD[(ProjectedExtent3D, PackedPoints)] = {
    sc.newAPIHadoopRDD(
      configuration(path, options),
      classOf[PackedPointsInputFormat],
      classOf[Path],
      classOf[Iterator[PackedPoints]]
    ).mapPartitions(
      _.flatMap { case (_, packedPointsIter) => packedPointsIter.map { packedPoints =>
        packedPoints.metadata.parseJson.convertTo[ProjectedExtent3D] -> packedPoints
      } },
      preservesPartitioning = true
    )
  }
}
