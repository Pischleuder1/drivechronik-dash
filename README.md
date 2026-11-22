# drivechronik-dash

Lokales Android-Dashboard und Navigationssystem für Tesla-Fahrzeuge.

**Aktuelle Version: 0.6.7**

## Projektbeschreibung

drivechronik-dash ist ein natives Android-Dashboard
mit lokaler Bluetooth-Anbindung an Tesla-Fahrzeuge.

Die Anwendung kombiniert Fahrzeugdaten, digitales Cockpit,
Navigation, Online- und Offline-Karten, Ladeinfrastruktur
sowie Verkehrsinformationen in einer gemeinsamen Oberfläche.

Der Schwerpunkt liegt auf einer möglichst lokalen Verarbeitung
der Fahrzeug- und Navigationsdaten.

## Dashboard

Ab Version 0.6.0 stehen drei Dashboard-Layouts zur Verfügung.
Die Auswahl erfolgt über das Service-Menü und wird
dauerhaft gespeichert.

### CLASSIC

Das ursprüngliche Dashboard mit:

- Plastischem digitalem 3D-Tacho
- Dynamischem POWER-/REKUP-Ring
- Restenergieanzeige mit fließendem Farbverlauf
- Navigationskarte und halbtransparenter KPI-Spalte

### TOURING

Für eine kartenorientierte Darstellung:

- Kompakter Digitaltacho
- Fahrstufenanzeige
- Fahrspuransicht mit Tesla-Symbol
- Vergrößerte Kartenfläche
- Verbreiterte vertikale KPI-Spalte ohne Scrollen
- Kombinierte Anzeige für Akkustand und Reichweite
- Kompakte Ziel-, Fahrzeit- und Ankunftsinformationen

### TECHNIK

Digitales Cockpit mit:

- Großer digitaler Geschwindigkeitsanzeige
- Fahrstufenanzeige
- Gemeinsamem POWER-/REKUP-Balken
- Batterie- und Restenergieanzeige
- Integrierter Navigationskarte
- Rundem Tempolimitschild auf der Karte

### DEMO und LIVE

Im DEMO-Modus werden simulierte Fahrzeugwerte dargestellt.

Im LIVE-Modus werden vorhandene Tesla-Telemetriedaten genutzt.
Noch nicht verifizierte Leistungs-, Batterie- und
Reichweitenwerte erscheinen neutral als Strich statt
als vermeintlich echte Messwerte.

Die Tesla-BLE-Anbindung befindet sich weiterhin in der
fahrzeugseitigen Verifizierung.

### Allgemeine Dashboard-Funktionen

- Plastischer digitaler 3D-Tacho
- Geschwindigkeitsanzeige
- Dynamische Ganganzeige
- Restenergie mit fließendem Farbverlauf
- Leistungsring mit POWER und REKUP
- Reichweitenanzeige
- Navigationsinformationen
- Automatischer Tag-/Nachtmodus
- Halbtransparenter rechter KPI-Bereich
- Kartenansicht bis unter den KPI-Bereich
- Anzeigen für Blinker und Fahrzeugbeleuchtung

## Screenshots

![DriveChronik](docs/screenshots/screen1.png)

![DriveChronik](docs/screenshots/screen2.png)

![DriveChronik](docs/screenshots/screen3.png)

![DriveChronik](docs/screenshots/screen4.png)

![DriveChronik](docs/screenshots/screen5.png)

![DriveChronik](docs/screenshots/screen6.png)

## Tesla-Bluetooth

- Lokaler BLE-Scan
- Fahrzeugidentifikation über VIN
- Geführte Einrichtung
- Tesla-Schlüsselregistrierung
- Verschlüsselte Kommunikation
- Tesla-Session-Verwaltung
- Verarbeitung von Fahrzeugdaten
- Smartphone-GPS und Tesla-BLE als Positionsquellen

Die privaten Fahrzeugschlüssel werden über den
Android KeyStore verwaltet und nicht im Git-Repository gespeichert.

### Tesla-BLE-Livedaten ab v0.6.7

- Live-Geschwindigkeit für alle drei Cockpit-Layouts
- Leistungsanzeige für POWER und Rekuperation
- Live-Akkustand und Reichweite
- Tesla-Navigationsinformationen einschließlich Ankunftsprognose
- Fahrzeugbeleuchtung und Warnblinker
- Automatische BLE-Wiederverbindung nach Verbindungsabbruch
- Zentraler BLE-Abfragetakt von 250 ms
- Keine parallelen Fahrzeuganfragen
- Verschlüsselte Tesla-Kommunikation

Die tatsächliche Aktualisierungsgeschwindigkeit hängt von
der Antwortzeit des Fahrzeugs und der BLE-Verbindung ab.

