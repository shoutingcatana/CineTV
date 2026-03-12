{
  description = "CineStream – Desktop & Mobile Streaming Client";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };

        jdk = pkgs.jdk17;

        runtimeLibs = with pkgs; [
          libx11
          libxtst
          libxrender
          libxext
          libxi
          libxrandr
          libxcursor
          libxfixes
          libxscrnsaver
          fontconfig
          freetype
          glib
          gtk3
          cairo
          pango
          gdk-pixbuf
          atk
          libGL
        ];

        runtimeBins = with pkgs; [ mpv git ];

        # Wrapper script that clones/updates the repo and runs via Gradle.
        # Gradle + Maven deps are downloaded at runtime (not in the Nix sandbox).
        cinestream = pkgs.writeShellScriptBin "CineStream" ''
          set -euo pipefail

          CACHE_DIR="''${XDG_CACHE_HOME:-$HOME/.cache}/cinestream"
          REPO_DIR="$CACHE_DIR/repo"

          export JAVA_HOME="${jdk}"
          export PATH="${pkgs.lib.makeBinPath ([ jdk ] ++ runtimeBins)}:$PATH"
          export LD_LIBRARY_PATH="${pkgs.lib.makeLibraryPath runtimeLibs}''${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"

          # Unset snap-related GTK vars that may conflict
          unset GTK_PATH GTK_EXE_PREFIX GIO_MODULE_DIR GSETTINGS_SCHEMA_DIR 2>/dev/null || true

          if [ ! -d "$REPO_DIR/.git" ]; then
            echo "╔══════════════════════════════════════╗"
            echo "║ CineStream – Erster Start            ║"
            echo "║ Lade Repository herunter...           ║"
            echo "╚══════════════════════════════════════╝"
            mkdir -p "$CACHE_DIR"
            ${pkgs.git}/bin/git clone --depth 1 https://github.com/shoutingcatana/CineTV.git "$REPO_DIR"
          else
            echo "Aktualisiere CineStream..."
            (cd "$REPO_DIR" && ${pkgs.git}/bin/git pull --ff-only 2>/dev/null) || true
          fi

          cd "$REPO_DIR"
          chmod +x gradlew
          exec ./gradlew --no-daemon run
        '';
      in
      {
        packages = {
          default = cinestream;
          cinestream = cinestream;
        };

        apps.default = {
          type = "app";
          program = "${cinestream}/bin/CineStream";
        };

        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            jdk
            mpv
            gradle
          ] ++ runtimeLibs;

          shellHook = ''
            export JAVA_HOME="${jdk}"
            export LD_LIBRARY_PATH="${pkgs.lib.makeLibraryPath runtimeLibs}''${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
            echo "CineStream dev shell – Java 17 + mpv + Gradle ready"
          '';
        };
      }
    );
}
