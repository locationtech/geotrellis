import AssemblyKeys._

seq(Revolver.settings: _*)

assemblySettings

addCompilerPlugin("com.azavea.math.plugin" %% "optimized-numeric" % "0.1")

mergeStrategy in assembly <<= (mergeStrategy in assembly) {
  (old) => {
    case "application.conf" => MergeStrategy.concat
    case _ => MergeStrategy.first
  }
}
