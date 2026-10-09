# Changelog – drivechronik-dash



## [0.4.2] – 2026-10-09

### Geändert

- Kreisverkehrsanzeige in der Live-Navigation wiederhergestellt.
- OSRM-Ausfahrtnummern werden nun übernommen und angezeigt.
- OSRM-Kreisverkehrsmanöver verwenden wieder eine eindeutige Ausfahrtsgeometrie.
- Die echte Kreisverkehrsgrafik mit passendem Ausfahrtspfeil funktioniert wieder.
- Kreisverkehrslogik bleibt kompatibel mit OSRM online, Valhalla online und Valhalla offline.
- Fahranweisungs-Popup schmaler und höher gestaltet.
- Navigations-Popup rechts auf rund 30 % der Bildschirmbreite begrenzt.
- Die darunterliegende Navigationskarte bleibt dadurch deutlich besser sichtbar.

## [0.4.1] – 2026-10-09

### Geändert

- Online-Routing wieder als primärer Routingweg hergestellt.
- „Autobahn vermeiden“ nutzt jetzt Online-Valhalla mit `use_highways = 0.0`.
- Für „Autobahn vermeiden“ sind keine Offline-Routingdaten mehr erforderlich.
- OSRM bleibt für normales Online-Routing und als Fallback erhalten.
- Offline-Valhalla bleibt optionaler Fallback, falls installiert.
- Fehlerhafte wiederholte Neuberechnungen im Stand behoben.
- GPS-Fallback bei manueller Routenneuberechnung verbessert.
- Wetterposition wird während der Navigation zuverlässiger aktualisiert.
- Open-Meteo-URL korrigiert.
- Startprüfung für Offline-Routing korrigiert:
  fehlende Routingdateien werden nicht mehr fälschlich als verfügbar markiert.
- Deutschland-Legacy-Fallback für bestehende Offline-Dateien robuster gemacht.
- Veraltete OSRM-Alternativenlogik für Autobahnvermeidung entfernt.
- Diagnose-Logging mit vollständigen Routing-Koordinaten entfernt.

## [0.4.0] – 2026-10-09

### Offline-Karten und Routing
- Länderunabhängige Offline-Paketarchitektur
- Unterstützung mehrerer Länderpakete
- Deutschland und Dänemark als getrennte Karten- und Routingpakete
- Automatische Erkennung installierter Offline-Pakete
- Länderweise Aktivierung von Karte und Routing
- Downloadbare Karten-, Routing- und Ladeinfrastruktur-Pakete
- GitHub-basierter Paketkatalog
- Update-Erkennung für Offline-Daten
- Fortschrittsanzeige und verbesserte Paketverwaltung

### Navigation
- Online-Routing bleibt primärer Routingweg
- Offline-Valhalla als Fallback
- Überarbeitete Navigations- und Kamera-Logik
- Verbesserte Simulation und Routenführung
- Große Abbiegeeinblendung bei 500 m
- Zweite Abbiegeeinblendung bei 125 m
- Einblenddauer jeweils 3 Sekunden
- Verbesserte Kreisverkehrdarstellung
- Stabileres automatisches Rerouting

### Karten und Ladeinfrastruktur
- Offline-Karten auf PMTiles-Basis
- Separate Ladeinfrastruktur-Ebene
- Antippbare Ladestationen
- Verbesserte Offline-Paketstatusanzeige
- Automatisierte Erstellung von Dänemark-Karten und Valhalla-Routing


## [0.3.0] – 2026-10-04

### Erweiterte Navigation – Kreisverkehre (2026-10-06)
- Große, animierte Abbiegehinweise rechts vom Tacho
- Große Abbiegehinweise bei 500 m und 125 m
- Strukturierte Fahranweisungen mit kräftigen Richtungspfeilen
- Offline-OSM-Kreisverkehrdaten für Deutschland
- Automatische Zuordnung von Einfahrt und Ausfahrt zur Valhalla-Route
- Darstellung tatsächlicher Kreisverkehranschlüsse
- Hervorgehobener Fahrweg zur richtigen Ausfahrt
- Fallback bei nicht eindeutiger Zuordnung


### Navigation und Sprachdialog (2026-10-06)
- Sprachbefehle zum Starten und Stoppen
- Gesprochene Zielbestätigung mit Ja/Nein
- Interne Android-Spracherkennung ohne Popup
- 10-Sekunden-Antwortzeit
- Eigener Aktivierungs-Doppelklang
- Postleitzahlen als einzelne Ziffern
- Optimierter Zoom für die Routenübersicht
- Stabilitätstests auf Pixel 8 Pro



### Navigation und Sprache (2026-10-05)
- Offline-Routing und Abbiegeführung
- Automatisches Rerouting
- Straßenbasierter GPS-Simulator
- GPS-Rückkehr nach Simulationsende
- Dynamischer Zoom und Kameraführung
- Navigationspfeil mit Tesla-Symbol
- Beenden-Taste für aktive Navigation
- Gesprochene Zieleingabe
- Aktivierungswort DriveChronik mit Vosk
- Mikrofonsteuerung und Symboloptimierung
- Vosk 0.3.75 mit 16-KB-Kompatibilität



### Dashboard und Tacho
- Plastischer 3D-Tacho mit Tiefeneffekt
- Restenergie als linker Tachokranz
- Stufenloser Restenergie-Farbverlauf
- Leistungsring im rechten Tachokranz
- Orangefarbener POWER-Verlauf
- Blauer REKUP-Verlauf
- Gebogene Beschriftungen im Tachokranz
- Entfernung der bisherigen Leistungs-KPI
- Optimiertes Navigationslayout

### Demo und Diagnose
- Demo startet mit 100 % Restenergie
- Nach 5 Sekunden animierter Übergang
- Lokale Telemetrie-Diagnosefunktionen
- Diagnose-Steuerung in der App
- Service-Modus-Steuerelemente
- Erweiterte Energieanzeige

### Bekannte Einschränkungen
- Live-Leistungswerte noch nicht verifiziert
- Weitere BLE-Diagnosearbeiten erforderlich

## [0.2.0] – 2026-10-03

### Dashboard
- Navigationstelemetrie integriert
- Automatische Tag-/Nachtumschaltung
- Dynamische Ganganzeige
- Umgang mit veralteten Fahrdaten
- Versionsanzeige in der App
- Verbesserte Nachtfarben

### Daten und Release
- Trennung von Demo- und Live-Daten
- Keine automatische Demo-Rückfallanzeige
- Signierte Release-Builds
- Eigenes DriveChronik-Dash-Appsymbol
- Lokale Release-Artefakte ausgeschlossen

## [0.1.0] – 2026-10-03

### Erste Entwicklungsfassung
- Native Android-App im Querformat
- Digitaler Tacho und Fahrsimulation
- Tesla-inspiriertes Dashboard
- Lokaler Bluetooth-Scan
- BLE-GATT-Verbindung
- Tesla-Protobuf-Integration
- RoutableMessage-Verarbeitung
- Verschlüsselte Tesla-Kommunikation
- Session- und Schlüsselverwaltung
- DriveState, ChargeState und LocationState
- Lokale Fahrzeug-Telemetrie
- VIN-Zuordnung
- Geführte BLE-Einrichtung
- Tesla-Schlüsselregistrierung
