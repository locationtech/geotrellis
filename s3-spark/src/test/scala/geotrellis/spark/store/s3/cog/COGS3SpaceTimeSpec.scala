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
import geotrellis.layer.*
import geotrellis.store.s3.*
import geotrellis.store.s3.cog.*
import geotrellis.spark.store.cog.*
import geotrellis.spark.store.s3.*
import geotrellis.spark.testkit.TestEnvironment
import geotrellis.spark.testkit.io.cog.*
import geotrellis.spark.testkit.testfiles.cog.*
import geotrellis.util.identityComponent
import geotrellis.raster.Implicits.withSinglebandMergeMethods
import geotrellis.raster.Implicits.withSinglebandTilePrototypeMethods
import geotrellis.raster.crop.Implicits.withSinglebandTileCropMethods
import org.apache.spark.rdd.RDD

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
  
  lazy val attributeStore = new S3AttributeStore(bucket, prefix, MockS3Client.instance)

  lazy val reader: S3COGLayerReader = new S3COGLayerReader(attributeStore, MockS3Client.instance)
  lazy val creader: S3COGCollectionLayerReader = new S3COGCollectionLayerReader(attributeStore, MockS3Client.instance)
  lazy val writer: S3COGLayerWriter = new S3COGLayerWriter(attributeStore, attributeStore.bucket, attributeStore.prefix, MockS3Client.instance)
  // TODO: implement and test all layer functions
  // lazy val deleter = new S3LayerDeleter(attributeStore) { override val MockS3Client.instance = () => MockS3Client }
  // lazy val copier = new S3LayerCopier(attributeStore, bucket, prefix) { override val MockS3Client.instance = () => MockS3Client() }
  // lazy val reindexer = GenericLayerReindexer[S3LayerHeader](attributeStore, reader, writer, deleter, copier)
  // lazy val mover = GenericLayerMover(copier, deleter)
  lazy val tiles: S3COGValueReader = new S3COGValueReader(attributeStore, MockS3Client.instance)
  lazy val sample: RDD[(SpaceTimeKey, Tile)] with Metadata[TileLayerMetadata[SpaceTimeKey]] = CoordinateSpaceTime
}
