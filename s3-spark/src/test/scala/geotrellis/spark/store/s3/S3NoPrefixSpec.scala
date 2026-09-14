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
import geotrellis.layer.*
import geotrellis.store.*
import geotrellis.store.s3.*
import geotrellis.spark.store.*
import geotrellis.spark.testkit.io.*
import geotrellis.spark.testkit.testfiles.TestFiles
import geotrellis.spark.testkit.TestEnvironment

class S3NoPrefixSpec
  extends PersistenceSpec[SpatialKey, Tile, TileLayerMetadata[SpatialKey]]
    with SpatialKeyIndexMethods
    with TestEnvironment with TestFiles
    with AllOnesTestTileSpec {

  lazy val s3Uri = new AmazonS3URI(s"s3://mock-bucket")
  lazy val bucket = s3Uri.getBucket
  lazy val prefix = s3Uri.getKey

  val client = MockS3Client()

  S3TestUtils.cleanBucket(client, bucket)
  registerAfterAll { () =>
    S3TestUtils.cleanBucket(client, bucket)
  }

  // We need to register the mock client for SPI loaded classes
  S3ClientProducer.set(() => MockS3Client())

  lazy val attributeStore = S3AttributeStore(bucket, prefix, MockS3Client.instance)

  lazy val rddReader = new S3RDDReader(MockS3Client.instance)
  lazy val rddWriter = new S3RDDWriter(MockS3Client.instance)

  lazy val reader: S3LayerReader = new S3LayerReader(attributeStore, MockS3Client.instance)
  lazy val creader: S3CollectionLayerReader = new S3CollectionLayerReader(attributeStore)
  lazy val writer: S3LayerWriter = new S3LayerWriter(attributeStore, bucket, prefix, identity, MockS3Client.instance)
  lazy val deleter: S3LayerDeleter = new S3LayerDeleter(attributeStore, MockS3Client.instance)
  lazy val copier: S3LayerCopier = new S3LayerCopier(attributeStore, bucket, prefix, MockS3Client.instance)
  lazy val reindexer = GenericLayerReindexer(attributeStore, reader, writer, deleter, copier)
  lazy val mover: GenericLayerMover[LayerId] = GenericLayerMover(copier, deleter)
  lazy val tiles: S3ValueReader = new S3ValueReader(attributeStore, MockS3Client.instance)
  lazy val sample: AllOnesTestFile.type = AllOnesTestFile
}
