{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  packages = [
    pkgs.git
    pkgs.graalvm-ce
    pkgs.maven
    pkgs.zsh
  ];

  shellHook = ''
    export JAVA_HOME="${pkgs.graalvm-ce}"
    echo "Setting JAVA_HOME to $JAVA_HOME"

    export MAVEN_HOME="${pkgs.maven}"
    echo "Setting MAVEN_HOME to $MAVEN_HOME"
  '' ;
}
