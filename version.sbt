enablePlugins(GitVersioning)

git.baseVersion := "2.22.2"

git.useGitDescribe := true

val VersionRegex = "v([0-9]+.[0-9]+.[0-9]+)-?(.*)?".r
git.gitTagToVersionNumber := {
  case VersionRegex(v,"") => Some(v)
  case VersionRegex(v,"SNAPSHOT") => Some(s"$v-SNAPSHOT")  
  case VersionRegex(v,s) => Some(s"$v-$s-SNAPSHOT")
  case _ => None
}
