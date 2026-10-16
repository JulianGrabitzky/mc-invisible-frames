{
  description = "Development environment for the Invisible Frames NeoForge 1.21.1 backport";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs = { nixpkgs, ... }:
    let
      systems = [
        "x86_64-linux"
        "aarch64-linux"
        "aarch64-darwin"
      ];
    in
    {
      devShells = nixpkgs.lib.genAttrs systems (system:
        let
          pkgs = import nixpkgs { inherit system; };
        in
        {
          default = pkgs.mkShellNoCC {
            packages = with pkgs; [
              jdk21
              git
              curl
              unzip
              zip
            ];

            JAVA_HOME = "${pkgs.jdk21}";
          };
        });
    };
}
