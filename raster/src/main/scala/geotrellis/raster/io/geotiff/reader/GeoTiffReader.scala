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

package geotrellis.raster.io.geotiff.reader

import java.io.File

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.vector.Extent
import geotrellis.util.{ByteReader, Filesystem, FileRangeReader, StreamingByteReader}
import java.nio.ByteBuffer


class MalformedGeoTiffException(msg: String) extends RuntimeException(msg)

class GeoTiffReaderLimitationException(msg: String) extends RuntimeException(msg)

object GeoTiffReader {

  /* Read a single band GeoTIFF file.
   * If there is more than one band in the GeoTiff, read the first band only.
   */
  def readSingleband(path: String): SinglebandGeoTiff =
    readSingleband(path, false)

  /* Read in only the extent of a single band GeoTIFF file.
   * If there is more than one band in the GeoTiff, read the first band only.
   */
  def readSingleband(path: String, e: Extent): SinglebandGeoTiff =
    readSingleband(path, Some(e))

  /* Read in only the extent of a single band GeoTIFF file.
   * If there is more than one band in the GeoTiff, read the first band only.
   */
  def readSingleband(path: String, e: Option[Extent]): SinglebandGeoTiff =
    e match {
      case Some(x) => readSingleband(path, true).crop(x)
      case None => readSingleband(path)
    }

  /* Read a single band GeoTIFF file.
   * If there is more than one band in the GeoTiff, read the first band only.
   */
  def readSingleband(path: String, streaming: Boolean): SinglebandGeoTiff = {
    val ovrPath = s"${path}.ovr"
    val ovrPathExists = new File(ovrPath).isFile
    if (streaming)
      readSingleband(
        StreamingByteReader(FileRangeReader(path)),
        streaming, true,
        if(ovrPathExists) Some(StreamingByteReader(FileRangeReader(ovrPath))) else None
      )
    else
      readSingleband(
        ByteBuffer.wrap(Filesystem.slurp(path)),
        streaming, true,
        if(ovrPathExists) Some(ByteBuffer.wrap(Filesystem.slurp(ovrPath))) else None
      )
  }

  def readSingleband(bytes: Array[Byte]): SinglebandGeoTiff =
    readSingleband(ByteBuffer.wrap(bytes), false)

  def readSingleband(bytes: Array[Byte], streaming: Boolean): SinglebandGeoTiff =
    readSingleband(ByteBuffer.wrap(bytes), streaming)

  def readSingleband(byteReader: ByteReader): SinglebandGeoTiff =
    readSingleband(byteReader, false)

  def readSingleband(byteReader: ByteReader, e: Extent): SinglebandGeoTiff =
    readSingleband(byteReader, Some(e))

  def readSingleband(byteReader: ByteReader, e: Option[Extent]): SinglebandGeoTiff =
    e match {
      case Some(x) => readSingleband(byteReader, true).crop(x)
      case None => readSingleband(byteReader)
    }

  /* Read a single band GeoTIFF file.
   * If there is more than one band in the GeoTiff, read the first band only.
   */
  def readSingleband(byteReader: ByteReader, streaming: Boolean): SinglebandGeoTiff =
    readSingleband(byteReader, streaming, true, None)

  def readSingleband(byteReader: ByteReader, streaming: Boolean, withOverviews: Boolean, byteReaderExternal: Option[ByteReader]): SinglebandGeoTiff = {
    def getSingleband(geoTiffTile: GeoTiffTile, info: GeoTiffInfo): SinglebandGeoTiff =
      SinglebandGeoTiff(
        geoTiffTile,
        info.extent,
        info.crs,
        info.tags,
        info.options,
        info.overviews.map { i => getSingleband(geoTiffSinglebandTile(i), i) }
      )

    val info = GeoTiffInfo.read(byteReader, streaming, withOverviews, byteReaderExternal)
    val geoTiffTile = geoTiffSinglebandTile(info)

    getSingleband(geoTiffTile, info)
  }

  def geoTiffSinglebandTile(info: GeoTiffInfo): GeoTiffTile =
    if(info.bandCount == 1) {
      GeoTiffTile(
        info.segmentBytes,
        info.decompressor,
        info.segmentLayout,
        info.compression,
        info.cellType,
        Some(info.bandType),
        info.overviews.map(geoTiffSinglebandTile)
      )
    } else {
      GeoTiffMultibandTile(
        info.segmentBytes,
        info.decompressor,
        info.segmentLayout,
        info.compression,
        info.bandCount,
        info.cellType,
        Some(info.bandType),
        info.overviews.map(geoTiffMultibandTile)
      ).band(0)
    }

