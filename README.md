This is a Backport from 1.21.11
You should can use the normal API from 1.21.11 but if you want you can use the specific for this
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
