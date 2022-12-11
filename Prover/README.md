<p align="center">
    <img src="./../sureThing.png" width="70" height="100" alt="CROSS Logo"/>
</p>

<h3 align="center">Prover</i></h3>
<h4 align="center"><i>Extension of <a href="https://github.com/inesc-id/SureThingLedger/tree/v1/Prover">Prover V1</a>
developed by <a href="https://github.com/PedroMatias98">Pedro Matias</a></i></h4>
<h4 align="center"><i>(Prover REST API and PSQL DB developed using Springboot)</i></h4>

---

## Table of Contents

- [Source Prerequisites](#source-prerequisites)
- [Generate a CA.key and CA.crt](#generate-a-cakey-and-cacrt)
- [Run the Prover](#run-the-prover)
- [Authors](#authors)

## Source Prerequisites

- Java Development Kit (JDK) = 11
- Maven >= 3.8
- Postgresql >= 14.2
- Build the [Verifier-Contract](https://github.com/inesc-id/SureThing_Transparency_Data/tree/main/Verifier-Contract)
- Build the [Monitor-Contract](https://github.com/inesc-id/SureThing_Transparency_Data/tree/main/Monitor-Contract)

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

## Run the Prover

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