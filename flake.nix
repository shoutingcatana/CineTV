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

        cinestream = pkgs.stdenv.mkDerivation {
          pname = "cinestream";
          version = "1.0.0";

          src = ./.;

          nativeBuildInputs = with pkgs; [
            jdk
            makeWrapper
          ];

          buildInputs = with pkgs; [
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

          # Gradle needs a writable home
          GRADLE_USER_HOME = "/tmp/gradle-home";

          buildPhase = ''
            export HOME="$TMPDIR"
            export GRADLE_USER_HOME="$TMPDIR/gradle-home"
            mkdir -p "$GRADLE_USER_HOME"

            # Use system Java
            export JAVA_HOME="${jdk}"

            # Build the distribution archive (creates a tar.gz with libs + start script)
            chmod +x gradlew
            ./gradlew --no-daemon createDistributable
          '';

          installPhase = ''
            mkdir -p $out

            # Copy the distributable
            cp -r build/compose/binaries/main/app/CineStream/* $out/

            # Wrap the launcher to set up the environment
            wrapProgram $out/bin/CineStream \
              --set JAVA_HOME "${jdk}" \
              --prefix LD_LIBRARY_PATH : "${pkgs.lib.makeLibraryPath [
                pkgs.libx11
                pkgs.libxtst
                pkgs.libxrender
                pkgs.libxext
                pkgs.libxi
                pkgs.libxrandr
                pkgs.libxcursor
                pkgs.libxfixes
                pkgs.libxscrnsaver
                pkgs.fontconfig
                pkgs.freetype
                pkgs.glib
                pkgs.gtk3
                pkgs.cairo
                pkgs.pango
                pkgs.gdk-pixbuf
                pkgs.atk
                pkgs.libGL
              ]}" \
              --prefix PATH : "${pkgs.lib.makeBinPath [ pkgs.mpv ]}"
          '';

          meta = with pkgs.lib; {
            description = "CineStream – Desktop Streaming Client mit BitTorrent-Support";
            license = licenses.mit;
            platforms = platforms.linux;
            mainProgram = "CineStream";
          };
        };
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
          ];

          shellHook = ''
            export JAVA_HOME="${jdk}"
            echo "CineStream dev shell – Java 17 + mpv + Gradle ready"
          '';
        };
      }
    );
}
