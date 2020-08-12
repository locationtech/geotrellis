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

import geotrellis.raster.{Tile, TileFeature}
import geotrellis.layer._
import geotrellis.store._
import geotrellis.store.s3._
import geotrellis.spark.store._
import geotrellis.spark.testkit.io._
import geotrellis.spark.testkit.testfiles.TestTileFeatureFiles
import geotrellis.spark.testkit.TestEnvironment

import org.scalatest.BeforeAndAfterAll

class S3TileFeatureSpaceTimeSpec
  extends PersistenceSpec[SpaceTimeKey, TileFeature[Tile, Tile], TileLayerMetadata[SpaceTimeKey]]
    with SpaceTimeKeyIndexMethods
    with TestEnvironment
    with TestTileFeatureFiles
    with CoordinateSpaceTimeTileFeatureSpec
    with LayerUpdateSpaceTimeTileFeatureSpec
    with BeforeAndAfterAll {

  lazy val bucket = "mock-bucket"
  lazy val prefix = "catalog"
  val client = MockS3Client()
  S3TestUtils.cleanBucket(client, bucket)
  registerAfterAll { () =>
    S3TestUtils.cleanBucket(client, bucket)
  }
  S3ClientProducer.set(() => MockS3Client())
  
  lazy val attributeStore = new S3AttributeStore(bucket, prefix, MockS3Client.instance)

  lazy val rddReader = new S3RDDReader(MockS3Client.instance)
  lazy val rddWriter = new S3RDDWriter(MockS3Client.instance)

  lazy val reader = new S3LayerReader(attributeStore, MockS3Client.instance)
  lazy val writer = new S3LayerWriter(attributeStore, bucket, prefix, identity, MockS3Client.instance)
  lazy val deleter = new S3LayerDeleter(attributeStore, MockS3Client.instance)
  lazy val copier = new S3LayerCopier(attributeStore, bucket, prefix, MockS3Client.instance)
  lazy val creader = new S3CollectionLayerReader(attributeStore)
  lazy val reindexer = GenericLayerReindexer(attributeStore, reader, writer, deleter, copier)
  lazy val mover = GenericLayerMover(copier, deleter)
  lazy val tiles = new S3ValueReader(attributeStore, MockS3Client.instance)
  lazy val sample =  CoordinateSpaceTime
}
