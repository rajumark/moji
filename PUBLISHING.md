# Publishing Moji

Publishing uses the [vanniktech maven-publish plugin](https://vanniktech.github.io/gradle-maven-publish-plugin/). It uploads to the Maven Central Portal and produces the AAR, sources jar, Dokka javadoc jar, POM and signatures.

## Status (2026-09-23)

- **JitPack: live.** `com.github.rajumark:moji:v1.0.0` is published and resolvable — see the README's Install section.
- **Maven Central: blocked.** The upload validated and was rejected with `Namespace 'io.github.rajumark' is not allowed` (deployment id `0ff5fe17-d014-4892-9b6f-ba97d55c165b`). The Central Portal account currently signed in doesn't own the verified `io.github.rajumark` namespace, even though it's the same GitHub identity (`rajumark`) — looks like a duplicate-account issue on Sonatype's side. Emailed `central-support@sonatype.com` to ask them to identify/merge the account that owns the namespace. Once that's resolved, re-run `./gradlew publishAndReleaseToMavenCentral` — everything else (signing, tests, sample build against the artifact) already passed.

## One-time setup (about an hour)

1. **Maven Central account.** Sign in at https://central.sonatype.com with the GitHub account `rajumark`. The namespace `io.github.rajumark` is verified automatically; check that it shows as *Verified* under Namespaces.
2. **Coordinates** are already set in `gradle.properties`: `io.github.rajumark:moji`, package `io.github.rajumark.hoverfly.moji`, repo `github.com/rajumark/moji`. Create that GitHub repo and push this project, because Central shows the POM links.
3. **User token.** Central Portal → Account → *Generate User Token*.
4. **GPG key** for signing:
   ```bash
   brew install gnupg
   gpg --full-generate-key                      # RSA 4096, no expiry is fine
   gpg --list-secret-keys --keyid-format SHORT  # note the 8-char key id
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
   gpg --armor --export-secret-keys <KEY_ID> > /tmp/moji-signing.asc
   ```
5. **Secrets.** Put these in `~/.gradle/gradle.properties`, never in the repo:
   ```properties
   mavenCentralUsername=<token username>
   mavenCentralPassword=<token password>
   signingInMemoryKey=<contents of moji-signing.asc, newlines replaced by \n>
   signingInMemoryKeyPassword=<gpg passphrase>
   ```
   One way to produce the single-line key:
   ```bash
   awk 'NR>1{printf "\\n"} {printf "%s",$0}' /tmp/moji-signing.asc
   ```
   Then delete `/tmp/moji-signing.asc`.

   In CI, use environment variables instead: `ORG_GRADLE_PROJECT_mavenCentralUsername`, `ORG_GRADLE_PROJECT_mavenCentralPassword`, `ORG_GRADLE_PROJECT_signingInMemoryKey`, `ORG_GRADLE_PROJECT_signingInMemoryKeyPassword`.

## Every release

```bash
# 1. bump the version in gradle.properties (VERSION_NAME) and add a CHANGELOG entry

# 2. tests: JVM parity + a device run
./gradlew :moji:testDebugUnitTest
./gradlew :moji:connectedDebugAndroidTest          # with a device or emulator attached

# 3. dry run: publish to ~/.m2 and build the sample against that artifact
./gradlew :moji:publishToMavenLocal
./gradlew :sample:assembleRelease -PuseMavenLocal

# 4. upload, then check the deployment at https://central.sonatype.com/publishing and press "Publish"
./gradlew :moji:publishToMavenCentral
#    or upload and release in one step:
./gradlew :moji:publishAndReleaseToMavenCentral
```

It usually takes 10–30 minutes after release for `implementation("io.github.rajumark:moji:<version>")` to resolve.

Tag the release too: `git tag v1.0.0 && git push --tags`.

## Before the first public release

- Put the project on GitHub at the URL in `POM_URL`, since Central shows it.