  /* Read a multi band GeoTIFF file.
   */
  def readMultiband(path: String): MultibandGeoTiff =
    readMultiband(path, false)

  /* Read in only the extent for each band in a multi ban GeoTIFF file.
   */
  def readMultiband(path: String, e: Extent): MultibandGeoTiff =
    readMultiband(path, Some(e))

  /* Read in only the extent for each band in a multi ban GeoTIFF file.
   */
  def readMultiband(path: String, e: Option[Extent]): MultibandGeoTiff =
    e match {
      case Some(x) => readMultiband(path, true).crop(x)
      case None => readMultiband(path)
    }

  /* Read a multi band GeoTIFF file.
   */
  def readMultiband(path: String, streaming: Boolean): MultibandGeoTiff = {
    val ovrPath = s"${path}.ovr"
    val ovrPathExists = new File(ovrPath).isFile
    if (streaming)
      readMultiband(
        StreamingByteReader(FileRangeReader(path)),
        streaming, true,
        if(ovrPathExists) Some(StreamingByteReader(FileRangeReader(ovrPath))) else None
      )
    else
      readMultiband(
        ByteBuffer.wrap(Filesystem.slurp(path)),
        streaming, true,
        if(ovrPathExists) Some(ByteBuffer.wrap(Filesystem.slurp(ovrPath))) else None
      )
  }

  def readMultiband(byteReader: ByteReader, e: Extent): MultibandGeoTiff =
    readMultiband(byteReader, Some(e))

  def readMultiband(byteReader: ByteReader, e: Option[Extent]): MultibandGeoTiff =
    e match {
      case Some(x) => readMultiband(byteReader, true).crop(x)
      case None => readMultiband(byteReader)
    }

  /* Read a multi band GeoTIFF file.
   */
  def readMultiband(bytes: Array[Byte]): MultibandGeoTiff =
    readMultiband(bytes, false)

  def readMultiband(bytes: Array[Byte], streaming: Boolean): MultibandGeoTiff =
    readMultiband(ByteBuffer.wrap(bytes), streaming)

  def readMultiband(byteReader: ByteReader): MultibandGeoTiff =
    readMultiband(byteReader, false)

  def readMultiband(byteReader: ByteReader, streaming: Boolean): MultibandGeoTiff =
    readMultiband(byteReader, streaming, true, None)

  def readMultiband(byteReader: ByteReader, streaming: Boolean, withOverviews: Boolean, byteReaderExternal: Option[ByteReader]): MultibandGeoTiff = {
    def getMultiband(geoTiffTile: GeoTiffMultibandTile, info: GeoTiffInfo): MultibandGeoTiff =
      new MultibandGeoTiff(
        geoTiffTile,
        info.extent,
        info.crs,
        info.tags,
        info.options,
        info.overviews.map { i => getMultiband(geoTiffMultibandTile(i), i) }
      )

    val info = GeoTiffInfo.read(byteReader, streaming, withOverviews, byteReaderExternal)
    val geoTiffTile = geoTiffMultibandTile(info)

    getMultiband(geoTiffTile, info)
  }

    def geoTiffMultibandTile(info: GeoTiffInfo): GeoTiffMultibandTile = {
      GeoTiffMultibandTile(
        info.segmentBytes,
        info.decompressor,
        info.segmentLayout,
        info.compression,
        info.bandCount,
        info.cellType,
        Some(info.bandType),
        info.overviews.map(geoTiffMultibandTile)
      )
    }

  implicit val singlebandGeoTiffReader: GeoTiffReader[Tile] = new GeoTiffReader[Tile]{
    def read(byteReader: ByteReader, streaming: Boolean): GeoTiff[Tile] =
      GeoTiffReader.readSingleband(byteReader, streaming)
  }

  implicit val multibandGeoTiffReader: GeoTiffReader[MultibandTile] = new GeoTiffReader[MultibandTile]{
    def read(byteReader: ByteReader, streaming: Boolean): GeoTiff[MultibandTile] =
      GeoTiffReader.readMultiband(byteReader, streaming)
  }

  def apply[V <: CellGrid[Int]](implicit ev: GeoTiffReader[V]): GeoTiffReader[V] = ev
}

trait GeoTiffReader[V <: CellGrid[Int]] extends Serializable {
  def read(byteReader: ByteReader, streaming: Boolean): GeoTiff[V]

  def read(bytes: Array[Byte]): GeoTiff[V] =
    read(ByteBuffer.wrap(bytes), streaming = false)
}
