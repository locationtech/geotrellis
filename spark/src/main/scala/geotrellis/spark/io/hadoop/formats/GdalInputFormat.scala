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

package geotrellis.spark.io.hadoop.formats

import geotrellis.spark.utils.HdfsUtils
import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.spark.utils.HdfsUtils.LocalPath
import geotrellis.vector.Extent
import geotrellis.vector.Extent
import geotrellis.proj4._
import geotrellis.gdal.{RasterBand, RasterDataSet, Gdal}

import org.apache.hadoop.fs.Path
import org.apache.hadoop.fs.FSDataInputStream

import org.apache.hadoop.mapreduce.InputSplit
import org.apache.hadoop.mapreduce.JobContext
import org.apache.hadoop.mapreduce.RecordReader
import org.apache.hadoop.mapreduce.TaskAttemptContext
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.lib.input.FileSplit
import org.apache.hadoop.conf.Configuration

/**
 * Uses GDAL to attempt to read a raster file.
 *
 * GDAL only supports reading files from local filesystem.
 * In order for this InputFormat to work it must copy the entire input file to hadoop.tmp.dir,
 * if it is not already on a local file system, and invoke GDAL JNI bindings.
 *
 * If there the .so/.dylib files are not found in the class path, this will crash gloriously.
 *
 * Also note that all the blocks need to be shuffled to a single machine for each file.
 * If the file being ingested is much larger than HDFS block size this will be very inefficient.
 */
class GdalInputFormat extends FileInputFormat[(Extent, CRS, Map[String, String]), Tile] {
  override def isSplitable(context: JobContext, fileName: Path) = false

  override def createRecordReader(split: InputSplit, context: TaskAttemptContext) =
    new GdalRecordReader
}

class GdalRecordReader extends RecordReader[(Extent, CRS, Map[String, String]), Tile] {
  private var conf: Configuration = _
  private var file: LocalPath = _
  private var rasterDataSet: RasterDataSet = _
  private var rasterExtent: Extent = _
  private var crs: CRS = _
  private var maxBand: Int = _
  private var bandIndex: Int = 0
  private var band: RasterBand = null
  private var meta: Map[String, String] = _

  def initialize(split: InputSplit, context: TaskAttemptContext) = {
    val path = split.asInstanceOf[FileSplit].getPath

    conf            = context.getConfiguration
    file            = HdfsUtils.localCopy(conf, path)
    rasterDataSet   = Gdal.open(file.path.toUri.getPath)
    rasterExtent    = rasterDataSet.rasterExtent.extent
    maxBand         = rasterDataSet.maxBandIndex
    rasterDataSet.projection match {
      case None =>
        crs = LatLng //This seems to be default for NetCDF
      case Some(s) =>
        //If you got this error, you should know how to handle it now.
        sys.error(s"Don't know how to handle GDAL projection: $s")
    }
  }

  def close() = file match {
    case LocalPath.Temporary(path) => path.getFileSystem(conf).delete(path)
    case LocalPath.Original(_) => //leave it well alone
  }

  def getProgress = bandIndex / maxBand
  def nextKeyValue = {
    val hasNext = bandIndex < maxBand
    if (hasNext) {
      bandIndex += 1
      band = rasterDataSet.band(bandIndex)
      meta = band
        .metadata
        .map(_.split("="))
        .map(l => l(0) -> l(1))
        .toMap
    }
    hasNext
  }
  def getCurrentKey = (rasterExtent, crs, meta)
  def getCurrentValue = band.toTile()

}
