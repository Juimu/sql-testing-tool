তোমার দেওয়া SqliDumper v5.5-এর সম্পূর্ণ changelog এবং আগের আলোচনায় নির্ধারিত advanced features অনুসারে নিচে একটি পূর্ণাঙ্গ Software Requirements Specification (SRS), Version 1.0 তৈরি করলাম।

এখানে original v5.5-এর সব documented feature অন্তর্ভুক্ত করা হয়েছে। পাশাপাশি নতুন architecture, AI features, security controls, database compatibility, UI/UX, reporting, testing এবং development phases নির্ধারণ করা হয়েছে।

# SOFTWARE REQUIREMENTS SPECIFICATION (SRS)

## SqliDumper Next — Advanced SQL Security Assessment Platform

**Document Version:** 1.0\
**Document Date:** 20 October 2026\
**Project Type:** Cross-Platform Desktop Security Application\
**Original Software Reference:** SqliDumper v5.5 by DonDivel\
**Target Platforms:** Windows, Linux, macOS\
**Document Status:** Initial Development Specification\
**Classification:** Software Development and Architecture

---

# 1. INTRODUCTION

## 1.1 Purpose

এই Software Requirements Specification (SRS)-এর উদ্দেশ্য হচ্ছে SqliDumper v5.5-এর documented capabilities বিশ্লেষণ করে একটি নতুন, উন্নত, modular, secure এবং professional desktop-based SQL security assessment application তৈরি করার প্রয়োজনীয় functional ও non-functional requirements নির্ধারণ করা।

নতুন সফটওয়্যারটি নিম্নলিখিত বিষয়গুলোকে গুরুত্ব দেবে:

- Existing v5.5 features-এর পূর্ণাঙ্গ inventory এবং compatibility assessment।
- Modern desktop interface এবং enhanced user experience।
- Multi-database security assessment।
- AI-assisted vulnerability analysis।
- Advanced task scheduling এবং automation।
- Session management এবং persistent data storage।
- Network proxy ও Tor diagnostics।
- Professional reporting এবং analytics।
- Secure application architecture।
- Cross-platform distribution।
- Extensible plugin system।
- Authorized security testing এবং complete auditability।

## 1.2 Project Objectives

**OBJ-001:** Original SqliDumper v5.5-এর প্রতিটি documented feature traceability matrix-এ অন্তর্ভুক্ত করতে হবে।

**OBJ-002:** Existing supported workflows-এর compatibility বজায় রাখতে হবে, যেখানে সেগুলো নিরাপদ এবং অনুমোদিত ব্যবহারের সঙ্গে সামঞ্জস্যপূর্ণ।

**OBJ-003:** নতুন application-এ modular architecture ব্যবহার করতে হবে, যাতে একটি module পরিবর্তন করলে অন্য module অপ্রয়োজনীয়ভাবে প্রভাবিত না হয়।

**OBJ-004:** Application Windows, Linux এবং macOS-এ ব্যবহারযোগ্য হতে হবে।

**OBJ-005:** Secure assessment, findings verification, evidence management এবং remediation-oriented reporting নিশ্চিত করতে হবে।

**OBJ-006:** উন্নত performance, error handling, task recovery এবং data persistence প্রদান করতে হবে।

## 1.3 Intended Users

1. Authorized penetration testers।
2. Security researchers working in approved laboratories।
3. Web application security analysts।
4. Database security administrators।
5. Security operation teams।
6. Software developers ও QA security engineers।
7. Organizations conducting authorized vulnerability assessments।

## 1.4 Scope

SqliDumper Next হবে একটি desktop security testing এবং vulnerability management platform। সফটওয়্যারটি পূর্বনির্ধারিত ও অনুমোদিত testing scope-এর মধ্যে web application/database security assessment, findings analysis, result management এবং reporting পরিচালনা করবে।

**Operational restriction:** কোনো অননুমোদিত target exploitation, sensitive data theft, uncontrolled brute-force operation বা third-party security controls bypass করার জন্য unrestricted automation এই specification-এর অংশ নয়। Original changelog-এ থাকা এ ধরনের ক্ষমতাগুলো inventory-তে সংরক্ষিত থাকবে, কিন্তু implementation হবে অনুমোদিত laboratory, controlled validation বা নিরাপদ বিকল্প workflow-এর মাধ্যমে।

---

