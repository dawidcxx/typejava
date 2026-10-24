{
  description = "TypeJava development environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs =
    {
      self,
      nixpkgs,
      flake-utils,
    }:
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = import nixpkgs { inherit system; };
      in
      {
        devShells.default = pkgs.mkShell {
          JAVA_HOME = "${pkgs.jdk21}";
          JAVA_VERSION = "21";

          buildInputs = with pkgs; [
            git
            jdk21
            gradle_9
          ];
        };
      }
    );
}
