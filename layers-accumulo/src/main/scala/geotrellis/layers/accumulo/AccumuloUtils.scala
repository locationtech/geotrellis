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

package geotrellis.layers.accumulo

import geotrellis.tiling.{Bounds, Boundable, KeyBounds, EmptyBounds}
import geotrellis.layers.accumulo._
import geotrellis.layers.index.{KeyIndexMethod, KeyIndex}

import org.apache.accumulo.core.data.Key
import org.apache.hadoop.io.Text

import scala.collection.JavaConverters._


object AccumuloUtils {
  /**
   * Mapping KeyBounds of Extent to SFC ranges will often result in a set of non-contigrious ranges.
   * The indices exluded by these ranges should not be included in split calculation as they will never be seen.
   */
  def getSplits[K](kb: KeyBounds[K], ki: KeyIndex[K], count: Int): Seq[BigInt] =
    KeyIndex.breaks(kb, ki, count)

  /**
    * Split the given Accumulo table into the given number of tablets.
    * This should improve the ingest performance, as it will allow
    * more than one tablet server to participate in the ingestion.
    *
    * @param  tableName         The name of the table to be split
    * @param  accumuloInstnace  The Accumulo instance associated with the ingest
    * @param  keyBounds         The KeyBounds of the RDD that is being stored in the table
    * @param  keyIndexer        The indexing scheme used to turn keys K into Accumulo keys
    * @param  count             The number of tablets to split the table into
    */
  def addSplits[K](
    tableName: String,
    accumuloInstance: AccumuloInstance,
    keyBounds: KeyBounds[K],
    keyIndexer: KeyIndex[K],
    count: Int
  ) = {
    val ops = accumuloInstance.connector.tableOperations

    val splits = AccumuloUtils
      .getSplits(keyBounds, keyIndexer, count)
      .map({ i => AccumuloKeyEncoder.index2RowId(i) })

    ops.addSplits(tableName, new java.util.TreeSet(splits.asJava))
  }

}
