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

package geotrellis.store

import geotrellis.raster.CellGrid
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.render.{Jpg, Png}

import cats.syntax.either._
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{HeadObjectRequest, NoSuchKeyException, RequestPayer}

import scala.util.Try

package object s3 {
  // https://github.com/aws/aws-sdk-java/blob/1.12.267/aws-java-sdk-s3/src/main/java/com/amazonaws/services/s3/Headers.java#L201
  val REQUESTER_PAYS_HEADER: String = "x-amz-request-payer"

  private[geotrellis] def makePath(chunks: String*): String =
    chunks
      .collect { case str if str.nonEmpty => if(str.endsWith("/")) str.dropRight(1) else str }
      .mkString("/")

  implicit class S3ClientExtension(val client: S3Client) extends AnyVal {
    def objectExists(bucket: String, key: String): Boolean = {
      val request = HeadObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build()

      Try(client.headObject(request))
        .map(_ => true)
        .recover { case _: NoSuchKeyException => false }
        .toEither
        .valueOr(throw _)
    }

    def objectExists(path: String): Boolean = {
      val arr = path.split("/").filterNot(_.isEmpty)
      val bucket = arr.head
      val key = arr.tail.mkString("/")
      client.objectExists(bucket, key)
    }
  }

  implicit class ClientOverrideConfigurationBuilderOps(val self: ClientOverrideConfiguration.Builder) extends AnyVal {
    def requestPayer(requestPayer: Option[RequestPayer]): ClientOverrideConfiguration.Builder =
      requestPayer.map(_.toString).fold(self)(self.putHeader(REQUESTER_PAYS_HEADER, _))
  }

  implicit class withJpgS3WriteMethods(val self: Jpg) extends JpgS3WriteMethods(self)

  implicit class withPngS3WriteMethods(val self: Png) extends PngS3WriteMethods(self)

  implicit class withGeoTiffS3WriteMethods[T <: CellGrid[Int]](val self: GeoTiff[T]) extends GeoTiffS3WriteMethods[T](self)
}
