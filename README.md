<p align="center">
    <img src="./sureThing.png" width="70" height="100" alt="CROSS Logo"/>
</p>

<h3 align="center">Location Certificate Transparency</i></h3>
<h4 align="center"><i>Extension of <a href="https://github.com/inesc-id/SureThingLedger/tree/v1">LCT V1</a> developed
by <a href="https://github.com/PedroMatias98">Pedro Matias</a></i></h4>
<h4 align="center"><i>(Server REST API and PSQL DB developed using Springboot)</i></h4>

---

## Table of Contents

- [Introduction](#introduction)
- [Benefits of LCT](#benefits-of-lct)
- [Main Components](#main-components)
- [Extra Components](#extra-components)
- [Structure](#structure)
- [Source Prerequisites](#source-prerequisites)
- [Generate a CA.key and CA.crt](#generate-a-cakey-and-cacrt)
- [Generate Keys and Certificates for Each Module](#generate-keys-and-certificates-for-each-module)
- [Initialize and Run the Postgresql Database for the Ledger or Monitor](#initialize-and-run-the-postgresql-database-for-the-ledger-or-monitor)
- [Run Modules](#run-modules)
    - [Run Ledger, Verifier, Monitor or Auditor Modules](#run-ledger-verifier-monitor-or-auditor-modules)
    - [Run the Prover](#run-the-prover)
- [Authors](#authors)

## Introduction

Location Certificate Transparency (LCT) is a framework of logs, monitors, and auditors created to help mobile users and
location-based service providers oversee location certificates (LCerts) issued or provided to them. LCerts bind
locations, at specific time, to users (provers). Similar
to [Certificate Transparency (CT)](https://certificate.transparency.dev/) that improves the web PKI (Public key
infrastructure), LCT provides an easy way to find misissued or rogue LCerts and strengthens the location certification
systems by creating publicly auditable records of certificate issuance. All issued LCerts should be published to public
LCT logs.

### Benefits of LCT

- Earlier Detection
  - LCT helps detect unauthorized certificates faster.
  - Users can identify any certificate issued without express approval or outside their policy.
- Faster Mitigation
  - Using LCT helps users identify which certificate requires revocation, allowing them to quickly communicate and
    shortening the revocation process of a certificate.
- Better Insight
  - LCT provides public insight into the location certification systems, giving anyone the ability to observe and
    verify the system's health and integrity.
- Strong Security
  - LCT strengthens the chain of trust on location certification systems.
  - Verifying identities and issuing high-assurance location certificates.

### Main Components

The following are the main components of the LCT framework:

1) **Certificate Log**: Certificate logs maintain records of all issued certificates. Multiple independent logs are
   required to allow for backups in case of a log failure or log operator is compromised and also to guarantee consensus
   on log’s activities and avoid collusion. The certificate log is:
  - Append only: Certificates can only be added to the log; they can’t be deleted, modified, or retroactively
    inserted.
  - Cryptographically assured: Log uses Merkle Tree Hash to prevent tampering.
  - Publicly auditable: Anyone can query the log and look for misused or rogue certificates. All certificate logs must
    publicly advertise their URLs and public key.

2) **Certificate Monitor**: A certificate monitor is a service that watches the certificate logs for suspicious
   activities and can fetch information from the logs.

3) **Certificate Auditor**: Certificate auditors check the log to verify that its consistent with other replicated logs,
   that new entries have been added, and that the log has not been corrupted (e.g., inserting, deleting, or modifying a
   certificate). Auditors could be a standalone service or could be a secondary function of a monitor.

### Extra Components

1) **Demo Prover**: Example of a Prover, that can create Location Claims and can submit them to a Verifier. It can also
   ask for Audit Proofs to a Certificate Auditor.
2) **Demo Verifier**: Example of a Verifier that can create Location Certificates and can store them in a Certificate
   Log. It can also use a Certificate Monitor to get stored Location Certificates from the Certificate Log.
3) **CA**: CA that is responsible for signing Certificate Signing Requests.
4) **CertificateRequester**: CertificateRequester is a module that allows the generation of Keys and Certificates for a module.

## Structure

| Directory                                    |         Description          |
|:---------------------------------------------|:----------------------------:|
| [CA](CA)                                     |          CA Module           |
| [CertificateRequester](CertificateRequester) | CertificateRequester Module  |
| [Ledger](Ledger)                             |        Ledger Module         |
| [Monitor](Monitor)                           |       Monitor  Module        |
| [Auditor](Auditor)                           |        Monitor Module        |
| [Verifier](Verifier)                         |       Verifier Module        |

## Source Prerequisites

- Java Development Kit (JDK) = 11
- Maven >= 3.8
- Postgresql >= 14.2
- Module specific Prerequisites (See Readme in specific Module)

## Generate a CA.key and CA.crt

From the root of the repository go to the resources of CA

```shell script
cd CA/src/main/resource
```

Give execute permissions to the script _(responsible for creating the key and a self signed certificate for the CA)_

```shell script
chmod +x newCA.sh
```

Execute the script

```shell script
./newCA.sh
```

Add CA Certificate to Java Trusted Certificates. _(You will need the password. (Default=changeit))_

```shell script
keytool -importcert -trustcacerts -cacerts -file CA.crt -alias LedgerCA -storepass changeit
```

Note: If you had previously done this step, then you need to delete first the LedgerCA certificate.

```shell script
keytool -delete -alias LedgerCA -trustcacerts -cacerts -storepass changeit
```

## Generate Keys and Certificates for each Module

From the root of the repository go to the CA

```shell script
cd CA
```

Give execute permissions to the script _(responsible for creating a certificate for a module)_
```shell script
chmod +x newCA.sh
```

Run CA
```shell script
mvn spring-boot:run
```

From the root of the repository go to the CertificateRequester

```shell script
cd CertificateRequester
```

Give execute permissions to the scripts  _(responsible for creating the key and the certificate signed request to give
to the CA for each module)_:

```shell script
chmod +x src/main/resource/newCSR.sh
chmod +x src/main/resource/newKeyStore.sh
```

Run CertificateRequester for the modules you want to run in order to get their keys and certificates ${ModuleName} =
Ledger, Verifier, Monitor, Auditor, MaliciousLedger

```shell script
mvn spring-boot:run -Dspring-boot.run.arguments="--requester.name=${ModuleName}"
```

## Initialize and Run the Postgresql Database for the Ledger or Monitor

${ModuleName} = Ledger Or Verifier Or Monitor

From the root of the project go to the ${ModuleName}:

```shell script
cd ${ModuleName}
```

Give execute permissions to the initialization script:

```shell script
chmod +x ./newDb.sh
```

Execute the initialization script _(responsible for creating and populating the tables)_:

```shell script
./newDb.sh
```

## Run Modules

Ledger must be deployed before Monitor, Auditor and Verifier. Prover can only run after Monitor, Auditor, Verifier and
Ledger are deployed.

### Run Ledger, Verifier, Monitor or Auditor Modules

From the root of the project go to the ${ModuleName}:

```shell script
cd ${ModuleName}
```

Run Module:

```shell script
mvn spring-boot:run
```

### Run the Prover

From the root of the project go to the Prover:

```shell script
cd Prover
```

```shell script
mvn exec:java
```

## Authors

| Name              | University                 |                                                                                                                                                                                                                                                                                                                                                             More info |
|:------------------|----------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
| Pedro Matias      | Instituto Superior Técnico |                                                                                                                                                  [<img src="https://i.ibb.co/brG8fnX/mail-6.png" width="17">](mailto:pedro.matias.carvalho@tecnico.ulisboa.pt) [<img src="https://github.githubassets.com/favicon.ico" width="17">](https://github.com/PedroMatias98) |
| Rafael Figueiredo | Instituto Superior Técnico |     [<img src="https://i.ibb.co/brG8fnX/mail-6.png" width="17">](mailto:rafafigoalexandre@gmail.com "rafafigoalexandre@gmail.com") [<img src="https://github.githubassets.com/favicon.ico" width="17">](https://github.com/rafafigo "rafafigo") [<img src="https://i.ibb.co/TvQPw7N/linkedin-logo.png" width="17">](https://www.linkedin.com/in/rafafigo/ "rafafigo") |