# 2. EXISTING SOFTWARE BASELINE

## 2.1 Original Technology

| Component             | SqliDumper v5.5 baseline     |
| --------------------- | ---------------------------- |
| Programming Language  | Java 17                      |
| GUI Framework         | JavaFX 21                    |
| Build System          | Apache Maven                 |
| Supported OS          | Windows, Linux, macOS        |
| Distribution          | JAR                          |
| Interface             | Custom Dark Theme            |
| Icons                 | Font Awesome 5               |
| Assessment Dependency | sqlmap                       |
| Additional Networking | Tor, HTTP and SOCKS5 proxies |

## 2.2 New Technology Requirements

| Component            | Proposed requirement                                  |
| -------------------- | ----------------------------------------------------- |
| Programming Language | Java 25 LTS, subject to compatibility testing         |
| GUI Framework        | Compatible maintained JavaFX release                  |
| Build                | Maven with dependency locking and reproducible builds |
| Architecture         | Modular layered architecture                          |
| Local Database       | SQLite                                                |
| Secure Storage       | OS keychain plus encrypted application data           |
| Process Management   | Cross-platform supervised process execution           |
| UI Styling           | Custom CSS, themes and SVG icons                      |
| Reporting            | PDF, HTML, JSON, CSV, SARIF                           |
| Logging              | Structured JSON logging                               |
| Test Framework       | JUnit, TestFX and integration-test tooling            |
| Packaging            | jpackage/native installers                            |

All versions must be validated against actual dependencies before release.

---

# 3. SYSTEM ARCHITECTURE

## 3.1 Architectural Layers

The system shall consist of six principal layers:

**Layer 1 — Presentation Layer**

- Main desktop window।
- Sidebar navigation।
- Dashboard।
- Assessment configuration।
- Results and findings।
- Reports।
- Settings।
- Notifications।

**Layer 2 — Application Services**

- Project management।
- User preferences।
- Authorization and scope management।
- Task scheduling।
- Session management।
- Import/export services।

**Layer 3 — Assessment Services**

- Safe SQL injection diagnostics।
- Database-specific assessment profiles।
- Finding verification।
- Structured output parsing।
- Evidence collection।

**Layer 4 — Network Services**

- Proxy management।
- Tor lifecycle and route diagnostics।
- HTTP client।
- TLS verification।
- Connection monitoring।

**Layer 5 — Persistence Services**

- Local project database।
- Scan sessions।
- Findings।
- Reports।
- Logs।
- Encrypted credentials।

**Layer 6 — Extension and Integration Services**

- Plugins।
- AI analysis service।
- CI/CD integrations।
- Issue tracking integrations।
- Report templates।

## 3.2 Architecture Principles

**ARCH-001:** Each module must expose documented interfaces।

**ARCH-002:** UI rendering must not block during long-running tasks।

**ARCH-003:** Background processes must have controlled lifecycles।

**ARCH-004:** Persistent operations must be transaction-safe।

**ARCH-005:** Third-party extensions must operate under explicit permissions।

**ARCH-006:** All security-related actions must be attributable to a project, user and authorized scope।

---

# 4. FUNCTIONAL REQUIREMENTS

## 4.1 Dashboard Module

**Module ID: MOD-001**

| ID       | Requirement                              |
| -------- | ---------------------------------------- |
| DASH-001 | Show total assessment runs               |
| DASH-002 | Show verified vulnerability count        |
| DASH-003 | Show identified DBMS distributions       |
| DASH-004 | Show active and completed tasks          |
| DASH-005 | Show findings by severity                |
| DASH-006 | Show assessment history charts           |
| DASH-007 | Show recently assessed authorized assets |
| DASH-008 | Show task success/failure trends         |
| DASH-009 | Support dashboard data refresh           |
| DASH-010 | Preserve dashboard preferences           |

Acceptance criteria:

- Dashboard opens without initiating assessments।
- KPI values match the local persistent database।
- Charts update after completed tasks।
- Empty projects display meaningful empty states।

## 4.2 Single Target Assessment

**Module ID: MOD-002**

**SCAN-001:** Users shall be able to configure one authorized target URL।

**SCAN-002:** Input URL syntax and protocol must be validated।

**SCAN-003:** The target must be checked against the approved project scope।

**SCAN-004:** Assessment profiles shall support supported database-specific security diagnostics।

