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

package geotrellis.spark.io.s3

import geotrellis.spark.io._
import com.typesafe.scalalogging.LazyLogging
import geotrellis.layers.LayerId

class S3LayerDeleter(val attributeStore: AttributeStore) extends LazyLogging with LayerDeleter[LayerId] {

  def getS3Client: () => S3Client = () => S3Client.DEFAULT

  def delete(id: LayerId): Unit = {
    try {
      val header = attributeStore.readHeader[S3LayerHeader](id)
      val bucket = header.bucket
      val prefix = header.key + "/"
      val s3Client = getS3Client()

      s3Client.deleteListing(bucket, s3Client.listObjects(bucket, prefix))
    } catch {
      case e: AttributeNotFoundError =>
        logger.info(s"Metadata for $id was not found. Any associated layer data (if any) will require manual deletion")
        throw new LayerDeleteError(id).initCause(e)
    } finally {
      attributeStore.delete(id)
    }
  }
}

object S3LayerDeleter {
  def apply(attributeStore: AttributeStore): S3LayerDeleter = new S3LayerDeleter(attributeStore)

  def apply(bucket: String, prefix: String): S3LayerDeleter = apply(S3AttributeStore(bucket, prefix))
}
