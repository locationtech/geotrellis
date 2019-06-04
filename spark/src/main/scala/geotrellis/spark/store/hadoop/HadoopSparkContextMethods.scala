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

package geotrellis.spark.store.hadoop

import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.tiling.TemporalProjectedExtent
import geotrellis.spark.store.hadoop.formats._
import geotrellis.vector._

import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.conf.Configuration

trait HadoopSparkContextMethods {
  implicit val sc: SparkContext

  def hadoopGeoTiffRDD(path: String): RDD[(ProjectedExtent, Tile)] =
    hadoopGeoTiffRDD(new Path(path), HadoopGeoTiffRDD.Options.DEFAULT.tiffExtensions)

  def hadoopGeoTiffRDD(path: String, tiffExtension: String): RDD[(ProjectedExtent, Tile)] =
    hadoopGeoTiffRDD(new Path(path), Seq(tiffExtension))

  def hadoopGeoTiffRDD(path: String, tiffExtensions: Seq[String]): RDD[(ProjectedExtent, Tile)] =
    hadoopGeoTiffRDD(new Path(path), tiffExtensions)

  def hadoopGeoTiffRDD(path: Path): RDD[(ProjectedExtent, Tile)] =
    hadoopGeoTiffRDD(path, HadoopGeoTiffRDD.Options.DEFAULT.tiffExtensions)

  def hadoopGeoTiffRDD(path: Path, tiffExtension: String): RDD[(ProjectedExtent, Tile)] =
    hadoopGeoTiffRDD(path, Seq(tiffExtension))

  def hadoopGeoTiffRDD(path: Path, tiffExtensions: Seq[String], crs: Option[CRS] = None): RDD[(ProjectedExtent, Tile)] =
    HadoopGeoTiffRDD.spatial(path, HadoopGeoTiffRDD.Options(
      tiffExtensions = tiffExtensions,
      crs = crs
    ))

  def hadoopTemporalGeoTiffRDD(path: String): RDD[(TemporalProjectedExtent, Tile)] =
    hadoopTemporalGeoTiffRDD(new Path(path), HadoopGeoTiffRDD.Options.DEFAULT.tiffExtensions)

  def hadoopTemporalGeoTiffRDD(path: String, tiffExtension: String): RDD[(TemporalProjectedExtent, Tile)] =
    hadoopTemporalGeoTiffRDD(new Path(path), Seq(tiffExtension))

  def hadoopTemporalGeoTiffRDD(path: String, tiffExtensions: Seq[String] ): RDD[(TemporalProjectedExtent, Tile)] =
    hadoopTemporalGeoTiffRDD(new Path(path), tiffExtensions)

  def hadoopTemporalGeoTiffRDD(path: Path): RDD[(TemporalProjectedExtent, Tile)] =
    hadoopTemporalGeoTiffRDD(path, HadoopGeoTiffRDD.Options.DEFAULT.tiffExtensions)

  def hadoopTemporalGeoTiffRDD(path: Path, tiffExtension: String): RDD[(TemporalProjectedExtent, Tile)] =
    hadoopTemporalGeoTiffRDD(path, Seq(tiffExtension))

  def hadoopTemporalGeoTiffRDD(
    path: Path,
    tiffExtensions: Seq[String] = HadoopGeoTiffRDD.Options.DEFAULT.tiffExtensions,
    timeTag: String = HadoopGeoTiffRDD.GEOTIFF_TIME_TAG_DEFAULT,
    timeFormat: String = HadoopGeoTiffRDD.GEOTIFF_TIME_FORMAT_DEFAULT,
    crs: Option[CRS] = None
  ): RDD[(TemporalProjectedExtent, Tile)] =
    HadoopGeoTiffRDD.temporal(path, HadoopGeoTiffRDD.Options(
      tiffExtensions = tiffExtensions,
      timeTag = timeTag,
      timeFormat = timeFormat,
      crs = crs
    ))

  def hadoopMultibandGeoTiffRDD(path: String): RDD[(ProjectedExtent, MultibandTile)] =
    hadoopMultibandGeoTiffRDD(new Path(path), HadoopGeoTiffRDD.Options.DEFAULT.tiffExtensions)

  def hadoopMultibandGeoTiffRDD(path: String, tiffExtension: String): RDD[(ProjectedExtent, MultibandTile)] =
    hadoopMultibandGeoTiffRDD(new Path(path), Seq(tiffExtension))

  def hadoopMultibandGeoTiffRDD(path: String, tiffExtensions: Seq[String]): RDD[(ProjectedExtent, MultibandTile)] =
    hadoopMultibandGeoTiffRDD(new Path(path), tiffExtensions)

  def hadoopMultibandGeoTiffRDD(
    path: Path,
    tiffExtensions: Seq[String] = HadoopGeoTiffRDD.Options.DEFAULT.tiffExtensions,
    crs: Option[CRS] = None
  ): RDD[(ProjectedExtent, MultibandTile)] =
    HadoopGeoTiffRDD.spatialMultiband(path, HadoopGeoTiffRDD.Options(
      tiffExtensions = tiffExtensions,
      crs = crs
    ))

  def hadoopTemporalMultibandGeoTiffRDD(path: String): RDD[(TemporalProjectedExtent, MultibandTile)] =
    hadoopTemporalMultibandGeoTiffRDD(new Path(path), HadoopGeoTiffRDD.Options.DEFAULT.tiffExtensions)

  def hadoopTemporalMultibandGeoTiffRDD(path: String, tiffExtension: String): RDD[(TemporalProjectedExtent, MultibandTile)] =
    hadoopTemporalMultibandGeoTiffRDD(new Path(path), Seq(tiffExtension))

  def hadoopTemporalMultibandGeoTiffRDD(path: String, tiffExtensions: Seq[String]): RDD[(TemporalProjectedExtent, MultibandTile)] =
    hadoopTemporalMultibandGeoTiffRDD(new Path(path), tiffExtensions)

  def hadoopTemporalMultibandGeoTiffRDD(
    path: Path,
    tiffExtensions: Seq[String] = HadoopGeoTiffRDD.Options.DEFAULT.tiffExtensions,
    timeTag: String = HadoopGeoTiffRDD.GEOTIFF_TIME_TAG_DEFAULT,
    timeFormat: String = HadoopGeoTiffRDD.GEOTIFF_TIME_FORMAT_DEFAULT,
    crs: Option[CRS] = None
  ): RDD[(TemporalProjectedExtent, MultibandTile)] =
    HadoopGeoTiffRDD.temporalMultiband(path, HadoopGeoTiffRDD.Options(
      tiffExtensions = tiffExtensions,
      timeTag = timeTag,
      timeFormat = timeFormat,
      crs = crs
    ))

  def newJob: Job =
    Job.getInstance(sc.hadoopConfiguration)

  def newJob(name: String) =
    Job.getInstance(sc.hadoopConfiguration, name)
}