**SCAN-005:** Users shall be able to start, pause where supported, cancel and inspect progress।

**SCAN-006:** Findings shall record confidence, category, time and verification status।

**SCAN-007:** Engine output shall be interpreted through a structured parser।

**SCAN-008:** Connection errors, timeouts and dependency failures shall be clearly displayed।

**SCAN-009:** No assessment shall start outside the configured scope।

## 4.3 Batch Assessment Module

**Module ID: MOD-003**

**BATCH-001:** Import multiple URLs from a text file or input area।

**BATCH-002:** Validate URLs before adding them to the task queue।

**BATCH-003:** Identify duplicate targets।

**BATCH-004:** Configure concurrency within project-defined limits।

**BATCH-005:** Track each URL as an independent task।

**BATCH-006:** Support pause, resume and cancellation।

**BATCH-007:** Preserve completed findings during interruption।

**BATCH-008:** Maintain failed-task diagnostics।

**BATCH-009:** Support progress and completion notifications।

**BATCH-010:** Apply per-target rate limits and resource limits।

## 4.4 Vulnerable Targets and Findings

**Module ID: MOD-004**

The original Vulnerable URLs tab shall be represented by a dedicated Findings module.

**FIND-001:** Save verified vulnerability findings automatically।

**FIND-002:** Display URL, hostname, DBMS, web server information when established, severity and verification state।

**FIND-003:** Record whether available evidence establishes elevated database privilege, without requiring privileged actions।

**FIND-004:** Support finding selection and controlled revalidation।

**FIND-005:** Allow filtering and sorting।

**FIND-006:** Support duplicate finding detection।

**FIND-007:** Maintain historical finding revisions।

**FIND-008:** Support statuses: Open, In Progress, Fixed, Accepted Risk and Reopened।

**FIND-009:** Provide a remediation explanation for verified findings।

## 4.5 Database Inspection and Column Selection

**Module ID: MOD-005**

The original software documents database/table/column discovery and selective data dumping. These capabilities must be represented in the compatibility inventory.

**DB-001:** Support database metadata inspection through approved credentials or controlled fixtures।

**DB-002:** Display database, table and column hierarchies।

**DB-003:** Provide column selection checkboxes।

**DB-004:** Provide Select All and Select None controls।

**DB-005:** Display supported data types and schema metadata।

**DB-006:** Restrict real-data access according to database permissions and assessment scope।

**DB-007:** Allow redacted or synthetic sample-data validation।

**DB-008:** Protect authorized exports with appropriate access controls।

## 4.6 Result and Dump Accumulation

**Module ID: MOD-006**

**RESULT-001:** New result groups shall append without overwriting previous groups।

**RESULT-002:** Display source, table identifier where applicable, row count where permitted and timestamp।

**RESULT-003:** Support result history browsing।

**RESULT-004:** Support search and sorting।

**RESULT-005:** Allow redaction of sensitive field values।

**RESULT-006:** Maintain result provenance।

**RESULT-007:** Provide secure evidence export and deletion workflows।

## 4.7 Dork Scanner Compatibility

**Module ID: MOD-007**

The original feature generates search-engine queries and automatically queues discovered URLs for SQL injection testing.

**DISC-001:** Maintain an inventory of documented query-template capabilities।

**DISC-002:** Provide query templates for owned or explicitly authorized web assets।

**DISC-003:** Allow manually reviewed discovery results to be added to an authorized asset inventory।

**DISC-004:** Validate discovered URLs against project scope before assessment।

**DISC-005:** Avoid automatic exploitation of arbitrary search-engine results।

**DISC-006:** Record discovery source and timestamp।

## 4.8 Admin Finder Compatibility

**Module ID: MOD-008**

The original implementation checks candidate administrative paths with configurable wordlists and multiple threads.

**ADMIN-001:** Record original wordlist and endpoint-discovery functionality in the compatibility matrix।

**ADMIN-002:** Support safe endpoint exposure assessments on authorized assets।

**ADMIN-003:** Allow approved endpoint lists।

**ADMIN-004:** Apply request-rate limits।

**ADMIN-005:** Display observed endpoint response status and evidence।

**ADMIN-006:** Avoid authentication brute forcing and unrestricted endpoint enumeration।

## 4.9 File Read Compatibility

**Module ID: MOD-009**

The v5.5 baseline includes server-side file reading through SQL injection, including sensitive-path presets.

