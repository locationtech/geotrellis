package geotrellis.spark

class EmptyBoundsError(message: String = "")
    extends Exception(s"""Empty bounds error ${(if (message.nonEmpty) ": " + message else message)}""")
