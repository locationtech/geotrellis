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

import geotrellis.layers.TileLayerMetadata
import geotrellis.raster.{Tile, TileFeature}
import geotrellis.tiling.SpatialKey
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.s3.testkit._
import geotrellis.spark.testkit.io._
import geotrellis.spark.testkit.testfiles.TestTileFeatureFiles
import geotrellis.spark.testkit.TestEnvironment
import org.scalatest._

class S3TileFeatureSpatialSpec
  extends PersistenceSpec[SpatialKey, TileFeature[Tile, Tile], TileLayerMetadata[SpatialKey]]
    with SpatialKeyIndexMethods
    with TestEnvironment
    with TestTileFeatureFiles
    with AllOnesTestTileFeatureSpec {

  lazy val bucket = "mock-bucket"
  lazy val prefix = "catalog"
  val client = MockS3Client()
  S3TestUtils.cleanBucket(client, bucket)
  registerAfterAll { () =>
    S3TestUtils.cleanBucket(client, bucket)
  }
  S3ClientProducer.set(() => MockS3Client())
  lazy val threads = 2

  lazy val getS3Client = () => MockS3Client()
  lazy val attributeStore = new S3AttributeStore(bucket, prefix, getS3Client)

  lazy val rddReader = new S3RDDReader(getS3Client, threads)
  lazy val rddWriter = new S3RDDWriter(getS3Client, threads)

  lazy val reader = new S3LayerReader(attributeStore, getS3Client, threads)
  lazy val writer = new S3LayerWriter(attributeStore, bucket, prefix, identity, getS3Client, threads)
  lazy val deleter = new S3LayerDeleter(attributeStore, getS3Client)
  lazy val copier  = new S3LayerCopier(attributeStore, bucket, prefix, getS3Client)
  lazy val creader = new S3CollectionLayerReader(attributeStore)
  lazy val reindexer = GenericLayerReindexer[S3LayerHeader](attributeStore, reader, writer, deleter, copier)
  lazy val mover = GenericLayerMover(copier, deleter)
  lazy val tiles = new S3ValueReader(attributeStore, getS3Client)
  lazy val sample = AllOnesTestFile
}
