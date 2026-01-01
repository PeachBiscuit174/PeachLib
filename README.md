# 🍑 PeachPaperLib

A PaperMC Library.

> [!IMPORTANT]
> **Project Status:** This is a **hobby project**. Updates and support depend on availability and motivation. There is no guarantee for immediate bug fixes, but feedback and interaction are expressly encouraged!

---

### 📌 Compatibility
| Feature | Status            |
| :--- |:------------------|
| **Supported MC Version** | **1.21.11**       |
| **Platform** | PaperMC and forks |
| **Java Version** | 21+               |

*Note: Only the version listed above is officially supported by the latest library version. Other versions might work but are not actively tested.*

---

## 🚀 Installation

Add the library to your project via **JitPack**.
[![](https://jitpack.io/v/PeachBiscuit174/PeachPaperLib.svg)](https://jitpack.io/#PeachBiscuit174/PeachPaperLib)

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
        <artifactId>PeachPaperLib</artifactId>
        <version>v1.0.0-SNAPSHOT5</version>
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
* **Bug Reports:** If something isn't working, please report it via [GitHub Issues](https://github.com/PeachBiscuit174/PeachPaperLib/issues).
* **Contributions:** [Pull Requests](https://github.com/PeachBiscuit174/PeachPaperLib/pulls) are welcome at any time.

---

## 🔄 Updates

* **Update Checker:** The library checks for new versions every 12 hours. Server administrators (OPs) are gently notified upon joining if an update is available.

---

## 📄 License

This project is licensed under the MIT License – see the [LICENSE file](https://github.com/PeachBiscuit174/PeachPaperLib/blob/master/LICENSE) for details.

---

## ❤️ Credits & Contributions

We are grateful for any help to make **PeachPaperLib** better! Whether it's a new feature, a bug report, or a great idea – every contribution is welcome.

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

# 🍑 PeachPaperLib (Deutsch)

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
[![](https://jitpack.io/v/PeachBiscuit174/PeachPaperLib.svg)](https://jitpack.io/#PeachBiscuit174/PeachPaperLib)

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
        <artifactId>PeachPaperLib</artifactId>
        <version>v1.0.0-SNAPSHOT5</version>
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
* **Bug Reports:** Falls etwas nicht funktioniert, melde es bitte über die [GitHub Issues](https://github.com/PeachBiscuit174/PeachPaperLib/issues).
* **Beiträge:** [Pull Requests](https://github.com/PeachBiscuit174/PeachPaperLib/pulls) sind jederzeit willkommen.

---

## 🔄 Updates

* **Update-Checker:** Die Library prüft alle 12 Stunden auf neue Versionen. Server-Administratoren (OP) werden beim Joinen dezent benachrichtigt, falls ein Update verfügbar ist.

---

## 📄 Lizenz

Dieses Projekt ist unter der MIT-Lizenz lizenziert – siehe die [LICENSE Datei](https://github.com/PeachBiscuit174/PeachPaperLib/blob/master/LICENSE) für Details.

---

## ❤️ Credits & Mitwirkende

Wir sind dankbar für jede Hilfe, die dazu beiträgt, die **PeachPaperLib** zu verbessern! Egal ob es eine neue Funktion, ein Fehlerbericht oder eine kreative Idee ist – jeder Beitrag ist willkommen.

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
