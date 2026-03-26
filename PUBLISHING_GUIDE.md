# 📦 Maven Publishing Guide: Publish to Maven Central

Follow these steps to publish `env-validator` to the Central Repository via Sonatype.

## 1. Prerequisites
- **Sonatype Account**: Register at [JIRA Sonatype](https://issues.sonatype.org/).
- **GPG Keys**: Install GPG and generate a key pair to sign your artifacts.
  ```bash
  gpg --gen-key
  gpg --keyserver hkp://keyserver.ubuntu.com --send-keys <KEY_ID>
  ```

## 2. Prepare `pom.xml`
Ensure your `pom.xml` includes metadata for Central:
- `<name>`, `<description>`, `<url>`
- `<licenses>`, `<developers>`, `<scm>`

Add the following plugins:

### GPG Signing Plugin
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-gpg-plugin</artifactId>
    <version>3.1.0</version>
    <executions>
        <execution>
            <id>sign-artifacts</id>
            <phase>verify</phase>
            <goals>
                <goal>sign</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Sonatype Nexus Staging Plugin
```xml
<plugin>
    <groupId>org.sonatype.plugins</groupId>
    <artifactId>nexus-staging-maven-plugin</artifactId>
    <version>1.6.13</version>
    <extensions>true</extensions>
    <configuration>
        <serverId>ossrh</serverId>
        <nexusUrl>https://s01.oss.sonatype.org/</nexusUrl>
        <autoReleaseAfterClose>true</autoReleaseAfterClose>
    </configuration>
</plugin>
```

## 3. Configure `settings.xml`
Add your Sonatype credentials to `~/.m2/settings.xml`:
```xml
<servers>
    <server>
        <id>ossrh</id>
        <username>your-jira-username</username>
        <password>your-jira-password</password>
    </server>
</servers>
```

## 4. Deployment
Run the following command to sign and upload:
```bash
mvn clean deploy -P release
```

## 5. Final Release
Log in to [OSS Index](https://s01.oss.sonatype.org/), find your "Staging Repository", and click **Close** then **Release**.
