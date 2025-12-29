# 🍑 PeachPaperLib

Eine PaperMC-Library;

> [!IMPORTANT]
> **Projekt-Status:** Dies ist ein **Freizeitprojekt**. Updates und Support erfolgen nach zeitlicher Verfügbarkeit und Lust. Es besteht kein Anspruch auf sofortige Fehlerbehebung, jedoch sind Feedback und Interaktion ausdrücklich erwünscht!

---

### 📌 Kompatibilität
| Feature | Status |
| :--- | :--- |
| **Unterstützte MC-Version** | **1.21.10** |
| **Plattform** | PaperMC und forks davon |
| **Java Version** | 21+ |

*Hinweis: Es wird offiziell immer nur die oben genannte Version unterstützt von der aktuelsten Library. Andere Versionen können funktionieren, werden aber nicht aktiv getestet.*

---

## 🚀 Installation

Füge die Library über **JitPack** zu deinem Projekt hinzu.
[![](https://jitpack.io/v/PeachBiscuit174/PeachPaperLib.svg)](https://jitpack.io/#PeachBiscuit174/PeachPaperLib)

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
	    <artifactId>PeachPaperLib</artifactId>
	    <version>v1.0.0</version>
	</dependency>

```

> [!IMPORTANT]
> Damit die Library korrekt geladen wird, füge sie auch als Dependency hinzu in deiner plugin.yml oder paper-plugin.yml

---

## 🛠 API Nutzung

Die Nutzung erfolgt zentral über die Klasse `API`.

**Beispiel:**
```java
// Erstellt einen ItemStack eines Kopfes via Base64-String
ItemStack head = API.getCustomThingsManager().getCustomHeadUtils().getCustomHead("eyJ0ZXh0dXJlcyI6...");
```

---

## 💡Features & Support

Obwohl dies ein Freizeitprojekt ist, ist deine Meinung wichtig!

  **Feature-Wünsche:** Du hast eine Idee für ein neues Tool? Erstelle gerne ein Issue mit dem Label enhancement. Ich schaue es mir an, sobald ich Zeit finde!

  **Bug Reports:** Falls etwas nicht funktioniert, melde es bitte über die [GitHub Issues](https://github.com/PeachBiscuit174/PeachPaperLib/issues).

  **Beiträge:** [Pull Requests](https://github.com/PeachBiscuit174/PeachPaperLib/pulls) sind jederzeit willkommen.

---

## 🔄 Updates

  **Update-Checker:** Die Library prüft alle 12 Stunden auf neue Versionen. Server-Administratoren (OP) werden beim Joinen dezent benachrichtigt, falls ein Update verfügbar ist.

---

## 📄 Lizenz

Dieses Projekt ist unter der MIT-Lizenz lizenziert – siehe die [LICENSE Datei](https://github.com/PeachBiscuit174/PeachPaperLib/blob/master/LICENSE) für Details.
