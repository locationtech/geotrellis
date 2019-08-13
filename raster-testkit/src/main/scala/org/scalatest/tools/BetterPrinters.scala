package org.scalatest.tools

object BetterPrinters {
  def printText(text: String, color: AnsiColor, presentInColor: Boolean): Unit =
    print(Fragment(text, color).toPossiblyColoredText(presentInColor))

  def printAnsiGreen(text: String): Unit = printText(text, AnsiGreen, true)
  def printAnsiRed(text: String): Unit = printText(text, AnsiRed, true)
  def printAnsiYellow(text: String): Unit = printText(text, AnsiYellow, true)
  def printAnsiCyan(text: String): Unit = printText(text, AnsiCyan, true)
}
