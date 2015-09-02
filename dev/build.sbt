import Dependencies._

libraryDependencies += sigar
Keys.fork in run := true
fork := true
javaOptions in run += "-Djava.library.path=./sigar"