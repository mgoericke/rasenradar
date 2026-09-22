# Spec 6 — Pokal und Nations League anzeigen

> Rein fachliche Spec. Keine Technologie-, Schnittstellen- oder Datentyp-Festlegungen.
> Diese trifft der Scaffold-/Implementierungsschritt.

## Ziel

Neben den beiden Bundesligen und der Champions-League-Ligaphase zeigt das System
zwei weitere Wettbewerbe: den **DFB-Pokal** und die **Nations League**. Beide dienen
allein der Ansicht — Begegnungen, Ergebnisse und Statistik. Es entsteht keine
KI-Vorschau zu ihren Begegnungen.

Der Schwerpunkt liegt auf Statistik, nicht auf Prognose. Was den Pokal fachlich
ausmacht — dass eine klassentiefere Mannschaft eine höherklassige wirft, dass eine
Begegnung erst im Elfmeterschießen fällt, wie weit eine Mannschaft gekommen ist —,
soll sichtbar sein.

Beide Wettbewerbe sind anders aufgebaut als eine Liga: der Pokal kennt keine Tabelle,
sondern Runden bis zum Endspiel; die Nations League spielt mehrere Gruppen
nebeneinander, jede mit eigener Tabelle. Das System braucht dafür einen Begriff von
der **Form eines Wettbewerbs**, statt jeden Wettbewerb einzeln zum Sonderfall zu
machen.

## Akteure

- **Betrachter** — sieht Begegnungen, Ergebnisse und Statistik beider Wettbewerbe
- **Spieltagsdatenpflege (systemintern)** — lädt beide Wettbewerbe aus derselben
  äußeren Quelle und nach denselben Regeln wie die Ligen (Spec 1)

## Ablauf

1. Jeder Wettbewerb trägt eine Form: **Tabellenwettbewerb** (die beiden Bundesligen,
   die Champions-League-Ligaphase), **Gruppenwettbewerb** (Nations League) oder
   **K.-o.-Wettbewerb** (DFB-Pokal).
2. Die Spieltagsdatenpflege lädt die Begegnungen beider neuen Wettbewerbe wie die der
   Ligen: dieselbe Quelle, dieselbe Änderungsprüfung, derselbe Übergang von vorläufig
   zu endgültig (Spec 1).
3. Im K.-o.-Wettbewerb bildet jede **Runde** einen Abschnitt und trägt ihren eigenen
   Namen (erste Runde bis Endspiel). Eine Tabelle entsteht nicht.
4. Im Gruppenwettbewerb führt jede **Gruppe** ihre eigene Tabelle und ihren eigenen
   Spieltagszähler. Die Gruppen laufen zeitlich nebeneinander.
5. Der Betrachter ruft je Wettbewerb eine Übersicht auf: beim Pokal die Begegnungen
   der Runden, bei der Nations League die Tabellen aller Gruppen samt den Begegnungen
   des aktuellen Spieltags.
6. Der Betrachter ruft zu jeder beteiligten Mannschaft eine Mannschaftsseite auf, die
   deren Weg durch den Wettbewerb, Bilanz und Torschützen zeigt. Im K.-o.-Wettbewerb ist
   dieser Weg eine Kette von Runde zu Runde, die dort endet, wo die Mannschaft
   ausgeschieden ist.
7. Im K.-o.-Wettbewerb weist das System zu jeder Begegnung die Spielklasse beider
   Mannschaften aus und hebt die **Überraschungen** einer Ausgabe hervor: Begegnungen,
   die eine klassentiefere Mannschaft gewonnen hat.

## Regeln

- Die Form eines Wettbewerbs entscheidet, ob eine Tabelle geführt wird und wie ein
  Abschnitt benannt ist. Neue Wettbewerbe ordnen sich einer der drei Formen zu,
  statt eigene Ausnahmen zu erhalten.
- Ein K.-o.-Wettbewerb führt weder Tabelle noch Tabellenposition. Seine Abschnitte
  werden mit dem Namen der Runde bezeichnet, nicht als Spieltag gezählt.
- In einem Gruppenwettbewerb gilt jede Regel, die bisher je Liga galt, je Gruppe:
  eigene Tabelle, eigener Spieltagszähler, eigene Tabellenposition.
- Liefert die Quelle für einen Gruppenwettbewerb keine Spieltagsnummer, leitet das
  System sie aus der zeitlichen Abfolge innerhalb der Gruppe ab: Begegnungen
  derselben Gruppe am selben Kalendertag bilden einen Spieltag, die Reihenfolge der
  Tage ergibt die Zählung. Diese Ableitung ist Sache des Systems und als solche
  nachvollziehbar; sie greift nur, wo die Quelle nichts hergibt.
