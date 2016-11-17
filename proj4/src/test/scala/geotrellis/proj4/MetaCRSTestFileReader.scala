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

package geotrellis.proj4

import java.io.File
import com.opencsv.CSVReader

import scala.collection.breakOut
import scala.collection.JavaConverters._
import scala.util.Try

/**
 * Reads a file in MetaCRS Test format
 * into a list of {@link MetaCRSTestCase}.
 * This format is a CSV file with a standard set of columns.
 * Each record defines a transformation from one coordinate system
 * to another.
 * For full details on the file format, see http://trac.osgeo.org/metacrs/
 * 
 * @author Martin Davis (port by Rob Emanuele)
 *
 */
object MetaCRSTestFileReader {
  final val COL_COUNT = 19

  private def parse(path: String): java.util.List[Array[String]] = {
    val reader = new CSVReader(new java.io.FileReader(path))
    try {
      reader.readAll().asInstanceOf[java.util.List[Array[String]]]
    } finally {
      reader.close()
    }
  }
  
  def readTests(file: File): List[MetaCRSTestCase] = {
    parse(file.getAbsolutePath).asScala.iterator
      .filter(r => r.nonEmpty && !r.head.startsWith("#"))
      .drop(1)
      .map(parseTest)
      .to[List]
  }

  private def parseTest(cols: Array[String]): MetaCRSTestCase = {
    if (cols.length != COL_COUNT)
      throw new IllegalStateException("Expected " + COL_COUNT + " columns in file, but found " + cols.length)

    val testName    = cols(0)
    val testMethod  = cols(1)
    val srcCrsAuth  = cols(2)
    val srcCrs      = cols(3)
    val tgtCrsAuth  = cols(4)
    val tgtCrs      = cols(5)
    val srcOrd1     = parseNumber(cols(6))
    val srcOrd2     = parseNumber(cols(7))
    val srcOrd3     = parseNumber(cols(8))
    val tgtOrd1     = parseNumber(cols(9))
    val tgtOrd2     = parseNumber(cols(10))
    val tgtOrd3     = parseNumber(cols(11))
    val tolOrd1     = parseNumber(cols(12))
    val tolOrd2     = parseNumber(cols(13))
    val tolOrd3     = parseNumber(cols(14))
    val using       = cols(15)
    val dataSource  = cols(16)
    val dataCmnts   = cols(17)
    val maintenanceCmnts = cols(18)
    
    MetaCRSTestCase(testName,testMethod,srcCrsAuth,srcCrs,tgtCrsAuth,tgtCrs,srcOrd1,srcOrd2,srcOrd3,tgtOrd1,tgtOrd2,tgtOrd3,tolOrd1,tolOrd2,tolOrd3,using,dataSource,dataCmnts,maintenanceCmnts)
  }
 
  private def parseNumber(numStr: String): Double = {
    if (numStr == null || numStr.isEmpty) {
      Double.NaN
    } else {
      numStr.toDouble
    }
  }
}
