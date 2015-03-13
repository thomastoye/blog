lazy val root = (project in file(".")).dependsOn(assemblyPlugin)

lazy val assemblyPlugin = uri("git://github.com/arnolddevos/Soapbox")
