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

package geotrellis.spark.ingest

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.vector.Extent
import geotrellis.spark.cmd.NoDataHandler

import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.InputSplit
import org.apache.hadoop.mapreduce.JobContext
import org.apache.hadoop.mapreduce.RecordReader
import org.apache.hadoop.mapreduce.TaskAttemptContext
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.lib.input.FileSplit

import java.awt.image._
import java.nio.ByteBuffer

class GeotiffInputFormat extends FileInputFormat[Extent, Tile] {
  override def isSplitable(context: JobContext, fileName: Path) = false

  override def createRecordReader(split: InputSplit, context: TaskAttemptContext): RecordReader[Extent, Tile] =
    new GeotiffRecordReader

}

case class GeotiffData(cellType: CellType, userNoData: Double)

class GeotiffRecordReader extends RecordReader[Extent, Tile] {
  private var tup: (Extent, Tile) = null
  private var hasNext: Boolean = true

  def initialize(split: InputSplit, context: TaskAttemptContext) = {
    val path = split.asInstanceOf[FileSplit].getPath()

//    tup =
      GeoTiff.withReader[(Extent, Tile)](path, context.getConfiguration()) { reader =>
        // val image = GeoTiff.getGridCoverage2D(reader)
        // val imageMeta = GeoTiff.getMetadata(reader)
        // val (cols, rows) = imageMeta.pixels
        // val rawDataBuff = image.getRenderedImage().getData().getDataBuffer()
        // val cellType = CellType.fromAwtType(imageMeta.cellType)
        
        // val tile =  cellType match {
        //   case TypeDouble => ArrayTile(rawDataBuff.asInstanceOf[DataBufferDouble].getData(), cols, rows)
        //   case TypeFloat  => ArrayTile(rawDataBuff.asInstanceOf[DataBufferFloat].getData(), cols, rows)
        //   case TypeInt    => ArrayTile(rawDataBuff.asInstanceOf[DataBufferInt].getData(), cols, rows)
        //   case TypeShort  => ArrayTile(rawDataBuff.asInstanceOf[DataBufferShort].getData(), cols, rows)
        //   case TypeByte   => ArrayTile(rawDataBuff.asInstanceOf[DataBufferByte].getData(), cols, rows)
        //   case TypeBit   => sys.error("TypeBit is not yet supported")
        // }
        // NoDataHandler.removeUserNoData(tile, imageMeta.nodata)
       
        (Extent(10.0, 20.0, 30.0, 40.0), ArrayTile.empty(TypeByte, 512, 512))
//        (imageMeta.extent, tile)
      }


    val fs = path.getFileSystem(context.getConfiguration())

    val len = 
      fs.getFileStatus(path).getLen match {
        case l if l > Int.MaxValue.toLong =>
          sys.error(s"Cannot read path $path because it's too big..." + 
                     "you must tile your rasters to smaller tiles!")
        case l => l.toInt
      }

    val bytes = Array.ofDim[Byte](len)

    val stream = fs.open(path)

    try {
      stream.readFully(0, bytes)
    } finally {
      stream.close()
    }

    tup =
      GeoTiffReader(bytes).read().imageDirectories(0).toRaster.swap



//    tup = (Extent(10.0, 20.0, 30.0, 40.0), ArrayTile.empty(TypeByte, 512, 512))

  }

  def close = {}
  def getCurrentKey = tup._1
  def getCurrentValue = { hasNext = false ; tup._2 }
  def getProgress = 1
  def nextKeyValue = hasNext
}
