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

import geotrellis.store.s3._
import geotrellis.store.s3.conf.S3Config

import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.regions.Region

import java.net.URI

object MockS3Client {
  def apply(): S3Client = {
    val cred = AwsBasicCredentials.create("minio", "password")
    val credProvider = StaticCredentialsProvider.create(cred)
    val overrideConfig = ClientOverrideConfiguration.builder().requestPayer(S3Config.requestPayer).build()

    S3Client.builder()
      .overrideConfiguration(overrideConfig)
      // To forcePathStyle we can use localhost ip address
      // https://docs.aws.amazon.com/AmazonS3/latest/userguide/VirtualHosting.html#path-style-access
      // https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3.html
      .endpointOverride(new URI("http://127.0.0.1:9091"))
      .credentialsProvider(credProvider)
      .region(Region.US_EAST_1)
      .build()
  }

  @transient lazy val instance: S3Client = apply()
}