**FILE-001:** Document the original file-read capability for compatibility evaluation।

**FILE-002:** Provide laboratory fixtures for validating file-access vulnerabilities।

**FILE-003:** Use harmless predetermined test files for evidence collection।

**FILE-004:** Prevent unauthorized reads of sensitive system files।

**FILE-005:** Preserve relevant test evidence without exposing secrets।

**FILE-006:** Provide mitigation recommendations for file-access vulnerabilities।

## 4.10 Tamper Script Compatibility

**Module ID: MOD-010**

The original version integrates sqlmap tamper-script selection for WAF bypass.

**TAMP-001:** Catalog supported script metadata and compatibility information।

**TAMP-002:** Display enabled and disabled script configurations in controlled laboratory profiles।

**TAMP-003:** Validate scripts for compatibility and integrity before execution।

**TAMP-004:** Isolate untrusted extension code।

**TAMP-005:** Prevent unauthorized WAF-evasion automation against external targets।

**TAMP-006:** Preserve script test results and relevant diagnostics।

## 4.11 Tor Management

**Module ID: MOD-011**

**TOR-001:** Detect available Tor installations।

**TOR-002:** Support application-controlled start/stop when permitted।

**TOR-003:** Support an existing system Tor instance and supported Tor Browser configuration।

**TOR-004:** Display connection status।

**TOR-005:** Display verified exit IP when connectivity checks succeed।

**TOR-006:** Clearly distinguish disconnected, connecting, connected and failed states।

**TOR-007:** Verify configured network paths instead of assuming traffic uses Tor।

**TOR-008:** Restrict traffic to approved targets and configured routes।

**TOR-009:** Prevent unintended routing changes without user approval।

## 4.12 Proxy Management

**Module ID: MOD-012**

**PROXY-001:** Support HTTP and SOCKS5 profiles।

**PROXY-002:** Allow adding, editing and removing proxies।

**PROXY-003:** Import proxy configurations from files।

**PROXY-004:** Perform health checks।

**PROXY-005:** Display health and connection status।

**PROXY-006:** Support approved failover routing policies।

**PROXY-007:** Define explicit behavior when a proxy is unavailable।

**PROXY-008:** Protect proxy authentication credentials।

**PROXY-009:** Record effective routing configuration with assessment logs।

## 4.13 Process Management

**Module ID: MOD-013**

**PROC-001:** Supervise externally launched assessment processes।

**PROC-002:** Stop entire child-process trees when a task is cancelled।

**PROC-003:** Implement platform-specific shutdown mechanisms।

**PROC-004:** Prevent orphaned child processes।

**PROC-005:** Support graceful shutdown before forced termination, where practical।

**PROC-006:** Release temporary resources after task completion।

**PROC-007:** Report abnormal terminations।

## 4.14 Advanced Output Parser

**Module ID: MOD-014**

**PARSE-001:** Parse structured database, table, column and result states।

**PARSE-002:** Preserve original output for troubleshooting with appropriate redaction।

**PARSE-003:** Handle partial output and interrupted tasks।

**PARSE-004:** Maintain deterministic state transitions।

**PARSE-005:** Detect malformed or unsupported output।

**PARSE-006:** Associate parser events with originating tasks।

**PARSE-007:** Provide regression tests for supported output formats।

## 4.15 Navigation and State Preservation

**Module ID: MOD-015**

**NAV-001:** Preserve view state while switching modules।

**NAV-002:** Cache commonly used interface data।

**NAV-003:** Restore table filters and selected project context।

**NAV-004:** Support asynchronous loading।

**NAV-005:** Avoid unnecessary full-view initialization।

## 4.16 Dependency and Path Validation

**Module ID: MOD-016**

**DEP-001:** Detect installed Java and required external dependencies।

**DEP-002:** Validate configured Python/sqlmap paths when these components are enabled।

**DEP-003:** Cache successful validation results।

**DEP-004:** Revalidate when file metadata or configuration changes।

**DEP-005:** Display compatible versions and errors।

**DEP-006:** Prevent execution of missing or untrusted dependencies।

## 4.17 Session Management

**Module ID: MOD-017**

**SESSION-001:** Persist assessment sessions across restarts।

**SESSION-002:** Automatically save safe checkpoints।

**SESSION-003:** Restore completed results and task history।

