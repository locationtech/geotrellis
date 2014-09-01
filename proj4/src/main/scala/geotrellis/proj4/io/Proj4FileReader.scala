/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.proj4.io

import geotrellis.proj4._

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StreamTokenizer

import collection.mutable.ArrayBuffer
import scala.io.Source

class Proj4FileReader {

  def readParametersFromFile(authorityCode: String, name: String): Array[String] = {
    // TODO: read comment preceding CS string as CS description
    // TODO: use simpler parser than StreamTokenizer for speed and flexibility
    // TODO: parse CSes line-at-a-time (this allows preserving CS param string for later access)
    
    val filename = "/nad/" + authorityCode.toLowerCase
    val inStr = getClass.getResourceAsStream( filename )
    if (inStr == null) {
      throw new IllegalStateException(s"Unable to access CRS file: $filename")
    }

    val reader = new BufferedReader( new InputStreamReader(inStr) )
    try {
      readFile(reader, name)
    }
    finally {
      if (reader != null)
        reader.close()
    }
  }

  private def createTokenizer(reader: BufferedReader): StreamTokenizer = {
    val t = new StreamTokenizer(reader)

    t.commentChar('#')
    t.ordinaryChars('0','9')
    t.ordinaryChars('.','.')
    t.ordinaryChars('-','-')
    t.ordinaryChars('+','+')
    t.wordChars('0','9')
    t.wordChars('\'','\'')
    t.wordChars('"','"')
    t.wordChars('_','_')
    t.wordChars('.','.')
    t.wordChars('-','-')
    t.wordChars('+','+')
    t.wordChars(',',',')
    t.wordChars('@','@')

    t
  }

  private def readFile(reader: BufferedReader, name: String): Array[String] = {
    val t = createTokenizer(reader)

    t.nextToken

    val ab = new ArrayBuffer[String]()
    while (t.ttype == '<' && ab.isEmpty) {
      t.nextToken

      if (t.ttype != StreamTokenizer.TT_WORD)
        throw new IOException(s"${t.lineno}: Word expected after '<'.")

      val crsName = t.sval
      t.nextToken

      if (t.ttype != '>')
        throw new IOException(s"${t.lineno}: '>' expected.")

      t.nextToken

      ab.clear

      while (t.ttype != '<') {

        if (t.ttype == '+') t.nextToken
        if (t.ttype != StreamTokenizer.TT_WORD)
          throw new IOException(s"${t.lineno}+: Word expected after '+'")

        val key = t.sval
        t.nextToken

        if (t.ttype == '=') {
          t.nextToken
          val value = t.sval
          t.nextToken
          addParam(ab, key, if (value != null) Some(value) else None)
        } else addParam(ab, key, None)
      }

      t.nextToken

      if (t.ttype != '>')
        throw new IOException(s"${t.lineno}: '<>' expected")

      t.nextToken

      if (crsName != name) ab.clear
    }

    ab.toArray
  }

  def addParam(ab: ArrayBuffer[String], key: String, value: Option[String]) = {
    val plusKey = if (!key.startsWith("+")) s"+$key" else key

    ab += {
      value match {
        case Some(v) => s"$plusKey=$v"
        case None => plusKey
      }
    }
  }

  def getParameters(crsName: String): Array[String] = {
    val p = crsName.indexOf(":")
    if (p >= 0) {
      val auth = crsName.substring(0, p)
      val id = crsName.substring(p + 1)
      readParametersFromFile(auth, id)
    } else {
      throw new UnsupportedParameterException(s"Invalid CRS name: $crsName")
    }
  }
}
