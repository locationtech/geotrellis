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

import geotrellis.raster.Tile
import geotrellis.tiling.SpatialKey
import geotrellis.layers._
import geotrellis.layers.index._
import geotrellis.store.s3._
import geotrellis.spark.store._
import geotrellis.spark.store.s3.testkit._
import geotrellis.spark._
import geotrellis.spark.testkit.io._
import geotrellis.spark.testkit.testfiles.TestFiles
import geotrellis.spark.testkit.TestEnvironment
import org.scalatest._

class S3SpatialSpec
  extends PersistenceSpec[SpatialKey, Tile, TileLayerMetadata[SpatialKey]]
    with SpatialKeyIndexMethods
    with TestEnvironment with TestFiles
    with AllOnesTestTileSpec {

  lazy val bucket = "mock-bucket"
  lazy val prefix = "catalog"
  val client = MockS3Client()
  S3TestUtils.cleanBucket(client, bucket)
  registerAfterAll { () =>
    S3TestUtils.cleanBucket(client, bucket)
  }

  // We need to register the mock client for SPI loaded classes
  S3ClientProducer.set(() => MockS3Client())

  lazy val getS3Client = () => MockS3Client()
  lazy val attributeStore = new S3AttributeStore(bucket, prefix, getS3Client)
  lazy val threadCount = 2

  lazy val rddReader = new S3RDDReader(getS3Client, threadCount)
  lazy val rddWriter = new S3RDDWriter(getS3Client, threadCount)

  lazy val reader = new S3LayerReader(attributeStore, getS3Client, threadCount)
  lazy val creader = new S3CollectionLayerReader(attributeStore)
  lazy val writer = new S3LayerWriter(attributeStore, bucket, prefix, identity, getS3Client)
  lazy val deleter = new S3LayerDeleter(attributeStore, getS3Client)
  lazy val copier  = new S3LayerCopier(attributeStore, bucket, prefix, getS3Client)
  lazy val reindexer = GenericLayerReindexer[S3LayerHeader](attributeStore, reader, writer, deleter, copier)
  lazy val mover = GenericLayerMover(copier, deleter)
  lazy val tiles = new S3ValueReader(attributeStore, getS3Client)
  lazy val sample = AllOnesTestFile
}
