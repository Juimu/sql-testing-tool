# Validation evidence — SQLNext 0.2.0

The Linux cloud instance uses JDK 25, Maven 3.9.11, JavaFX 21.0.8 and checksum-verified sqlmap 1.10.10. Java source release remains 21.

The full configured run completed **23 tests, 0 failures, 0 errors, 0 skipped**:

- AssessmentTest: 7 passed.
- SecurityServicesTest: 11 passed.
- SqlmapIntegrationTest: 1 passed against a loopback-only in-memory synthetic database endpoint.
- AppSmokeTest: 1 passed; constructed all 18 views and submitted/completed a SQL lab task through JavaFX controls.
- WorkspaceTest: 3 passed.

Reproduce with a trusted installed Python/sqlmap pair:

```sh
mvn -B -ntp -Pheadless \
  -Dsqlnext.python.path=/full/path/to/python \
  -Dsqlnext.sqlmap.script=/full/path/to/sqlmap.py verify
```

The normal `mvn -Pheadless verify` skips the optional sqlmap integration. `mvn verify` additionally skips the headless UI test. These skipped outcomes must not be reported as executed checks.

The build produces CycloneDX JSON/XML SBOMs. A Linux dependency ZIP and a bundled-runtime native app-image were generated; the image's `--self-test` passed. That is a service/dependency self-test without a graphical display. Interactive native OS UI and Windows/macOS packaging acceptance are separate checks.

A previous process-cancellation test exposed descendant reaping order. The supervisor was corrected to terminate/reap descendants before the parent; the targeted test and full suite then passed.

External AI approval, verified-finding restriction and payload minimization are tested through a fake transport. A live AI endpoint has not been called. Real Tor/proxy routing, OS keychain, roles, plugin/update signatures and original v5.5 compatibility are not validated by these tests. See RELEASE-CHECKLIST.md for remaining production requirements.

GitHub Actions run [39679397048](https://github.com/Juimu/sql-testing-tool/actions/runs/39679397048) completed Ubuntu and Windows jobs successfully, producing Linux and Windows artifacts. The macOS job failed at packaging; diagnostic capture was added in the subsequent commit. Those Windows results cover core tests and package generation, not manual graphical use.

The captured macOS failure was the jpackage CFBundleVersion constraint: its first component cannot be zero. The packager now maps pre-1.0 macOS native metadata from 0.x.y to 1.x.y while retaining the actual product/archive version, recorded in PACKAGE-INFO.json. The native packaging command also executes the bundled application's display-free self-test before archiving. Cross-OS results for this correction must be checked in the new run.

Final cross-platform workflow [39680139512](https://github.com/Juimu/sql-testing-tool/actions/runs/39680139512), source commit 5474b36, completed successfully on Ubuntu x64, Windows x64 and macOS ARM64. All three jobs generated dependency/native archives and passed the bundled-runtime self-test. Ubuntu also ran the headless JavaFX test; Windows/macOS ran core tests, not interactive UI tests. The public run exposes all three downloadable artifacts. The production/enterprise checklist remains separate and incomplete.
