import AssemblyKeys._

//seq(Revolver.settings: _*)

assemblySettings

mergeStrategy in assembly <<= (mergeStrategy in assembly) {
  (old) => {
    case "application.conf" => MergeStrategy.concat
    case _ => MergeStrategy.first
  }
}
