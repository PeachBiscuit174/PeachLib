# 🍑 PeachLib
[![PeachLib](https://api.mcbanners.com/banner/resource/hangar/PeachLib/banner.png?background__template=MALACHITE_GREEN)](https://hangar.papermc.io/PeachBiscuit174/PeachLib)
[![PeachLib](https://img.shields.io/hangar/views/PeachLib?link=https%3A%2F%2Fhangar.papermc.io%2FPeachBiscuit174%2FPeachLib&style=flat)](https://hangar.papermc.io/PeachBiscuit174/PeachLib)
[![Servers](https://img.shields.io/bstats/servers/29074?style=flat&color=blue)](https://bstats.org/plugin/bukkit/PeachLib/29074) [![Players](https://img.shields.io/bstats/players/29074?style=flat&color=blue)](https://bstats.org/plugin/bukkit/PeachLib/29074)

A PaperMC Library.

> [!IMPORTANT]
> **Project Status:** This is a **hobby project**. Updates and support depend on availability and motivation. There is no guarantee for immediate bug fixes, but feedback and interaction are expressly encouraged!

---

### 📌 Compatibility
| Feature | Status            |
| :--- |:------------------|
| **Supported MC Version** | **1.21.10**       |
| **Platform** | PaperMC and forks |
| **Java Version** | 21+               |

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
            <url>[https://jitpack.io](https://jitpack.io)</url>
        </repository>
    </repositories>

    <dependency>
        <groupId>com.github.PeachBiscuit174</groupId>
        <artifactId>PeachLib</artifactId>
        <version>v1.0.0-SNAPSHOT16-MC1.21.10</version>
    </dependency>
```

> [!IMPORTANT]
> To ensure the library loads correctly, you must also add it as a dependency in your `plugin.yml` or `paper-plugin.yml`.

---

## 🛠 API Usage

Usage is centralized via the `API` class.

**Example:**
```java
// Creates an ItemStack of a head via Base64 string
ItemStack head = API.getItemsManager().getCustomHeadsAPI().getCustomHead("eyJ0ZXh0dXJlcyI6...");
```
or
```java
CustomHeadsAPI customHeadsAPI = API.getItemsManager().getCustomHeadsAPI();
ItemStack head = customHeadsAPI.getCustomHead("eyJ0ZXh0dXJlcyI6...");
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

## ⚖️ Legal & Privacy (English)

**Disclaimer:** PeachLib is **not** an official Minecraft product. It is not approved by or associated with Mojang or Microsoft. Minecraft is a trademark of Mojang Synergies AB.

**Privacy Notice:** This library uses the following third-party services to improve developer and administrator experience:

- **bStats:** Collects anonymous technical data (e.g., server version, Java version) to provide usage statistics. Data is anonymized and compliant with GDPR.

- **GitHub/JitPack:** The built-in Update Checker connects to GitHub's APIs to check for new versions. This involves transmitting the server's IP address to GitHub/JitPack during the request.


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

Eine PaperMC-Library.

> [!IMPORTANT]
> **Projekt-Status:** Dies ist ein **Freizeitprojekt**. Updates und Support erfolgen nach zeitlicher Verfügbarkeit und Lust. Es besteht kein Anspruch auf sofortige Fehlerbehebung, jedoch sind Feedback und Interaktion ausdrücklich erwünscht!

---

### 📌 Kompatibilität
| Feature | Status                  |
| :--- |:------------------------|
| **Unterstützte MC-Version** | **1.21.11**             |
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
            <url>[https://jitpack.io](https://jitpack.io)</url>
        </repository>
    </repositories>

    <dependency>
        <groupId>com.github.PeachBiscuit174</groupId>
        <artifactId>PeachLib</artifactId>
        <version>v1.0.0-SNAPSHOT16-MC1.21.10</version>
    </dependency>
```

> [!IMPORTANT]
> Damit die Library korrekt geladen wird, füge sie auch als Dependency hinzu in deiner `plugin.yml` oder `paper-plugin.yml`.

---

## 🛠 API Nutzung

Die Nutzung erfolgt zentral über die Klasse `API`.

**Beispiel:**
```java
// Erstellt einen ItemStack eines Kopfes mithilfe eines Base64 String
ItemStack head = API.getItemsManager().getCustomHeadsAPI().getCustomHead("eyJ0ZXh0dXJlcyI6...");
```
oder
```java
CustomHeadsAPI customHeadsAPI = API.getItemsManager().getCustomHeadsAPI();
ItemStack head = customHeadsAPI.getCustomHead("eyJ0ZXh0dXJlcyI6...");
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

**Datenschutzhinweis:** Diese Library nutzt folgende Dienste von Drittanbietern, um die Erfahrung für Entwickler und Administratoren zu verbessern:

- **bStats:** Sammelt anonyme technische Daten (z. B. Serverversion, Java-Version), um Nutzungsstatistiken zu erstellen. Die Daten werden anonymisiert und gemäß DSGVO verarbeitet.

- **GitHub/JitPack:** Der integrierte Update-Checker verbindet sich mit den APIs von GitHub, um nach neuen Versionen zu suchen. Dabei wird technisch bedingt die IP-Adresse des Servers an GitHub/JitPack übertragen.


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
