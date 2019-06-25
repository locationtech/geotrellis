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

package geotrellis.doc.examples.spark

import geotrellis.layer._
import geotrellis.spark._
import geotrellis.spark.store._
import geotrellis.store._
import geotrellis.store.json._
import geotrellis.store.index._
import scala.reflect.ClassTag

import _root_.io.circe._
import _root_.io.circe.syntax._
import _root_.io.circe.generic.JsonCodec
import cats.syntax.either._

import scala.reflect.ClassTag

// --- //

/**
 * An example of a generic KeyIndex which accounts for sharding that could occur
 * in a data store like Apache Accumulo. Given a shard count (pre-configured),
 * this index adds a "shard prefix" to the true index as given by the ''inner''
 * argument. Accumulo can shard based off of the first digit of a row ID,
 * and since we use a round-robin approach to generate prefixes, this distributes
 * spatially close indices across different shards, and thus helps avoid hot spots.
 *
 * ==Assumptions==
 *   - The given shard count will be between 1 and 8.
 *   - The ''inner'' index will produce a value less than 2^60 for any
 *     given key.
 */
class ShardingKeyIndex[K](val inner: KeyIndex[K], val shardCount: Int) extends KeyIndex[K] {

  /* Necessary for extending `KeyIndex` */
  def keyBounds: KeyBounds[K] =
    inner.keyBounds

  /**
   * Prefix the shard bits to the original index. Example:
   *
   * {{{
   * val i: Long = 37  // ... 0010 0101
   * val s: Long = 7   // ... 0000 0111
   *
   * // 0111 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0010 0101
   * prefixWithShard(i, s) == 8070450532247928869
   * }}}
   */
  private def prefixWithShard(i: BigInt, shard: Long): BigInt =
    (shard << 60) | i

  /* Necessary for extending `KeyIndex` */
  def toIndex(key: K): BigInt = {
    val i: BigInt = inner.toIndex(key)
    val shard: Long = (i % shardCount).toLong /* Shard prefix between 0 and 7 */

    prefixWithShard(inner.toIndex(key), shard)
  }

  /* Necessary for extending `KeyIndex` */
  def indexRanges(keyRange: (K, K)): Seq[(BigInt, BigInt)] = {
    inner
      .indexRanges(keyRange)
      .flatMap({ case (i1, i2) =>
        for (s <- 0 until shardCount) yield {
          (prefixWithShard(i1, s.toLong), prefixWithShard(i2, s.toLong))
        }
      })
  }
}

object ShardingKeyIndex {
  /**
    * Standard Json codecs for [[ShardingKeyIndex]], parameterized on the key type ''K''.
    */

  /* This is the foundation of the reflection-based deserialization process */
  val sharding = "sharding"

  /** An [[Encoder]] for [[ZVoxelKeyIndex]]. */
  implicit def shardingKeyIndexEncoder[K: Encoder: ClassTag]: Encoder[ShardingKeyIndex[K]] =
    Encoder.encodeJson.contramap[ShardingKeyIndex[K]] { index =>
      Json.obj(
        "type" -> sharding.asJson,
        "properties" -> Json.obj(
          "inner" -> index.inner.asJson,
          "shardCount" -> index.shardCount.asJson
        )
      )
    }

  implicit def shardingKeyIndexDecoder[K: Decoder: ClassTag]: Decoder[ShardingKeyIndex[K]] =
    Decoder.decodeHCursor.emap { c: HCursor =>
      (c.downField("type").as[String], c.downField("properties")) match {
        case (Right(typeName), properties) =>
          if(typeName != sharding) Left(s"Wrong KeyIndex type: $sharding expected.")
          else {
            (properties.downField("inner").as[KeyIndex[K]],
            properties.downField("shardCount").as[Int]) match {
              case (Right(inner), Right(shardCount)) => Right(new ShardingKeyIndex(inner, shardCount))
              case _ => Left("Couldn't deserialize ShardingKeyIndex.")
            }
          }

        case _ => Left("Wrong KeyIndex type: ShardingKeyIndex expected.")
      }
    }
}

/**
 * Register [[ShardingKeyIndex]]'s Codecs with the central GeoTrellis
 * [[KeyIndex]] JsonFormat registry.
 *
 * Q: Why do we need to do this?
 *
 * A: The [[KeyIndex]] trait is critical to Tile layer IO. However,
 * it is always used generically, without any class depending on KeyIndex's subtypes
 * directly. This is advantageous, as the user never needs to externally keep track of
 * what index they ingested a layer with. They can read and update a layer
 * with less of a mental burden. That said, inventing new KeyIndex subclasses
 * becomes labourous. In this way, we (the GeoTrellis authors) have accepted
 * the burden of complexity to the benefit of our users.
 */
class ShardingKeyIndexRegistrator extends KeyIndexRegistrator {
  /**
   * Necessary to extend KeyIndexRegistrator. This tells the central registry
   * about each possible JsonFormat.
   */
  def register(keyIndexRegistry: KeyIndexRegistry): Unit = {
    /* You need to make a [[KeyIndexFormatEntry]] for each key type */
    keyIndexRegistry.register(
      KeyIndexFormatEntry[SpatialKey, ShardingKeyIndex[SpatialKey]](ShardingKeyIndex.sharding)
    )
    keyIndexRegistry.register(
      KeyIndexFormatEntry[SpaceTimeKey, ShardingKeyIndex[SpaceTimeKey]](ShardingKeyIndex.sharding)
    )
  }
}
