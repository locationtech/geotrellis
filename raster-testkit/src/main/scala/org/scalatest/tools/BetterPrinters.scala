/*
 * Copyright 2019 Azavea
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

package org.scalatest.tools

object BetterPrinters {
  def printText(text: String, color: AnsiColor, presentInColor: Boolean): Unit =
    print(Fragment(text, color).toPossiblyColoredText(presentInColor))

  def printAnsiGreen(text: String): Unit = printText(text, AnsiGreen, true)
  def printAnsiRed(text: String): Unit = printText(text, AnsiRed, true)
  def printAnsiYellow(text: String): Unit = printText(text, AnsiYellow, true)
  def printAnsiCyan(text: String): Unit = printText(text, AnsiCyan, true)
}
