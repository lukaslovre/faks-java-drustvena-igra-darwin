# INDIVIDUALNI PROJEKTNI ZADATAK
## Programiranje u programskom jeziku Java
*Tehničko veleučilište u Zagrebu | Akademska godina 2025./2026.*

---

### Opis projektnog zadatka
Svaki student implementira simulaciju jedne od najpopularnijih suvremenih multiplayer board igara prema aktualnoj BoardGameGeek rang-listi (2025./2026.). Igra mora biti implementirana korištenjem tehnologije Java 25 i JavaFX uz odgovarajuće biblioteke.

### Bodovanje i kriteriji ocjenjivanja
*   **Ishod 1 — Game Engine i JavaFX GUI:** 30 bodova
*   **Ishod 2 — Serijalizacija i Reflection API:** 10 bodova
*   **Ishod 3 — Mrežna komunikacija (TCP/UDP, RMI, JNDI):** 40 bodova
*   **Ishod 4 — Niti, sinkronizacija i asinkroni JavaFX:** 10 bodova
*   **Ishod 5 — XML (SAX/DOM) i Replay sustav:** 10 bodova

---

*   50 bodova — ocjena dovoljan (2)
*   62 boda — ocjena dobar (3)
*   75 bodova — ocjena vrlo dobar (4)
*   87 bodova — ocjena izvrstan (5)

### Ostali kriteriji za prihvaćanje rješenja
*   Nijedna klasa ne smije imati više od 200 linija koda.
*   Potrebno je ispraviti sve probleme koje prijavljuje statički analizator koda SonarQube.
*   Aplikacija mora biti pokrenuta i demonstrirana u mrežnom okruženju s minimalno dva računala.
*   Sav kôd mora biti verzioniran u Git repozitoriju s redovitim commitovima koji dokumentiraju napredak.

---

...

### 22. Darwin's Journey
Godina: 2023. | Broj igrača: 1–4

**Opis igre i provjera dostupnosti rješenja:** Darwin's Journey worker-placement/progression igra je za 1–4 igrača koja prati Darwinovo putovanje Galapagos otocima. Igrači postavljaju radnike na akcijska mjesta istražujući otoke, sakupljajući uzorke, šaljući pisma Europi i usavršavajući znanje (3 Research trackove: Botany, Zoology, Geology). Novost: radnici napreduju razinom (1–3) — na višim razinama mogu pristupiti boljim akcijama na istom mjestu. Bodovi dolaze od uzoraka, pisama, istraživanja i ekspedicija. Nema Java/JavaFX implementacije.

**Ishod 1 — Game Engine i JavaFX grafičko sučelje (30 bodova)**
Implementirati klase Radnik (ime, razina 1–3), AkcijskoMjesto (min_razina, nagrade), Otok (verzija akcijskog mjesta s unikatnim bonusima za svaki od 5 Galapagos otoka), ResearchTrack (3 tipa, razine 1–5 s nagradama) i DarwinsJourneyEngine koji validira radničku razinu za akciju, koordinira ekspedicije (skup otoka) i boduje prikupljene uzorke. JavaFX sučelje prikazuje arhipelag Galapagosa s 5 otoka, personal Board s Research trackovima i tablicu radnika.

**Ishod 2 — Serijalizacija i Reflection API (10 bodova)**
Serijalizacijom pohraniti stanje istraživanja (razine trackova, prikupljeni uzorci, poslana pisma). Reflection API-jem generirati dokumentaciju ResearchTrack razina s nagradama i blokiranim akcijama.

**Ishod 3 — Mrežna komunikacija, RMI i JNDI (40 bodova)**
TCP server prima worker placement akcije s razinom radnika, validira i emitira. JNDI konfigurira server. RMI implementirati za globalni 'Darwin Archive' (koji otoci su istraženi, koliko uzoraka skupljeno). Chat za igrače.

**Ishod 4 — Niti, sinkronizacija i asinkroni JavaFX (10 bodova)**
Animacija napretka radnika (razina raste: token raste i mijenja boju) i otkrića novog otoka moraju biti asinkroni. Research Track animacija (marker klizi po putu) implementirati kao Timeline.

**Ishod 5 — XML (SAX/DOM) i Replay sustav (10 bodova)**
SAX parser bilježi ekspedicije, uzorke, pisma i razine istraživanja po rundi u XML. DOM replay rekonstruira Darwinovo putovanje. XSD shema definira otočnu strukturu i Research Track napredak.