**SESSION-004:** Distinguish resumable, completed and interrupted tasks।

**SESSION-005:** Provide session search, filtering and deletion।

**SESSION-006:** Recover from unexpected application shutdowns without database corruption।

## 4.18 Reporting

**Module ID: MOD-018**

**REPORT-001:** Export PDF reports।

**REPORT-002:** Export HTML reports।

**REPORT-003:** Export machine-readable JSON।

**REPORT-004:** Support CSV exports for appropriate tabular data।

**REPORT-005:** Support SARIF for suitable findings।

**REPORT-006:** Include target scope, assessment date, methodology and limitations।

**REPORT-007:** Include severity, evidence, findings and remediation recommendations।

**REPORT-008:** Support custom templates and branding।

**REPORT-009:** Include report redaction controls।

**REPORT-010:** Support comparison between historical assessments।

---

# 5. ADVANCED NEW MODULES

## 5.1 AI Security Assistant

**AI-001:** Analyze verified finding descriptions।

**AI-002:** Suggest likely vulnerability classes।

**AI-003:** Explain severity and business impact।

**AI-004:** Recommend mitigation steps।

**AI-005:** Identify likely duplicate findings।

**AI-006:** Assist with false-positive review।

**AI-007:** Generate developer-focused remediation summaries।

**AI-008:** Require user review before treating AI output as a verified security conclusion।

**AI-009:** Avoid transmitting secrets or unredacted sensitive evidence to external AI services without explicit approval।

## 5.2 Advanced Task Scheduler

**TASK-001:** Support task queue prioritization।

**TASK-002:** Schedule authorized assessments।

**TASK-003:** Enforce project concurrency policies।

**TASK-004:** Implement bounded retries।

**TASK-005:** Support cancellation tokens।

**TASK-006:** Provide persisted progress tracking।

**TASK-007:** Prevent duplicate execution after recovery।

## 5.3 Multi-Database Intelligence

**MDB-001:** MySQL support।

**MDB-002:** MariaDB support।

**MDB-003:** PostgreSQL support।

**MDB-004:** Microsoft SQL Server support।

**MDB-005:** Oracle Database support।

**MDB-006:** SQLite support।

**MDB-007:** Provide DBMS-specific security configuration guidance।

**MDB-008:** Record compatible drivers and supported server versions।

## 5.4 Network Diagnostics

**NET-001:** Verify network reachability।

**NET-002:** Inspect TLS certificate validity।

**NET-003:** Provide DNS resolution diagnostics।

**NET-004:** Verify configured proxy/Tor connectivity।

**NET-005:** Display connection errors and latency observations।

**NET-006:** Prevent requests to prohibited destinations।

## 5.5 Professional Findings Analysis

**VULN-001:** Support CVSS scoring।

**VULN-002:** Support CWE identifiers।

**VULN-003:** Map relevant findings to OWASP categories।

**VULN-004:** Track evidence and reproducibility।

**VULN-005:** Track remediation ownership।

**VULN-006:** Support retesting and closure verification।

## 5.6 Advanced Analytics

**ANALYTICS-001:** Historical trend charts।

**ANALYTICS-002:** Severity distributions।

**ANALYTICS-003:** Project-level coverage statistics।

**ANALYTICS-004:** Fixed versus outstanding findings।

**ANALYTICS-005:** Exportable dashboard summaries।

## 5.7 Plugin System

**PLUGIN-001:** Define a versioned extension API।

**PLUGIN-002:** Support signed plugins।

**PLUGIN-003:** Provide plugin enable/disable controls।

**PLUGIN-004:** Implement capability-based permissions।

**PLUGIN-005:** Support plugin isolation and failure recovery।

**PLUGIN-006:** Allow reporting and integration extensions।

## 5.8 Secure Credential Vault

**VAULT-001:** Integrate with supported operating-system credential storage।

**VAULT-002:** Encrypt application-managed secrets।

**VAULT-003:** Mask secret values in the UI।

**VAULT-004:** Redact secrets from logs and reports।

**VAULT-005:** Support credential removal and rotation workflows।

## 5.9 Team Collaboration

**TEAM-001:** Define Owner, Administrator, Analyst and Viewer roles।

**TEAM-002:** Enforce role-based permissions।

**TEAM-003:** Support project ownership।

**TEAM-004:** Record significant user actions।

**TEAM-005:** Support exportable audit trails।