Die automatische Wiederverbindung und der schnellere
Polling-Takt müssen noch abschließend im Fahrbetrieb
überprüft werden.

## Navigation

- Online-Routing
- Offline-Routing mit Valhalla
- Kartenansicht mit MapLibre
- Smartphone-GPS oder Tesla-BLE
- Abbiegehinweise und Reststrecke
- Dauerhafte Abbiegehinweise ab 125 m
- 1 Sekunde Nachlauf nach erkanntem Manöverende
- Sanfte Ausblendanimation über 2 Sekunden
- Automatisches Rerouting bei Fehlfahrten
- Fahrtrichtungsabhängige Kameraführung
- Geschwindigkeitsabhängiger Kartenzoom
- Navigationspfeil mit Tesla-Symbol
- Kreisverkehrsanzeige mit realer Ausfahrtsgeometrie
- Vermeidung von Autobahnen
- Navigation jederzeit beenden
- Navigationssimulation für Tests

## Online- und Offline-Karten

drivechronik-dash unterstützt sowohl Online-Karten
als auch lokal installierte Offline-Karten.

Offline-Pakete können länderweise verwaltet werden.

Aktuell werden unter anderem unterstützt:

- Deutschland
- Dänemark

Die Karten- und Routing-Pakete werden separat über
das Projekt **drivechronik-maps** bereitgestellt.

Ein Offline-Paket kann enthalten:

- MapLibre-Kartendaten
- Valhalla-Routingdaten
- Ladeinfrastruktur

Damit können Kartenanzeige und Routenberechnung
auch ohne permanente Internetverbindung genutzt werden,
sofern die benötigten Pakete lokal installiert sind.

## Ladeinfrastruktur

- Lokale Ladepunkte aus den Kartenpaketen
- Ladepunkte auch über der Online-Karte darstellbar
- Tesla Supercharger
- Schnellladestationen
- Normale Ladepunkte
- Unterschiedliche Anzeige abhängig von der Zoomstufe
- Ladepunkte auf der Karte antippbar
- Navigation zu ausgewählten Ladestationen

### Ladestations-Popup ab v0.6.3

In allen Cockpit-Layouts wird für Ladestationen
eine gemeinsame, kompakte Informationsansicht verwendet.

Angezeigt werden, soweit verfügbar:

- Stationsbezeichnung
- Postleitzahl, Ort, Straße und Hausnummer
- Anzahl der Ladepunkte und maximale Ladeleistung
- Anschlussarten und Steckertypen
- Schaltfläche „Dorthin navigieren“

Die separate Betreiber-Informationszeile entfällt.
Der Navigationsbutton bleibt unterhalb der
scrollbaren Stationsinformationen sichtbar.

Die Ladeinfrastruktur ist unabhängig davon nutzbar,
ob die Online- oder Offline-Basiskarte angezeigt wird.

## Verkehrsinformationen

Ab Version 0.5.0 können aktuelle Verkehrsinformationen
für deutsche Autobahnen eingeblendet werden.

Unterstützt werden unter anderem:

- Baustellen
- Warnmeldungen
- Straßensperrungen

Die Daten werden routenbezogen ausgewertet und
als Hinweis während der Navigation angezeigt.

Für Live-Verkehrsinformationen ist eine
Internetverbindung erforderlich.

Eine automatische Routenneuberechnung ausschließlich
aufgrund einer Verkehrsmeldung ist derzeit nicht aktiviert.

## Sprachsteuerung

Aktivierungswort: **DriveChronik**

- Lokale Aktivierungswort-Erkennung mit Vosk
- Deutsches Sprachmodell
- Mikrofon über Symbol ein-/ausschaltbar
- Zieleingabe per Sprache
- Zielkontrolle vor Navigationsstart
- Fadenkreuz für manuelle Zielsuche

Die Aktivierungswort-Erkennung erfolgt lokal.

Für die eigentliche Android-Spracherkennung
kann abhängig vom verwendeten Sprachdienst
eine Internetverbindung erforderlich sein.

Das Vosk-Modell wird nicht in Git gespeichert.

Zur Einrichtung:

    scripts/install-voice-model.sh

## Erweiterte Sprachbedienung

- Navigation per Sprache starten und stoppen
- Interne Spracheingabe ohne zusätzliches Google-Popup
- Eigener Doppelklang zur Aktivierung
- Gesprochene Zielbestätigung
- Ja/Nein-Antwort ohne erneutes Aktivierungswort
- Begrenztes Antwortzeitfenster
- Angepasste Aussprache von Postleitzahlen

## Demo- und Simulationsmodus

