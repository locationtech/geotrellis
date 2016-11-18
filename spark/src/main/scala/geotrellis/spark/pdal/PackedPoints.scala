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

package geotrellis.spark.pdal

import io.pdal.DimType

import java.nio.{ByteBuffer, ByteOrder}

/** PackedPoints abstraction to work with packed points in JVM memory */
case class PackedPoints(packedPoints: Array[Byte], dimTypes: Map[String, SizedDimType]) {
  def pointSize: Long = dimTypes.values.map(_.size).sum
  def length: Int = packedPoints.length
  def isPoint: Boolean = length == pointSize

  /**
    * Reads a packed point by point id from a set of packed points.
    */
  def get(i: Int): Array[Byte] = {
    val from = (i * pointSize).toInt
    val to = {
      val t = (from + pointSize).toInt
      if(t > length) length else t
    }

    packedPoints.slice(from, to)
  }

  /**
    * Reads dim from a packed point
    */
  def get(packedPoint: Array[Byte], sdim: SizedDimType): ByteBuffer = get(packedPoint, sdim.dimType.id)
  def get(packedPoint: Array[Byte], dim: DimType): ByteBuffer = get(packedPoint, dim.id)
  def get(packedPoint: Array[Byte], dim: String): ByteBuffer = {
    val sdt = dimTypes(dim)
    val from = sdt.offset.toInt
    val to = from + sdt.size.toInt
    ByteBuffer.wrap(packedPoint.slice(from, to)).order(ByteOrder.nativeOrder())
  }

  def getX(packedPoint: Array[Byte], dim: String): ByteBuffer = get(packedPoint, DimType.Id.X)
  def getY(packedPoint: Array[Byte], dim: String): ByteBuffer = get(packedPoint, DimType.Id.Y)
  def getZ(packedPoint: Array[Byte], dim: String): ByteBuffer = get(packedPoint, DimType.Id.Z)
}
