/*
 * Copyright 2018 Azavea
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

package geotrellis.spark.io.hadoop

import geotrellis.util.StreamingByteReader

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import java.net.URI

package object cog {
  def byteReader(uri: URI, conf: Configuration = new Configuration): StreamingByteReader =
    StreamingByteReader(HdfsRangeReader(new Path(uri), conf))

  private[cog]
  def makePath(chunks: String*) =
    chunks
      .collect { case str if str.nonEmpty => if(str.endsWith("/")) str.dropRight(1) else str }
      .mkString("/")
}
