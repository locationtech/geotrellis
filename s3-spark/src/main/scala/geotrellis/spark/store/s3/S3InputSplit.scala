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

import com.typesafe.scalalogging.LazyLogging
import software.amazon.awssdk.services.s3.model.S3Object
import org.apache.hadoop.io.Writable
import org.apache.hadoop.mapreduce.InputSplit

import java.io.{DataOutput, DataInput}

/**
 * Represents are batch of keys to be read from an S3 bucket.
 * AWS credentials have already been discovered and provided by the S3InputFormat.
 */
class S3InputSplit extends InputSplit with Writable with LazyLogging
{
  var sessionToken: String = null
  var bucket: String = _
  var keys: Seq[String] = Vector.empty
  /** Combined size of objects in bytes */
  var size: Long = _

  def addKey(s3obj: S3Object): Long = {
    size += s3obj.size
    keys = keys :+ s3obj.key
    s3obj.size
  }

  override def getLength: Long = keys.length

  override def getLocations: Array[String] = Array.empty

  override def write(out: DataOutput): Unit = {
    out.writeBoolean(null != sessionToken)
    if (null != sessionToken)
      out.writeUTF(sessionToken)
    out.writeUTF(bucket)
    out.writeInt(keys.length)
    keys.foreach(out.writeUTF)
  }

  override def readFields(in: DataInput): Unit = {
    if(in.readBoolean)
      sessionToken = in.readUTF
    else
      sessionToken = null
    bucket = in.readUTF
    val keyCount = in.readInt
    keys = for (i <- 1 to keyCount) yield in.readUTF
  }
}
