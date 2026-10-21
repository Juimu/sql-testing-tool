# SQLNext — assessment workspace

A first desktop implementation of the supplied **SqliDumper Next SRS**, with a JavaFX interface and a persistent SQLite workspace. This is an incremental foundation release, not the completed assessment platform.

## Working features

- Dashboard backed by real project, finding and audit records; severity chart.
- Project creation with an authorization reference and approved URL scope.
- Single/batch **local scope validation**, including duplicate detection. Exact origin, port and path boundaries are enforced. No requests are sent.
- Manual, explicitly unverified findings with redacted evidence, severity, search, duplicate protection and remediation statuses.
- Atomic finding/status changes with local audit events; persistence across restarts.
- JSON and standalone HTML reports. Evidence, authorization references and finding target URLs are omitted; scope URLs and titles remain. HTML content is escaped; existing export files are never overwritten.
- Persistent dark/light theme and a visible compatibility/delivery register.

## Requirements and launch

Use a full **JDK 21 or newer**, Maven 3.9.11, and JavaFX-compatible desktop libraries. The initial cloud image contains a Java runtime without a complete JDK; cloud setup installs a verified JDK 25. JavaFX 21.0.8 is pinned. Windows/macOS and native installers have not been validated.

```sh
mvn verify
mvn javafx:run
```

On Linux without a display, exercise the actual JavaFX toolkit using the test-only Monocle backend:

```sh
mvn -Pheadless verify
```

In this prepared cloud instance:

```sh
bash scripts/cloud-mvn.sh -Pheadless verify
# Requires a graphical display to interact with the application:
bash scripts/cloud-mvn.sh javafx:run
```

The Maven JAR is not a standalone distribution: use the Maven launcher, which supplies JavaFX native libraries and dependencies. The headless profile is for tests, not an interactive desktop.

## First workflow

1. Open **Projects & scope** and create a project, e.g. scope `https://example.org/app` and an authorization ticket reference.
2. Open **Scope validator**, select the project, and validate URLs. `/app/orders` is accepted; `/application`, different hosts/ports, user information, encoded paths and traversal segments are rejected.
3. Open **Findings**, record a manually reviewed observation for a target inside that scope, and enter redacted evidence.
4. Search records, review evidence and update remediation status. A `Fixed` status is a manual decision, not proof of independent retesting.
5. Export a report through **Reports**. Review its content before sharing.

Default data location: `~/.sqlnext/workspace.db`; the cloud helper sets `SQLNEXT_DATA=/workspace/sqlnext-data` for a writable location. You can override that environment variable, or use the higher-priority system property for an alternate workspace:

```sh
mvn javafx:run -Djavafx.options="-Dsqlnext.data=/path/to/workspace"
```

Back up the SQLite file while the application is closed. The current database and local audit are plaintext and intended for a single local user. Do not store secrets or sensitive production evidence. Vault encryption, authenticated roles, retention management and tamper-resistant auditing are not implemented.

## Scope and remaining work

The supplied [SRS](docs/SRS-v1.0.md) is preserved. The [compatibility register](src/main/resources/compatibility.tsv) accounts for its legacy features and advanced modules, with implemented subsets and pending work explicitly distinguished. Original v5.5 source was not supplied, so executable compatibility is unverified.

Automated assessments/sqlmap integration, task scheduling/recovery, database inspection, discovery, endpoint checks, controlled file/script laboratories, Tor/proxy routing, AI, plugins, credential vault, team permissions, remaining report formats, updates and native installers remain unimplemented. The app does not fabricate scans, vulnerability verification, DBMS identification or assessment counts.

Dependencies and build plugins have explicit versions; there is not yet a complete transitive dependency lock, SBOM or reproducible release acceptance result. Packaging and production release requirements remain pending.

## Validation

`WorkspaceTest` exercises scope boundary rejection, transaction rollback, duplicate protection, audit consistency, persistence after reopening, settings, export privacy, HTML escaping and overwrite protection. `AppSmokeTest` starts a real JavaFX toolkit in headless mode and constructs all seven views against a temporary SQLite database. This is a startup/layout smoke test, not full interactive UI coverage or OS packaging validation.
