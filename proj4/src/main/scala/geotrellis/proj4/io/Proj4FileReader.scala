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

import collection.mutable.ArrayBuffer

class Proj4FileReader {

  def readParametersFromFile(authorityCode: String, name: String): Array[String] = {

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

  private def readFile(reader: BufferedReader, name: String): Option[Array[String]] = {
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

    if (ab.isEmpty) None
    else Some(ab.toArray)
  }

  def addParam(ab: ArrayBuffer, key: String, value: Option[String]) = {
    val plusKey = if (!key.startsWith("+")) s"+$key" else key

    ab += value match {
      case Some(v) => s"$plusKey=$v"
      case None => plusKey
    }
  }

  def getParameters(crsName: String): Option[Array[String]] = {
    try {
      val p = crsName.indexOf(":")
      if (p >= 0) {
        val auth = crsName.substring(0, p)
        val id = crsName.substring(p + 1)
        Some(readParametersFromFile(auth, id))
      } else None
    } catch {
      case ioe: IOException => {
        e.printStackTrace
        None
      }
    }
  }

}
