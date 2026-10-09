# drivechronik-dash

Lokales Android-Dashboard für Tesla-Fahrzeuge.

**Aktuelle Version: 0.4.0**

## Projektbeschreibung

drivechronik-dash ist ein natives Android-Dashboard
mit Bluetooth-Anbindung an Tesla-Fahrzeuge.

Die Anwendung stellt Fahrzeuginformationen in
einem übersichtlichen digitalen Cockpit dar.

## Dashboard

- Plastischer digitaler 3D-Tacho
- Geschwindigkeitsanzeige
- Dynamische Ganganzeige
- Restenergie mit Farbverlauf
- Leistungsring mit POWER und REKUP
- Reichweitenanzeige
- Navigationsinformationen
- Automatischer Tag-/Nachtmodus

## Tesla-Bluetooth

- Lokaler BLE-Scan
- Fahrzeugidentifikation über VIN
- Geführte Einrichtung
- Tesla-Schlüsselregistrierung
- Verschlüsselte Kommunikation
- Tesla-Session-Verwaltung
- Verarbeitung von Fahrzeugdaten

## Offline-Navigation

- Offline-Routing mit Valhalla
- Kartenansicht mit MapLibre
- GPS über Smartphone oder Tesla-BLE
- Abbiegehinweise und Reststrecke
- Automatisches Rerouting
- Geschwindigkeitsabhängiger Zoom
- Ruhige, vorausschauende Kameraführung
- Navigationspfeil mit Tesla-Symbol
- Simulation mit straßenbasierter Fehlfahrt
- Navigation jederzeit beenden

Für die Offline-Routenberechnung werden
lokale Valhalla-Kartendaten benötigt.
Die Kartenanzeige kann Internet erfordern.

## Sprachsteuerung

Aktivierungswort: **DriveChronik**

- Lokale Aktivierungswort-Erkennung mit Vosk
- Deutsches Sprachmodell
- Mikrofon über Symbol ein-/ausschaltbar
- Grün bedeutet: Sprachsteuerung aktiviert
- Zieleingabe per Android-Spracherkennung
- Zielkontrolle vor Navigationsstart
- Fadenkreuz öffnet die manuelle Zielsuche

Die Aktivierung erfolgt nur bei geöffneter App.
Die Android-Spracherkennung für Adressen
kann eine Internetverbindung benötigen.

Das Vosk-Modell wird nicht in Git gespeichert.
Zur Einrichtung siehe das Installationsskript
`scripts/install-voice-model.sh`.

## Erweiterte Sprachbedienung

- Navigation per Sprache starten und stoppen
- Interne Spracheingabe ohne Google-Popup
- Eigener Doppelklang zur Aktivierung
- Gesprochene Zielbestätigung
- Ja/Nein ohne erneutes Aktivierungswort
- Antwortzeit auf 10 Sekunden begrenzt
- Postleitzahlen werden einzeln ausgesprochen
- Android-Sprachdienst kann online arbeiten
- Systemseitiger Signalton kann zusätzlich ertönen

## Demo-Modus

Die App enthält einen Simulationsmodus.

Die Restenergie beginnt bei 100 %.
Nach fünf Sekunden erfolgt ein sanfter
Übergang zum simulierten Akkustand.

Demo-Daten werden nicht automatisch
als echte Fahrzeugdaten angezeigt.

## Diagnose

- Lokale Telemetrie-Diagnose
- Diagnose-Steuerung in der App
- Service-Modus-Steuerelemente

Die Diagnosefunktionen befinden sich
noch in der Weiterentwicklung.

## Bekannte Einschränkungen

Einige Bluetooth-Telemetriewerte sind
noch nicht abschließend verifiziert.

Insbesondere die Live-Leistung wird
im Normalbetrieb noch neutral angezeigt.

## Build

Voraussetzungen:
- Android SDK
- Gradle-Projektumgebung
- Passende Java-Installation
- Konfigurierte Release-Signierung

Release erstellen:

    ./gradlew :app:assembleRelease

APK installieren:

    adb install -r app/build/outputs/apk/release/app-release.apk

## Versionen

- 0.1.0 – Basis-Dashboard und BLE-Grundlagen
- 0.2.0 – Navigation und signiertes Release
- 0.3.0 – 3D-Tacho und Leistungsanzeige
- 0.4.0 – Mehrländer-Offlinedaten, Dänemark, Ladeinfrastruktur und verbesserte Navigation

Weitere Details enthält CHANGELOG.md.

## Entwicklungsstatus

Das Projekt befindet sich in aktiver Entwicklung.
Nicht alle Telemetriewerte sind bereits für den
produktiven Live-Betrieb freigegeben.