- Mannschaften, die ausschließlich in diesen Wettbewerben auftreten — Amateurvereine
  im Pokal, Nationalmannschaften — sind vollwertige Mannschaften. Ihre
  Mannschaftsseite zeigt dieselbe Statistik wie die eines Bundesligavereins, auch bei
  nur einer einzigen Begegnung.
- Eine Mannschaftsseite zeigt in jedem Wettbewerb Spielplan, Bilanz und Torschützen.
  Tabellenposition, Tabellenzone und Positionsverlauf zeigt sie nur dort, wo der
  Wettbewerb eine Tabelle führt.
- Statistik einer Mannschaft wird je Wettbewerb ausgewiesen und nicht über
  Wettbewerbe hinweg zu einer Gesamtbilanz verrechnet. Eine Mannschaft, die in
  mehreren Wettbewerben derselben Saison spielt, ist von jedem ihrer Wettbewerbe aus
  erreichbar.
- Eine Begegnung eines K.-o.-Wettbewerbs kann nach Verlängerung oder im
  Elfmeterschießen entschieden werden. Maßgeblich für den Sieger ist das
  Elfmeterschießen, sonst die Verlängerung, sonst der Stand nach 90 Minuten. Angezeigt
  wird der Stand nach 90 Minuten zusammen mit der Art der Entscheidung.
- Die äußere Quelle weist zusätzlich einen zusammenfassenden Endstand aus, dessen
  Bedeutung von Ausgabe zu Ausgabe wechselt — mal der Stand nach 90 Minuten, mal nach
  Verlängerung, mal der des Elfmeterschießens. Er wird nicht übernommen; das System
  liest die eigens ausgewiesenen Stände.
- Treffer aus einem Elfmeterschießen sind keine Tore der Begegnung. Sie zählen weder in
  den Endstand noch in eine Torschützenliste noch in eine Bilanz.
- Jede Mannschaft eines K.-o.-Wettbewerbs trägt die **Spielklasse**, in der sie in
  dieser Saison spielt: erste, zweite oder dritte Liga, sonst unterklassig. Sie ergibt
  sich aus den Mannschaftslisten dieser Ligen in derselben Saison; wer in keiner davon
  steht, gilt als unterklassig.
- Eine **Überraschung** ist eine Begegnung, die eine klassentiefere Mannschaft gewonnen
  hat. Wie deutlich sie ausfällt, ergibt sich aus dem Abstand der beiden Spielklassen.
- Vorläufige Ergebnisse dürfen angezeigt werden und sind als solche gekennzeichnet,
  wie in Spec 1.
- Zu Begegnungen dieser Wettbewerbe entsteht keine KI-Vorschau, keine Rückschau und
  keine Saisonaussicht.

## Abgrenzung

- Keine KI-Vorschau (Spec 2), keine Rückschau und keine Trefferbilanz (Spec 3) und
  keine Saisonaussicht (Spec 4) für Pokal und Nations League. Spec 4 zählt den Pokal
  bereits heute zur Abgrenzung; das bleibt so.
- Nur die Liga A der Nations League. Die übrigen Ligen des Wettbewerbs und der
  Auf-/Abstieg zwischen ihnen bleiben außen vor.
- Keine weiteren Wettbewerbe mit Nationalmannschaften (Europa- und Weltmeisterschaft,
  deren Qualifikation, Freundschaftsspiele).
- Keine Aussage darüber, welche Mannschaft eine K.-o.-Runde erreichen wird — der
  Pokal wird gezeigt, nicht geschätzt.
