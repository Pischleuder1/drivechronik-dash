# Changelog – drivechronik-dash

## [0.5.5] – 2026-10-10

### Hinzugefügt

- Import kompletter Navigationspläne aus DriveChronik über einen Android-Deep-Link.
- Importierte Reisepläne können mehrere Lade- und Zwischenziele sowie ein Endziel enthalten.
- Automatische abschnittsweise Navigation durch alle importierten Zwischenziele bis zum Endziel.
- Ziel-SOC importierter Ladestopps wird im Übernahme-Dialog angezeigt.
- Kompletter importierter Reiseplan wird zusätzlich als rote gestrichelte Linie auf der Karte dargestellt.
- Alle Zwischenziele und das Endziel werden als rote Punkte auf der Gesamtplan-Linie markiert.

### Verbessert

- Import-Dialog verbreitert und für längere Reisepläne scrollbar gemacht.
- Alle Teilziele und das endgültige Reiseziel werden vor der Übernahme vollständig angezeigt.
- Aktiver Navigationsabschnitt bleibt unabhängig von der Gesamtplanung als blaue Route sichtbar.
- Manueller Navigationsabbruch beendet auch einen aktiven importierten DriveChronik-Reiseplan.



## [0.5.4] – 2026-10-10

### Hinzugefügt

- Zwei getrennte Verkehrseinstellungen:
  - automatische Verkehrswarnungen ein-/ausschalten
  - Verkehrsmarker auf der Karte ein-/ausschalten
- Verkehrseinstellungen werden dauerhaft gespeichert.
- Direkter Geschwindigkeitsumschalter während der Navigation-Simulation.
- Deutliche Kennzeichnung „Simulation“ im Tachobereich.

### Verbessert

- Gegenfahrbahnfilter für Autobahn-Verkehrsmeldungen erweitert.
- API-Geometrien der Verkehrsmeldungen werden für die Richtungsprüfung ausgewertet.
- Zusätzlicher Richtungsvergleich über Routenfortschritt und Fahrtrichtungswinkel.
- Mehrere Baustellenmeldungen auf derselben Autobahn innerhalb von 3 km entlang der Route werden zu einem Hinweis zusammengefasst.
- Simulationsanzeige aus der Kartenfläche entfernt und oberhalb des Tachos platziert.

## [0.5.3] – 2026-10-10

### Hinzugefügt

- Verkehrsmeldungen werden dauerhaft als typabhängige Marker direkt auf der Navigationskarte angezeigt.
- Verkehrssymbole können angetippt werden und öffnen ein zentriertes Detail-Popup über dem gesamten Dashboard.
- Verkehrswarnungen auf der aktiven Route erscheinen automatisch ab etwa 2 km Entfernung und schließen nach 6 Sekunden.
- Automatische Verkehrswarnungen werden pro Ereignis nur einmal eingeblendet.
- Fahrtrichtungsfilter für Autobahn-Verkehrsmeldungen ergänzt, damit eindeutig erkannte Gegenfahrbahn-Ereignisse ausgeblendet werden.
- Simulationsgeschwindigkeiten um 180 km/h und 240 km/h erweitert.

### Geändert

- Restzeit und Ankunftszeit der Navigation berücksichtigen in der Simulation die aktuell gewählte Simulationsgeschwindigkeit.
- Kartenzoom für hohe Simulationsgeschwindigkeiten erweitert.
- Live-Navigation verwendet ab 120 km/h den weiten Autobahn-Zoom 10.0.
- Manuell geöffnete Verkehrspopups bleiben unabhängig vom 6-Sekunden-Autotimer geöffnet.

## [0.5.2] – 2026-10-10

### Navigation und Verkehr
- Manueller Replay-Button für vorhandene Verkehrsmeldungen ergänzt.
- Verkehrs-Hinweis kann nach dem automatischen Ausblenden erneut für 6 Sekunden angezeigt werden.
- Verkehrsbutton rechts neben der Routenübersicht angeordnet und an der Mikrofon-Achse ausgerichtet.
- Online-Karte berücksichtigt den Nachtmodus jetzt zuverlässig.
- Navigationsziel im rechten Bereich kann zweizeilig dargestellt werden.

### Tacho und Fahrzeugstatus
- Blinkeranzeigen direkt in den Tacho integriert.
- Blinker auf Höhe des Drehpunkts der Tachonadel positioniert.
- Abblendlicht und Fernlicht direkt neben der digitalen Geschwindigkeitsanzeige integriert.
- Licht- und Blinkeranzeigen kompakter und näher zur Tachomitte ausgerichtet.
- Inaktive Blinker- und Lichtanzeigen werden vollständig ausgeblendet.
- Separate Fahrzeugstatusleiste im rechten KPI-Bereich entfernt.
- Bestehende Tesla-artige Farben beibehalten:
  Blinker grün, Abblendlicht grün, Fernlicht blau.




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