## 5.10 Assessment Comparison

**COMPARE-001:** Compare assessment runs।

**COMPARE-002:** Identify new findings।

**COMPARE-003:** Identify resolved findings।

**COMPARE-004:** Identify reopened findings।

**COMPARE-005:** Compare project risk trends।

## 5.11 External Integrations

**INT-001:** Support issue-tracker exports।

**INT-002:** Support optional GitHub/GitLab integrations।

**INT-003:** Support approved CI/CD workflows।

**INT-004:** Support authenticated webhooks।

**INT-005:** Prevent exporting sensitive information without authorization।

## 5.12 Update Management

**UPDATE-001:** Check official release metadata।

**UPDATE-002:** Validate update signatures।

**UPDATE-003:** Verify package integrity।

**UPDATE-004:** Provide rollback support।

**UPDATE-005:** Support stable and test release channels।

---

# 6. USER INTERFACE REQUIREMENTS

## 6.1 Main Navigation

The desktop application shall contain:

1. Dashboard
2. Projects and Authorized Assets
3. Single Assessment
4. Batch Assessment
5. Findings
6. Database Inspection
7. Results and Evidence
8. Asset Discovery
9. Admin Exposure Checks
10. Controlled File-Access Tests
11. Script Compatibility Lab
12. Proxy Manager
13. Tor Manager
14. Session History
15. Analytics
16. Reports
17. AI Assistant
18. Plugins
19. Settings
20. Logs and Diagnostics

## 6.2 Appearance

**UI-001:** Dark theme as default।

**UI-002:** Optional light theme।

**UI-003:** Consistent iconography।

**UI-004:** Resizable window and panels।

**UI-005:** Responsive desktop layouts।

**UI-006:** Searchable tables।

**UI-007:** Clear loading and error indicators।

**UI-008:** Keyboard shortcuts for common actions।

**UI-009:** Accessible labels, contrast and focus navigation।

**UI-010:** Remember user layout preferences।

---

# 7. DATA REQUIREMENTS

## 7.1 Primary Data Entities

The local database shall support at least:

- Users
- Projects
- AuthorizedAssets
- ScopeRules
- AssessmentProfiles
- AssessmentTasks
- TaskEvents
- Sessions
- Findings
- FindingEvidence
- DatabaseMetadata
- ResultGroups
- ProxyProfiles
- TorConfigurations
- ScriptProfiles
- Reports
- Plugins
- AuditLogs
- ApplicationSettings

## 7.2 Data Integrity

**DATA-001:** Each project must have a unique identifier।

**DATA-002:** Each assessment must reference its parent project।

**DATA-003:** Findings must reference their originating assessment।

**DATA-004:** Timestamp fields shall use an unambiguous standardized format।

**DATA-005:** Database migrations shall be version-controlled।

**DATA-006:** Sensitive evidence must support encryption and redaction।

**DATA-007:** Deletion operations must follow configurable retention policies।

**DATA-008:** The application must recover consistently from interrupted transactions।

---

# 8. NON-FUNCTIONAL REQUIREMENTS

## 8.1 Performance

**NFR-PERF-001:** The application shall keep the UI responsive during assessment operations।

**NFR-PERF-002:** Normal project navigation should complete within an initial target of 500 ms for locally cached views on reference hardware।

**NFR-PERF-003:** Concurrency and memory usage must be configurable।

**NFR-PERF-004:** Large result tables shall use pagination or virtualization।

**NFR-PERF-005:** Performance targets must be benchmarked and finalized during implementation।

## 8.2 Reliability

**NFR-REL-001:** Errors in one task must not crash the entire application।

**NFR-REL-002:** Critical task state must be persisted।

**NFR-REL-003:** Application shutdown must clean up owned processes।

**NFR-REL-004:** Crash recovery must preserve previously committed results।

## 8.3 Security

**NFR-SEC-001:** Authorized scope validation is mandatory before assessments।

**NFR-SEC-002:** Secrets must never be stored in plaintext application configuration files।

**NFR-SEC-003:** Evidence access must be permission-controlled।

**NFR-SEC-004:** Audit logs must preserve significant security-relevant events।

**NFR-SEC-005:** Plugins and updates must be integrity-verified।

**NFR-SEC-006:** External requests must respect defined boundaries and rate limits।

**NFR-SEC-007:** Default behavior must favor low-impact verification।

