# 🍑 PeachLib
[![PeachLib](https://api.mcbanners.com/banner/resource/hangar/PeachLib/banner.png?background__template=MALACHITE_GREEN)](https://hangar.papermc.io/PeachBiscuit174/PeachLib)
[![PeachLib](https://img.shields.io/hangar/views/PeachLib?link=https%3A%2F%2Fhangar.papermc.io%2FPeachBiscuit174%2FPeachLib&style=flat)](https://hangar.papermc.io/PeachBiscuit174/PeachLib)
[![Servers](https://img.shields.io/bstats/servers/29074?style=flat&color=blue)](https://bstats.org/plugin/bukkit/PeachLib/29074) [![Players](https://img.shields.io/bstats/players/29074?style=flat&color=blue)](https://bstats.org/plugin/bukkit/PeachLib/29074)
[![CodeFactor](https://www.codefactor.io/repository/github/peachbiscuit174/peachlib/badge)](https://www.codefactor.io/repository/github/peachbiscuit174/peachlib)

A PaperMC Library.

> [!IMPORTANT]
> **Project Status:** This is a **hobby project**. Updates and support depend on availability and motivation. There is no guarantee for immediate bug fixes, but feedback and interaction are expressly encouraged!
> 
> 📝 **Documentation Updated:** June 2026 *(Covers features up to `v1.0.0-SNAPSHOT26`).*
> *Please note: As this is a solo project, new features might sometimes exist in the codebase before they are fully documented here. Feel free to explore the API!*

---

### 📌 Compatibility
| Feature | Status               |
| :--- |:---------------------|
| **Supported MC Version** | **1.21.4+** |
| **Platform** | PaperMC and forks    |
| **Java Version** | 21+                  |

*Note: Only the version listed above is officially supported by the latest library version. Other versions might work but are not actively tested.*

---

## 🚀 Installation

Add the library to your project via **JitPack**.
[![](https://jitpack.io/v/PeachBiscuit174/PeachLib.svg)](https://jitpack.io/#PeachBiscuit174/PeachLib)

### Maven (`pom.xml`)
```xml
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>

    <dependency>
        <groupId>com.github.PeachBiscuit174</groupId>
        <artifactId>PeachLib</artifactId>
        <version>v1.0.0-SNAPSHOT27</version>
        <scope>provided</scope>
    </dependency>
```

> [!IMPORTANT]
> To ensure the library loads correctly, you must also add it as a dependency in your `plugin.yml` or `paper-plugin.yml`.

---

## ✨ Key Features & API

PeachLib offers a wide range of managers and APIs accessible via the central `PeachLibAPI` class:

- **💾 Data Manager:** Powerful multi-backend storage (MySQL, SQLite, YAML, FileTree) featuring asynchronous task queuing, smart caching, and built-in crash recovery.
- **🌍 Language Manager:** Effortless plugin-specific localization with automatic caching.
- **📦 Items Manager:** Build items with full MiniMessage support, generate Base64 player heads, and manage PersistentDataContainer tags effortlessly.
- **🪟 GUI Manager:** Create modern `InventoryGUI` and multi-page `PaginatedGUI` instances with visual layout mapping and auto-item protection.
- **📁 File Manager:** Asynchronous file handling, safe atomic writes, and secure Zip extractions (Zip-Bomb/Zip-Slip protection).
- **⏱️ Scheduler Manager:** A high-performance, real-time `LibraryScheduler` utilizing separate ThreadPools to prevent server lag.
- **👤 Player Manager:** Smart utilities for inventory management and MiniMessage-based display names.

**Built-in Systems & Admin Tools:**
- **Reload Protection:** Safely blocks the `/reload` command to prevent severe plugin corruption.
- **Smart Update Checker:** Automatically checks for GitHub releases, strictly respecting snapshot settings and API version compatibility.
- **Holiday Greetings:** Automatically greets players on holidays (New Year, Halloween, Christmas, etc.) with customizable messages.


**Example Usage:**
```java
// Central API access point
ItemsManager items = PeachLibAPI.getItemsManager();

// Create a custom head via Base64
ItemStack head = items.getCustomHeadsAPI().getCustomHead("eyJ0ZXh0dXJlcyI6...", "<gold>Special Head");

// Open a simple GUI
InventoryGUI gui = PeachLibAPI.getGUIManager().getInventoryGUIAPI().createGUI(3, "<red>Settings");
gui.setButton(13, new GUIButton(items.getNewItemBuilderAPI().builder(Material.DIAMOND), "my_action", event -> {
    event.getWhoClicked().sendMessage("Clicked!");
}));
gui.open(player);
```

---

## 💡 Features & Support

Even though this is a hobby project, your opinion matters!

* **Feature Requests:** Have an idea for a new tool? Feel free to open an issue with the `enhancement` label. I'll take a look when I find the time!
* **Bug Reports:** If something isn't working, please report it via [GitHub Issues](https://github.com/PeachBiscuit174/PeachLib/issues).
* **Contributions:** [Pull Requests](https://github.com/PeachBiscuit174/PeachLib/pulls) are welcome at any time.

---

## 🔄 Updates

* **Update Checker:** The library checks for new versions every 12 hours. Server administrators (OPs) are gently notified upon joining if an update is available.

---

## ⚖️ Legal & Privacy

**Disclaimer:** PeachLib is **not** an official Minecraft product. It is not approved by or associated with Mojang or Microsoft. Minecraft is a trademark of Mojang Synergies AB.

**Privacy Notice & Third-Party Services:** This library uses the following third-party services and bundled libraries to improve developer and administrator experience. **Network requests can be disabled in the plugin's configuration:**

- **bStats:** Collects anonymous technical data (e.g., server version, Java version) to provide usage statistics. Data is anonymized and compliant with GDPR.
- **GitHub:** The built-in Update Checker connects to `api.github.com` and GitHub Releases to check for and download new versions safely. This transmits the server's IP address to GitHub. *(Can be disabled via Config or in-game GUI)*.
- **NTP / Google (Time Synchronization):** The `TimeProvider` synchronizes time to ensure highest accuracy for database entries. It connects to `pool.ntp.org` (via UDP) and falls back to `google.com` (HTTP HEAD request). This transmits the server's IP address. *(Can be completely disabled via the `sync_time_for_database` config option)*.

**Bundled & External Dependencies:**

To provide out-of-the-box functionality, PeachLib utilizes the following third-party libraries. By using PeachLib, you also comply with their respective licenses:

- **bStats:** Collects anonymous technical data. *(Shaded and relocated within the library to prevent conflicts with other plugins).*
- **HikariCP** ([Apache 2.0](https://github.com/brettwooldridge/HikariCP/blob/HEAD/LICENSE)), **SQLite-JDBC** ([Apache 2.0](https://github.com/xerial/sqlite-jdbc/blob/HEAD/LICENSE)), and **MySQL Connector/J** ([GPLv2 with FOSS Exception](https://github.com/mysql/mysql-connector-j/blob/HEAD/LICENSE)): These database drivers and connection pools are **not** bundled inside the JAR. They are automatically and safely downloaded from Maven Central at runtime using the native Spigot/Paper libraries system.

---

## 📄 License & Transparency

License: This project is licensed under the MIT License – see the [LICENSE file](https://github.com/PeachBiscuit174/PeachLib/blob/master/LICENSE) for details.

AI Disclosure: In compliance with transparency best practices (and the EU AI Act guidelines), please be aware that parts of this library's code and documentation have been developed with the assistance of Artificial Intelligence (AI). The AI-generated content has been reviewed and refined by the human author; however, extensive functional testing has not been performed. Use this library at your own risk.

---

## ❤️ Credits & Contributions

We are grateful for any help to make **PeachLib** better! Whether it's a new feature, a bug report, or a great idea – every contribution is welcome.

### 💡 Idea Contributors
*Special thanks to those who helped shape the library with their suggestions.*
| Contributor | Reference |
| :--- | :--- |
| *None yet* | - |

### 🛠️ Code Contributors
*People who improved the codebase via Pull Requests.*
| Contributor | PR ID |
| :--- | :--- |
| *None yet* | - |

### 🐛 Bug Hunters
*Thanks for helping us find and squash bugs!*
| Reporter | Issue ID |
| :--- | :--- |
| *None yet* | - |

---
**Want to help?** Feel free to open an Issue or a Pull Request!

<br>

---
---

<br>

# 🍑 PeachLib (Deutsch)
[![PeachLib](https://api.mcbanners.com/banner/resource/hangar/PeachLib/banner.png?background__template=MALACHITE_GREEN)](https://hangar.papermc.io/PeachBiscuit174/PeachLib)
[![PeachLib](https://img.shields.io/hangar/views/PeachLib?link=https%3A%2F%2Fhangar.papermc.io%2FPeachBiscuit174%2FPeachLib&style=flat)](https://hangar.papermc.io/PeachBiscuit174/PeachLib)
[![Servers](https://img.shields.io/bstats/servers/29074?style=flat&color=blue)](https://bstats.org/plugin/bukkit/PeachLib/29074) [![Players](https://img.shields.io/bstats/players/29074?style=flat&color=blue)](https://bstats.org/plugin/bukkit/PeachLib/29074)
[![CodeFactor](https://www.codefactor.io/repository/github/peachbiscuit174/peachlib/badge)](https://www.codefactor.io/repository/github/peachbiscuit174/peachlib)

Eine PaperMC-Library.

> [!IMPORTANT]
> **Projekt-Status:** Dies ist ein **Freizeitprojekt**. Updates und Support erfolgen nach zeitlicher Verfügbarkeit und Lust. Es besteht kein Anspruch auf sofortige Fehlerbehebung, jedoch sind Feedback und Interaktion ausdrücklich erwünscht!
> 
> 📝 **Dokumentation aktualisiert:** Juni 2026 *(Beinhaltet Features bis `v1.0.0-SNAPSHOT26`).*
> *Hinweis: Da dies ein Ein-Mann-Projekt ist, kann es vorkommen, dass einige neuere Features bereits im Code existieren, bevor sie hier vollständig dokumentiert werden. Erkunde die API gerne auf eigene Faust!*

---

### 📌 Kompatibilität
| Feature | Status                  |
| :--- |:------------------------|
| **Unterstützte MC-Version** | **1.21.4+**    |
| **Plattform** | PaperMC und forks davon |
| **Java Version** | 21+                     |

*Hinweis: Es wird offiziell immer nur die oben genannte Version von der aktuellsten Library unterstützt. Andere Versionen können funktionieren, werden aber nicht aktiv getestet.*

---

## 🚀 Installation

Füge die Library über **JitPack** zu deinem Projekt hinzu.
[![](https://jitpack.io/v/PeachBiscuit174/PeachLib.svg)](https://jitpack.io/#PeachBiscuit174/PeachLib)

### Maven (`pom.xml`)
```xml
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>

    <dependency>
        <groupId>com.github.PeachBiscuit174</groupId>
        <artifactId>PeachLib</artifactId>
        <version>v1.0.0-SNAPSHOT27</version>
        <scope>provided</scope>
    </dependency>
```

> [!IMPORTANT]
> Damit die Library korrekt geladen wird, füge sie auch als Dependency hinzu in deiner `plugin.yml` oder `paper-plugin.yml`.

---

## ✨ Hauptfunktionen & API

PeachLib bietet eine Vielzahl an Managern und APIs, die zentral über die `PeachLibAPI` Klasse erreichbar sind:

- **💾 Data Manager:** Leistungsstarker Multi-Backend-Speicher (MySQL, SQLite, YAML, FileTree) mit asynchroner Queue, intelligentem Caching und Crash-Recovery.
- **🌍 Language Manager:** Mühelose, plugin-spezifische Lokalisierung mit automatischem Instanzen-Caching.
- **📦 Items Manager:** Erstelle Items mit MiniMessage-Support, generiere Base64-Spielerköpfe und verwalte PersistentData-Tags ganz einfach.
- **🪟 GUI Manager:** Erstelle moderne `InventoryGUI` und mehrseitige `PaginatedGUI` Instanzen mit visuellem Layout-Mapping und automatischem Item-Schutz.
- **📁 File Manager:** Asynchrones Datei-Handling, sicheres atomares Speichern und Zip-Extrahierung mit integriertem Schutz vor Zip-Bombing.
- **⏱️ Scheduler Manager:** Hochleistungsfähige ThreadPools zur sofortigen Entlastung des Main-Threads und Vermeidung von Server-Lags.
- **👤 Player Manager:** Smarte Utilities für das Inventar-Management und MiniMessage-Displaynamen.

**Integrierte Systeme & Admin-Tools:**
- **Ingame Config-GUI:** Server-Admins können Netzwerkfunktionen (wie Auto-Updates und NTP-Sync) ganz bequem direkt im Spiel steuern.
- **Reload-Schutz:** Blockiert den `/reload`-Befehl und erzwingt einen Neustart, um schwerwiegende Plugin-Korruption zu verhindern.
- **Smart Update Checker:** Sucht automatisch nach GitHub-Releases und berücksichtigt dabei intelligent Snapshot-Regeln und API-Versionskompatibilität.
- **Feiertagsgrüße:** Automatische, anpassbare Begrüßungen für Spieler an Feiertagen (Weihnachten, Halloween etc.).

**Beispiel:**
```java
// Zentraler API-Zugriff
ItemsManager items = PeachLibAPI.getItemsManager();

// Custom Head via Base64 erstellen
ItemStack head = items.getCustomHeadsAPI().getCustomHead("eyJ0ZXh0dXJlcyI6...", "<gold>Special Head");

// Ein simples GUI öffnen
InventoryGUI gui = PeachLibAPI.getGUIManager().getInventoryGUIAPI().createGUI(3, "<red>Einstellungen");
gui.setButton(13, new GUIButton(items.getNewItemBuilderAPI().builder(Material.DIAMOND), "my_action", event -> {
    event.getWhoClicked().sendMessage("Geklickt!");
}));
gui.open(player);
```

---

## 💡 Features & Support

Obwohl dies ein Freizeitprojekt ist, ist deine Meinung wichtig!

* **Feature-Wünsche:** Du hast eine Idee für ein neues Tool? Erstelle gerne ein Issue mit dem Label `enhancement`. Ich schaue es mir an, sobald ich Zeit finde!
* **Bug Reports:** Falls etwas nicht funktioniert, melde es bitte über die [GitHub Issues](https://github.com/PeachBiscuit174/PeachLib/issues).
* **Beiträge:** [Pull Requests](https://github.com/PeachBiscuit174/PeachLib/pulls) sind jederzeit willkommen.

---

## 🔄 Updates

* **Update-Checker:** Die Library prüft alle 12 Stunden auf neue Versionen. Server-Administratoren (OP) werden beim Joinen dezent benachrichtigt, falls ein Update verfügbar ist.

---

## ⚖️ Rechtliches & Datenschutz (Deutsch)

**Haftungsausschluss:** PeachLib ist **kein** offizielles Minecraft-Produkt. Es ist nicht von Mojang oder Microsoft genehmigt und steht nicht mit ihnen in Verbindung. Minecraft ist eine Marke von Mojang Synergies AB.

**Datenschutzhinweis & Drittanbieter:** Diese Library nutzt folgende Dienste und gebündelte Bibliotheken von Drittanbietern, um die Erfahrung für Entwickler und Administratoren zu verbessern. **Netzwerkanfragen können in der Konfiguration deaktiviert werden:**

- **bStats:** Sammelt anonyme technische Daten (z. B. Serverversion, Java-Version), um Nutzungsstatistiken zu erstellen. Die Daten werden anonymisiert und gemäß DSGVO verarbeitet.
- **GitHub:** Der integrierte Update-Checker verbindet sich mit `api.github.com` und den GitHub Releases, um sicher nach neuen Versionen zu suchen und diese herunterzuladen. Dabei wird technisch bedingt die IP-Adresse des Servers an GitHub übertragen. *(Kann via Config oder Ingame-GUI deaktiviert werden)*.
- **NTP / Google (Zeitsynchronisation):** Der `TimeProvider` synchronisiert die Zeit, um höchste Genauigkeit für Datenbankeinträge zu gewährleisten. Er verbindet sich mit `pool.ntp.org` (via UDP) und weicht auf `google.com` (HTTP HEAD) aus. Dabei wird die IP-Adresse des Servers übertragen. *(Kann über die Config-Option `sync_time_for_database` vollständig deaktiviert werden)*.

**Integrierte & externe Abhängigkeiten:**

Um eine sofort einsatzbereite Funktionalität zu bieten, nutzt PeachLib die folgenden Drittanbieter-Bibliotheken. Durch die Nutzung von PeachLib gelten auch deren jeweilige Lizenzen:

- **bStats:** Sammelt anonyme technische Daten. *(Innerhalb der Library integriert und relokalisiert, um Konflikte mit anderen Plugins zu vermeiden).*
- **HikariCP** ([Apache 2.0](https://github.com/brettwooldridge/HikariCP/blob/HEAD/LICENSE)), **SQLite-JDBC** ([Apache 2.0](https://github.com/xerial/sqlite-jdbc/blob/HEAD/LICENSE)) und **MySQL Connector/J** ([GPLv2 mit FOSS Exception](https://github.com/mysql/mysql-connector-j/blob/HEAD/LICENSE)): Diese Datenbanktreiber und Connection-Pools werden **nicht** mitkompiliert. Sie werden beim Serverstart automatisch und sicher über das native Spigot/Paper-Library-System von Maven Central nachgeladen.

---

## 📄 Lizenz & Transparenz

Dieses Projekt ist unter der MIT-Lizenz lizenziert – siehe die [LICENSE Datei](https://github.com/PeachBiscuit174/PeachLib/blob/master/LICENSE) für Details.

KI-Offenlegung: Zur Einhaltung von Transparenzstandards (und im Hinblick auf den EU AI Act) weisen wir darauf hin, dass Teile des Codes und der Dokumentation mit Unterstützung von Künstlicher Intelligenz (KI) erstellt wurden. Die Inhalte wurden vom menschlichen Autor gesichtet und überarbeitet, jedoch nicht vollumfänglich auf Funktionalität getestet. Die Nutzung erfolgt auf eigene Gefahr.

---

## ❤️ Credits & Mitwirkende

Wir sind dankbar für jede Hilfe, die dazu beiträgt, die **PeachLib** zu verbessern! Egal ob es eine neue Funktion, ein Fehlerbericht oder eine kreative Idee ist – jeder Beitrag ist willkommen.

### 💡 Ideen & Vorschläge
*Ein besonderer Dank geht an alle, die die Library durch ihre kreativen Ansätze mitgestalten.*
| Mitwirkende | Referenz |
| :--- | :--- |
| *Noch keine Einträge* | - |

### 🛠️ Code-Beiträge
*Entwickler, die den Code direkt über Pull Requests verbessert haben.*
| Mitwirkende | PR-ID |
| :--- | :--- |
| *Noch keine Einträge* | - |

### 🐛 Bug-Jäger
*Vielen Dank an alle, die uns helfen, Fehler zu finden und zu beheben!*
| Reporter | Issue-ID |
| :--- | :--- |
| *Noch keine Einträge* | - |

---
**Möchtest du helfen?** Du kannst jederzeit gerne ein Issue eröffnen oder einen Pull Request erstellen!