- Kein Turnierbaum. Der Wettbewerb wird nach jeder Runde neu ausgelost; ein Baum würde
  Pfade behaupten, die es nicht gibt (siehe „Im Spec-Interview geklärt").
- Keine Spielerstatistik über die Torschützen hinaus, wie in Spec 1.

## Im Spec-Interview geklärt

- **Umfang:** Beide Wettbewerbe dienen allein der Ansicht, wie die
  Champions-League-Ligaphase. Der Schwerpunkt liegt auf Statistik, nicht auf
  Prognose.
- **Gruppen der Nations League:** Die Gruppe ist ein eigenes Merkmal des Spieltags,
  kein eigener Wettbewerb. Der Betrachter sieht eine Wettbewerbsseite mit allen
  Gruppentabellen untereinander — nicht vier getrennte Einträge in der Navigation.
- **Spieltage der Nations League:** Die Quelle nennt nur die Gruppe, keine
  Spieltagsnummer. Sie wird aus dem Kalender abgeleitet (siehe Regeln). Geprüft am
  Spielplan der laufenden Ausgabe: jede Gruppe spielt an sechs Tagen mit je zwei
  Begegnungen, die Abgrenzung ist eindeutig.
- **Mannschaftsseite im Pokal:** ohne Tabellenteil — kein Platz, keine Zone, kein
  Positionsverlauf. Statistik und Weg durch die Runden bleiben.
- **Historie:** Vom Pokal werden vier Ausgaben ab 2023/24 vorgehalten, von der
  Nations League die Ausgaben 2024 und 2026. Grund ist die Quelle: das Kürzel des
  Nationenwettbewerbs wechselt von Ausgabe zu Ausgabe, nur diese beiden liegen unter
  demselben Kürzel vor. Ältere Pokalausgaben liegen nur lückenhaft vor (Stand:
  Prüfung 2026).
- **Saisonaussicht für die Nations League:** bewusst nicht. Gruppen mit vier
  Mannschaften und sechs Begegnungen wären eine eigene fachliche Entscheidung über
  Platzierungsziele; sie wird getrennt getroffen, falls sie gewünscht wird.
- **Kein Turnierbaum:** Geprüft an den Ausgaben 2023/24 bis 2025/26 — von sechzehn
  Paarungen, die ein fester Turnierbaum für die zweite Runde vorhersagen würde, traten
  über drei Ausgaben zusammen zwei ein. Der Wettbewerb wird nach jeder Runde neu
  ausgelost, auch vor Viertel- und Halbfinale. Die fachlich richtige Form ist die Kette
  je Mannschaft, nicht der Baum.
- **Entscheidung einer K.-o.-Begegnung:** Die Quelle weist die Stände nach 90 Minuten,
  nach Verlängerung und nach Elfmeterschießen getrennt aus; daneben einen
  zusammenfassenden Endstand, dessen Bedeutung wechselt (geprüft: 2023/24 trägt er den
  Stand des Elfmeterschießens, 2024/25 den nach Verlängerung, 2025/26 den nach 90
  Minuten). Nur die eigens ausgewiesenen Stände werden gelesen.
- **Elfmeterschützen:** Die Quelle führt die Schützen eines Elfmeterschießens in
  derselben Liste wie die Tore der Begegnung, erkennbar am fehlenden Spielzeitpunkt
  (16 bis 25 Einträge je Ausgabe). Ohne Trennung wäre jede Torschützenliste des
  Wettbewerbs verfälscht.
- **Spielklasse:** Sie wird aus den Mannschaftslisten der ersten, zweiten und dritten
  Liga derselben Saison abgeleitet; damit sind 56 der 64 Teilnehmer einer Ausgabe
  zugeordnet, der Rest gilt als unterklassig (Stichprobe 2025/26 geprüft). Die dritte
  Liga ist dafür nur Vergleichsliste, kein eigener Wettbewerb im System.
- **Saisonbezeichnung der Nations League:** Die Ausgabe reicht ins Folgejahr und wird
  wie eine Saison bezeichnet (Beispiel: 2026/27).

## Abnahmekriterien

- Die Übersicht des Pokals zeigt die Begegnungen einer Runde unter deren Namen
  (Beispiel: „Achtelfinale"), nicht als gezählten Spieltag, und führt keine Tabelle.
- Die Übersicht der Nations League zeigt alle Gruppentabellen und die Begegnungen des
  aktuellen Spieltags.
- Innerhalb einer Gruppe stimmen die abgeleiteten Spieltage mit den Spieltagen des
  Wettbewerbs überein: sechs Spieltage je Gruppe, je zwei Begegnungen.
- Eine Tabelle einer Gruppe enthält ausschließlich Begegnungen dieser Gruppe.
- Ein Amateurverein mit einer einzigen Pokalbegegnung hat eine Mannschaftsseite mit
  Spielplan, Bilanz und Torschützen — ohne Tabellenteil und ohne Fehlerbild.
- Ein Bundesligaverein, der auch im Pokal spielt, ist von seiner Ligaseite aus in
  seinem Pokalwettbewerb erreichbar; die Bilanzen beider Wettbewerbe bleiben
  getrennt.
- Zu einer Begegnung des Pokals oder der Nations League lässt sich keine KI-Vorschau
  auslösen, und es erscheint keine.
- Ein vorläufiges Ergebnis ist in beiden Wettbewerben als vorläufig gekennzeichnet.
- Eine im Elfmeterschießen entschiedene Pokalbegegnung zeigt den Stand nach 90 Minuten
  und die Art der Entscheidung; der Sieger ist die Mannschaft, die das Elfmeterschießen
  gewonnen hat. Geprüft an einer bekannten Begegnung: SV Sandhausen gegen Hannover 96
  (Ausgabe 2023/24) endete 3:3 und wurde im Elfmeterschießen mit 7:5 entschieden.
- Kein Schütze eines Elfmeterschießens erscheint in einer Torschützenliste, und kein
  solcher Treffer verändert eine Bilanz.
- Jede Mannschaft einer Pokalbegegnung ist mit ihrer Spielklasse ausgewiesen.
- Die Übersicht der Überraschungen einer Ausgabe enthält genau die Begegnungen, die
  eine klassentiefere Mannschaft gewonnen hat.
