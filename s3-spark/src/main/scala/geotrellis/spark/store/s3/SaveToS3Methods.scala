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

package geotrellis.spark.store.s3

import geotrellis.util.MethodExtensions

import software.amazon.awssdk.services.s3.model.PutObjectRequest
import org.apache.spark.rdd.RDD


class SaveToS3Methods[K](val self: RDD[(K, Array[Byte])]) extends MethodExtensions[RDD[(K, Array[Byte])]] {

  /**
    * Saves each RDD value to an S3 key.
    *
    * @param keyToUri A function from K (a key) to an S3 URI
    * @param putObjectModifier  Function that will be applied ot S3 PutObjectRequests, so that they can be modified (e.g. to change the ACL settings)
    * @param threads   Number of threads dedicated for the IO
    */
  def saveToS3(keyToUri: K => String, putObjectModifier: PutObjectRequest => PutObjectRequest = { p => p }, threads: Int = SaveToS3.defaultThreadCount): Unit =
    SaveToS3(self, keyToUri, putObjectModifier)
}