Die App enthält einen Simulationsmodus zur
Entwicklung und Prüfung der Navigation.

Unter anderem können getestet werden:

- Geschwindigkeit
- Energieverbrauch
- Streckenführung
- Fehlfahrten
- Rerouting
- Navigationshinweise
- Kreisverkehre

Die Restenergie beginnt im Demo-Modus zunächst
für fünf Sekunden bei 100 % und wechselt danach
sanft auf den simulierten Akkustand.

Demo-Daten werden nicht als echte
Fahrzeugdaten behandelt.

## Diagnose

- Lokale Telemetrie-Diagnose
- Diagnose-Steuerung in der App
- Service-Modus-Steuerelemente
- BLE-Diagnose
- Navigations- und GPS-Diagnose

Die Diagnosefunktionen dienen überwiegend
der Entwicklung und Fehlersuche.

## Kartenpakete

Die großen Offline-Daten werden bewusst
nicht direkt in diesem Repository gespeichert.

Karten, Routingdaten und Ladeinfrastruktur:

**Pischleuder1/drivechronik-maps**

Dadurch bleibt die Android-Anwendung unabhängig
von den jeweiligen Länder- und Kartenversionen.

## Bekannte Einschränkungen

Einige Tesla-Bluetooth-Telemetriewerte befinden sich
weiterhin in der Erprobung.

Insbesondere einzelne Live-Telemetriewerte sind noch
nicht für jeden Fahrzeugzustand abschließend verifiziert.

Live-Verkehrsinformationen benötigen Internetzugang.

Eine automatische Umleitung aufgrund von Baustellen
oder Straßensperrungen ist derzeit noch nicht aktiviert.

## Build

Voraussetzungen:

- Android SDK
- Gradle-Projektumgebung
- Passende Java-Installation
- Konfigurierte Release-Signierung

Release erstellen:

    ./gradlew clean :app:assembleRelease

APK installieren:

    adb install -r app/build/outputs/apk/release/app-release.apk

Die Installation mit `adb install -r` aktualisiert
eine vorhandene Installation, ohne deren App-Daten
automatisch zu löschen.

## Versionen

- **0.6.7** – Automatische Tesla-BLE-Wiederverbindung,
  schnellerer zentraler Tacho-Abfragetakt für alle Cockpits,
  Live-Power und Rekuperation, Tesla-Navigationsdaten,
  überarbeitetes TOURING-Cockpit
- **0.6.5** – Erweiterungen der Tesla-BLE-Anbindung
  und Fahrzeugtelemetrie


- **0.6.3** – Kompaktes Ladestations-Popup für alle Cockpits, übersichtliche Standort- und Anschlussdaten sowie dauerhaft sichtbare Navigation
- **0.6.2** – Verbesserungen am Touring-Dashboard und der Navigation

- **0.6.1** – Hotfix: Absturz im CLASSIC-Dashboard bei fehlenden LIVE-Energiewerten behoben
- **0.6.0** – Drei Dashboard-Layouts CLASSIC, TOURING und TECHNIK sowie verbesserte DEMO-/LIVE-Trennung
- **0.5.9** – Funktionstest des integrierten Updates
- **0.5.8** – GitHub-Updatefunktion, verbesserte Navigationseinstellungen und bereinigte Sprachsignalisierung

- **0.2.0** – Navigation und signiertes Release
- **0.3.0** – 3D-Tacho und Leistungsanzeige
- **0.4.0** – Mehrländer-Offlinedaten, Ladeinfrastruktur und erweiterte Navigation
- **0.4.1** – Online-Autobahnvermeidung und Routing-Verbesserungen
- **0.4.2** – Verbesserte Kreisverkehrs- und Abbiegeführung
- **0.5.0** – Live-Verkehrsinformationen für Autobahnen
- **0.5.1** – Ladepunkte auf Online-Karten und überarbeitetes Karten-/KPI-Layout
- **0.5.4** – Verkehrs-Replay sowie Blinker- und Lichtanzeigen direkt im Tacho
- **0.5.6** – Automatischer Etappenwechsel bei importierten Routen, durchgehende Simulation und zuverlässige Zielankunft
- **0.5.5** – DriveChronik-Routenimport mit mehreren Lade-/Zwischenzielen, abschnittsweiser Navigation und Gesamtplan-Anzeige

Weitere technische Details und Änderungen enthält:

    CHANGELOG.md

## Entwicklungsstatus

Das Projekt befindet sich in aktiver Entwicklung.

Navigation, Online-/Offline-Karten, Ladeinfrastruktur
und das Dashboard werden laufend weiterentwickelt.

Nicht alle Tesla-Telemetriewerte sind bereits für
jeden Fahrzeugzustand abschließend verifiziert.
