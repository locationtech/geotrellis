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

package geotrellis.spark.store.s3.cog

import geotrellis.raster.Tile
import geotrellis.tiling.SpaceTimeKey
import geotrellis.layers._
import geotrellis.store.s3._
import geotrellis.store.s3.cog._
import geotrellis.spark._
import geotrellis.spark.store._
import geotrellis.spark.store.cog._
import geotrellis.spark.store.s3._
import geotrellis.spark.store.s3.cog._
import geotrellis.spark.store.s3.testkit._
import geotrellis.spark.testkit.TestEnvironment
import geotrellis.spark.testkit.io._
import geotrellis.spark.testkit.io.cog._
import geotrellis.spark.testkit.testfiles.cog._

import org.scalatest._

class COGS3SpaceTimeSpec
  extends COGPersistenceSpec[SpaceTimeKey, Tile]
    with COGSpaceTimeKeyIndexMethods
    with TestEnvironment
    with COGTestFiles
    with COGCoordinateSpaceTimeSpec
    with COGLayerUpdateSpaceTimeTileSpec {

  lazy val bucket = "mock-bucket"
  lazy val prefix = "catalog"
  val client = MockS3Client()
  S3TestUtils.cleanBucket(client, bucket)
  registerAfterAll { () =>
    S3TestUtils.cleanBucket(client, bucket)
  }

  lazy val getS3Client = () => MockS3Client()
  lazy val attributeStore = new S3AttributeStore(bucket, prefix, getS3Client)

  lazy val reader = new S3COGLayerReader(attributeStore, getS3Client)
  lazy val creader = new S3COGCollectionLayerReader(attributeStore, getS3Client)
  lazy val writer = new S3COGLayerWriter(attributeStore, attributeStore.bucket, attributeStore.prefix, getS3Client)
  // TODO: implement and test all layer functions
  // lazy val deleter = new S3LayerDeleter(attributeStore) { override val getS3Client = () => MockS3Client }
  // lazy val copier = new S3LayerCopier(attributeStore, bucket, prefix) { override val getS3Client = () => MockS3Client() }
  // lazy val reindexer = GenericLayerReindexer[S3LayerHeader](attributeStore, reader, writer, deleter, copier)
  // lazy val mover = GenericLayerMover(copier, deleter)
  lazy val tiles = new S3COGValueReader(attributeStore, getS3Client)
  lazy val sample = CoordinateSpaceTime
}
