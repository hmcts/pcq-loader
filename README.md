# pcq-loader

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=uk.gov.hmcts.reform%3Apcq-loader&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=uk.gov.hmcts.reform%3Apcq-loader) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=uk.gov.hmcts.reform%3Apcq-loader&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=uk.gov.hmcts.reform%3Apcq-loader) [![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=uk.gov.hmcts.reform%3Apcq-loader&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=uk.gov.hmcts.reform%3Apcq-loader) [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=uk.gov.hmcts.reform%3Apcq-loader&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=uk.gov.hmcts.reform%3Apcq-loader) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=uk.gov.hmcts.reform%3Apcq-loader&metric=coverage)](https://sonarcloud.io/summary/new_code?id=uk.gov.hmcts.reform%3Apcq-loader)

This is an API/worker service for the protected characteristics questionnaire platform. It pulls PCQ payloads from blob storage and forwards them to the PCQ backend for processing.

## Overview

<p align="center">
<a href="https://github.com/hmcts/pcq-frontend">pcq-frontend</a> • <a href="https://github.com/hmcts/pcq-backend">pcq-backend</a> • <a href="https://github.com/hmcts/pcq-consolidation-service">pcq-consolidation-service</a> • <a href="https://github.com/hmcts/pcq-shared-infrastructure">pcq-shared-infrastructure</a> • <b><a href="https://github.com/hmcts/pcq-loader">pcq-loader</a></b>
</p>

<br>

<p align="center">
  <img src="https://raw.githubusercontent.com/hmcts/pcq-frontend/master/pcq_overview.png" width="500"/>
</p>

## Notes

Since Spring Boot 2.1 bean overriding is disabled. If you want to enable it you will need to set `spring.main.allow-bean-definition-overriding` to `true`.

JUnit 5 is now enabled by default in the project. Please refrain from using JUnit4 and use the next generation

## Building and deploying the application

### Building the application

The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

To build the project execute the following command:

```bash
  ./gradlew build
```

### Running the application

Create the image of the application by executing the following command:

```bash
  ./gradlew assemble
```

Create docker image:

```bash
  docker-compose build
```

Run the distribution (created in `build/install/pcq-backend` directory)
by executing the following command:

```bash
  docker-compose up
```

This will start the API container exposing the application's port
(set to `4556` in this pcq-loader app).

In order to test if the application is up, you can call its health endpoint:

```bash
  curl http://localhost:4556/health
```

You should get a response similar to this:

```
  {"status":"UP","diskSpace":{"status":"UP","total":249644974080,"free":137188298752,"threshold":10485760}}
```

## Operational configuration

The service reads configuration from environment variables (defaults are in `src/main/resources/application.yaml`) and optionally from a Kubernetes config tree mounted at `/mnt/secrets/pcq/` (see `spring.config.import`).

Required for production:
- `PCQ_BACKEND_URL` - URL for the PCQ backend API.
- `JWT_SECRET` - shared secret used to sign/verify JWTs.
- `STORAGE_ACCOUNT_NAME` - Azure storage account name.
- `STORAGE_KEY` - Azure storage account key (base64 encoded).
- `STORAGE_URL` - Azure blob endpoint URL for the account.

Common optional overrides:
- `APPINSIGHTS_INSTRUMENTATIONKEY` - Application Insights instrumentation key.
- `STORAGE_DOWNLOAD_PATH` - local path for temporary blob downloads.
- `STORAGE_BLOB_PCQ_CONTAINER` - source container name (default `pcq`).
- `STORAGE_BLOB_PCQ_REJECTED-CONTAINER` - rejected container name (default `pcq-rejected`).
- `STORAGE_BLOB_LEASE_TIMEOUT` - blob lease timeout in seconds.
- `BLOB_COPY_TIMEOUT_IN_MILLIS` - timeout for blob copy operations.
- `BLOB_COPY_POLLING_DELAY_IN_MILLIS` - polling delay for blob copy operations.
- `LEASE_ACQUIRE_DELAY_IN_SECONDS` - delay before acquiring blob lease.

Deployment patterns:
- Helm chart at `charts/pcq-loader` wires Key Vault secrets (JWT + storage credentials) to env vars for the CronJob.
- Ensure the pod has write access to `STORAGE_DOWNLOAD_PATH` or the default `/var/tmp/pcq-loader/download/blobs`.

### Alternative script to run application

To skip all the setting up and building, just execute the following command:

```bash
./bin/run-in-docker.sh
```

For more information:

```bash
./bin/run-in-docker.sh -h
```

Script includes bare minimum environment variables necessary to start api instance. Whenever any variable is changed or any other script regarding docker image/container build, the suggested way to ensure all is cleaned up properly is by this command:

```bash
docker-compose rm
```

It clears stopped containers correctly. Might consider removing clutter of images too, especially the ones fiddled with:

```bash
docker images

docker image rm <image-id>
```

There is no need to remove postgres and java or similar core images.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