## 8.4 Compatibility

**NFR-COMP-001:** Windows 10/11 compatibility shall be evaluated against officially supported runtime configurations।

**NFR-COMP-002:** Linux compatibility shall be tested on selected maintained distributions।

**NFR-COMP-003:** macOS Intel/Apple Silicon support shall be validated based on packaging dependencies।

**NFR-COMP-004:** Existing v5.5 configuration and session formats shall be assessed for migration feasibility।

## 8.5 Maintainability

**NFR-MAINT-001:** All public module interfaces must be documented।

**NFR-MAINT-002:** Automated tests must cover critical application logic।

**NFR-MAINT-003:** Dependency upgrades shall use compatibility verification।

**NFR-MAINT-004:** Release builds must be reproducible where supported।

---

# 9. TESTING AND QUALITY ASSURANCE

## 9.1 Testing Categories

- Unit testing
- Integration testing
- UI testing
- Cross-platform testing
- Performance testing
- Security testing
- Database migration testing
- Session recovery testing
- Plugin compatibility testing
- Report validation
- Legacy functionality regression testing

## 9.2 Legacy Compatibility Acceptance

Every original v5.5 feature shall have:

1. A unique compatibility identifier।
2. Original documented behavior।
3. Implementation decision।
4. Supported workflow।
5. Security constraints।
6. Test procedure।
7. Expected outcome।
8. Actual test result।
9. Final status।

Compatibility statuses shall be:

- Preserved
- Improved
- Safely Adapted
- Restricted to Lab
- Pending Verification

No feature shall be silently omitted from the compatibility register।

## 9.3 Release Acceptance Criteria

A production release shall require:

- No unresolved critical application security defects।
- Successful data migration testing where migration is supported।
- Critical workflows covered by automated regression tests।
- Verified process cleanup।
- Verified session persistence।
- Successful installer and update tests।
- Report accuracy checks।
- Scope enforcement testing।
- Cross-platform validation for advertised platforms।

---

# 10. DEVELOPMENT IMPLEMENTATION PLAN

## Phase 0 — Requirements and Compatibility Audit

Deliverables:

- Original v5.5 feature inventory।
- Architecture assessment।
- Compatibility matrix।
- Requirements baseline।
- Initial test fixtures।

## Phase 1 — Application Foundation

Deliverables:

- Java project structure।
- JavaFX application shell।
- Modern navigation।
- Theme engine।
- Settings।
- SQLite persistence।
- Structured logging।

## Phase 2 — Core Platform Services

Deliverables:

- Project management।
- Scope validation।
- Task management।
- Proxy management।
- Tor diagnostics।
- Dependency validation।
- Process supervision।

## Phase 3 — Assessment Modules

Deliverables:

- Single-target assessment।
- Batch task orchestration।
- Database-specific safe diagnostics।
- Structured result parser।
- Controlled assessment profiles।
- Findings recording।

## Phase 4 — Legacy Feature Compatibility

Deliverables:

- Asset discovery।
- Admin endpoint exposure assessment।
- Controlled file-access laboratory tests।
- Script compatibility laboratory।
- Column-selection interface।
- Result accumulation।
- Compatibility test results।

## Phase 5 — Intelligence and Reporting

Deliverables:

- AI Security Assistant।
- Findings verification।
- Risk scoring।
- Analytics।
- Report generation।
- Session recovery।
- Historical comparisons।

## Phase 6 — Enterprise Features

Deliverables:

- Plugin SDK।
- Team permissions।
- Integrations।
- Secure credential vault।
- Audit trails।
- Update manager।

## Phase 7 — Quality Assurance and Release

Deliverables:

- Automated testing।
- Security review।
- Cross-platform packaging।
- Installer validation।
- User documentation।
- Release notes।
- Production readiness report।

---

# 11. RISKS AND DEPENDENCIES

| Risk                                      | Mitigation                                                                       |
| ----------------------------------------- | -------------------------------------------------------------------------------- |
| Original source unavailable               | Treat changelog as documentation baseline; verify executable behavior separately |
| Differences between Java runtime releases | Use compatibility tests and controlled dependency upgrades                       |
| sqlmap output changes                     | Version-aware parsers and regression fixtures                                    |
| Sensitive assessment data                 | Encryption, minimization, redaction and retention policies                       |
| Untrusted plugins                         | Signing, sandboxing and permissions                                              |
| Network route misconfiguration            | Route validation and safe connection policies                                    |
| Data loss after crashes                   | Transaction-safe writes and session checkpoints                                  |
| Cross-platform inconsistencies            | Dedicated operating-system test matrix                                           |
| AI hallucinated findings                  | Human verification and evidence-based results                                    |
| Legacy capability restrictions            | Record status and provide controlled test alternatives                           |

