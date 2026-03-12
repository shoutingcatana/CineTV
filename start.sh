#!/bin/bash
# GlycoGuide TV - Start Script
# Startet die App mit der System-JVM (empfohlen)

cd "$(dirname "$0")"

echo "╔══════════════════════════════════════╗"
echo "║         CineStream v1.0.0            ║"
echo "║   Desktop Streaming Client           ║"
echo "╚══════════════════════════════════════╝"
echo ""

# Check for Java
if ! command -v java &> /dev/null; then
    echo "ERROR: Java nicht gefunden. Installiere Java 17+:"
    echo "  sudo apt install openjdk-17-jdk"
    exit 1
fi

# Check for AWT/Swing (non-headless JRE)
if [ ! -f "/usr/lib/jvm/java-17-openjdk-amd64/lib/libawt_xawt.so" ]; then
    echo "WARNUNG: GUI-Bibliotheken fehlen (nur headless JRE installiert)."
    echo "Installiere das vollständige JRE:"
    echo ""
    echo "  sudo apt install openjdk-17-jre"
    echo ""
    read -p "Jetzt installieren? (j/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Jj]$ ]]; then
        sudo apt install -y openjdk-17-jre
    else
        echo "Abgebrochen. Ohne GUI-Bibliotheken kann die App nicht starten."
        exit 1
    fi
fi

# Check for video player
if command -v mpv &> /dev/null; then
    echo "  Video Player: mpv ✓"
elif command -v vlc &> /dev/null; then
    echo "  Video Player: vlc ✓"
else
    echo "  WARNUNG: Kein Video-Player gefunden!"
    echo "  Installiere: sudo apt install mpv"
fi

echo ""
echo "Starte App..."

# Workaround: VS Code Snap setzt GTK_PATH auf Snap-Bibliotheken,
# die mit dem System-glibc inkompatibel sind (libpthread Konflikt).
unset GTK_PATH GTK_EXE_PREFIX GIO_MODULE_DIR GSETTINGS_SCHEMA_DIR

exec ./gradlew run
