/*
 * Copyright 2019 Azavea
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

import geotrellis.store.s3.conf.S3Config
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.core.retry.backoff.FullJitterBackoffStrategy
import software.amazon.awssdk.core.retry.conditions.{OrRetryCondition, RetryCondition}
import software.amazon.awssdk.awscore.retry.conditions.RetryOnErrorCodeCondition
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.core.retry.RetryPolicy

import java.time.Duration

/**
  *  Singleton which can be used to customize the default S3 client used throughout geotrellis
  *
  *  Various classes are loaded based on URI patterns and because SPI requires
  *   parameterless constructors, it is necessary to register any customizations
  *   to the S3Client that should be applied by default here.
  *
  */
object S3ClientProducer {
  @transient private lazy val client = {
    val retryCondition =
      OrRetryCondition.create(
        RetryCondition.defaultRetryCondition(),
        RetryOnErrorCodeCondition.create("RequestTimeout")
      )
    val backoffStrategy =
      FullJitterBackoffStrategy.builder()
        .baseDelay(Duration.ofMillis(50))
        .maxBackoffTime(Duration.ofMillis(15))
        .build()
    val retryPolicy =
      RetryPolicy.defaultRetryPolicy()
        .toBuilder
        .retryCondition(retryCondition)
        .backoffStrategy(backoffStrategy)
        .build()
    val overrideConfig =
      ClientOverrideConfiguration.builder()
        .retryPolicy(retryPolicy)
        .requestPayer(S3Config.requestPayer)
        .build()

    S3Client.builder()
      .overrideConfiguration(overrideConfig)
      .build()
  }

  private var summonClient: () => S3Client = {
    () => client
  }

  /**
    * Set an alternative default function for summoning S3Clients
    */
  def set(getClient: () => S3Client): Unit =
    summonClient = getClient

  /**
    * Get the current function registered as default for summong S3Clients
    */
  def get: () => S3Client =
    summonClient
}
