/*
 * Copyright 2021 Azavea
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

package geotrellis.store.s3

import geotrellis.raster.CellGrid
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.render.{Jpg, Png}
import geotrellis.util.MethodExtensions
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest

trait S3RasterMethods[T] extends MethodExtensions[T] {
  def write(uri: AmazonS3URI, client: S3Client = S3ClientProducer.get()): Unit

  protected def writeArray(uri: AmazonS3URI, client: S3Client, bytes: Array[Byte]): Unit = {
    val objectRequest = PutObjectRequest.builder
      .bucket(uri.getBucket)
      .key(uri.getKey)
      .build

    client.putObject(objectRequest, RequestBody.fromBytes(bytes))
  }
}

abstract class JpgS3WriteMethods(self: Jpg) extends S3RasterMethods[Jpg] {
  def write(uri: AmazonS3URI, client: S3Client = S3ClientProducer.get()): Unit =
    writeArray(uri, client, self.bytes)
}

abstract class PngS3WriteMethods(self: Png) extends S3RasterMethods[Png] {
  def write(uri: AmazonS3URI, client: S3Client = S3ClientProducer.get()): Unit =
    writeArray(uri, client, self.bytes)
}

abstract class GeoTiffS3WriteMethods[T <: CellGrid[Int]](self: GeoTiff[T]) extends S3RasterMethods[GeoTiff[T]] {
  def write(uri: AmazonS3URI, client: S3Client = S3ClientProducer.get()): Unit =
    writeArray(uri, client, self.toCloudOptimizedByteArray)
}