---

# 12. REQUIREMENTS TRACEABILITY

The following v5.5 components are explicitly covered:

| Original capability      | Requirement references     |
| ------------------------ | -------------------------- |
| Java/JavaFX architecture | Section 2, ARCH            |
| Cross-platform support   | NFR-COMP                   |
| Maven and distribution   | Section 2, NFR-MAINT       |
| Custom UI and icons      | Section 6                  |
| Tor integration          | TOR-001–009                |
| Live Tor status          | TOR-004–007                |
| Tor routing coverage     | TOR-007–009                |
| Batch scanning           | BATCH-001–010              |
| Vulnerable URLs          | FIND-001–009               |
| Column selection         | DB-001–008                 |
| Dump accumulation        | RESULT-001–007             |
| Dork scanner             | DISC-001–006               |
| Admin finder             | ADMIN-001–006              |
| File read                | FILE-001–006               |
| Tamper scripts           | TAMP-001–006               |
| HTTP/SOCKS5 proxies      | PROXY-001–009              |
| Proxy rotation/failover  | PROXY-006–009              |
| Process tree kill        | PROC-001–007               |
| Error alerts             | SCAN-008, PROC-007, UI-007 |
| State-machine parsing    | PARSE-001–007              |
| Cached navigation        | NAV-001–005                |
| Path validation caching  | DEP-001–006                |
| Dashboard                | DASH-001–010               |
| Saved sessions           | SESSION-001–006            |
| Exportable reports       | REPORT-001–010             |

---

# 13. DELIVERABLES

The project shall ultimately deliver:

1. Complete desktop application source code।
2. Build configuration and documentation।
3. Windows distribution।
4. Linux distribution।
5. macOS distribution।
6. Application database schema।
7. Automated test suite।
8. Legacy feature compatibility matrix।
9. Module API documentation।
10. User manual।
11. Security administrator guide।
12. Report templates।
13. Release checklist।
14. Software Bill of Materials (SBOM)।
15. Versioned changelog।

---

# 14. FINAL PROJECT REQUIREMENTS

**REQ-FINAL-001:** Every original SqliDumper v5.5 documented feature must be accounted for, including features that require restricted or adapted implementation।

**REQ-FINAL-002:** All newly proposed advanced modules must remain independently testable and maintainable।

**REQ-FINAL-003:** The application must support incremental improvements without requiring a complete rewrite for each feature।

**REQ-FINAL-004:** User data, settings, findings and reports must be protected during updates।

**REQ-FINAL-005:** Application behavior and assessment authorization boundaries must be transparent and auditable।

**REQ-FINAL-006:** A feature must not be marked complete without an appropriate test and documented acceptance result।

**REQ-FINAL-007:** The software must be suitable for legitimate, permission-based security assessment workflows।

---

# 15. DOCUMENT APPROVAL STATUS

**SRS Version:** 1.0\
**Baseline:** User-provided SqliDumper v5.5 changelog\
**Advanced Scope:** Proposed SqliDumper Next enhancements\
**Implementation Status:** Not yet implemented\
**Testing Status:** Not yet tested\
**Compatibility Status:** Pending access to original software/source and functional verification\
**Approval Status:** Draft for technical review

**End of Software Requirements Specification — Version 1.0**

এই SRS-এ original-এর Tor, Proxy, Batch Scanning, Vulnerable URLs, Column Selection, Dump Accumulation, Dork Scanner, Admin Finder, File Read, Tamper Scripts, Process Management, Output Parser, Sessions, Dashboard এবং Reports—সবগুলো documented capability-এর জন্য আলাদা requirements রাখা হয়েছে।

পরবর্তী development baseline: Phase 0-এর feature compatibility audit সম্পন্ন করে Phase 1-এর JavaFX application foundation তৈরি করা। এর মাধ্যমে প্রতিটি নতুন implementation মূল SRS-এর requirement ID অনুযায়ী যাচাই করা যাবে।