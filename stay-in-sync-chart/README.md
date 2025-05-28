# Stay-in-Sync Helm Chart

The stay-in-sync helm chart is supposed to be used for development purposes and testing.

<!-- toc -->

- [Prerequisites](#prerequisites)
  * [Post Setup](#post-setup)
- [Usage](#usage)
- [Ingress](#ingress)
  * [Tractus-x](#tractus-x)
    + [Consumer-1 Controlplane](#consumer-1-controlplane)
    + [Consumer-1 Dataplane](#consumer-1-dataplane)
    + [Dataprovider Controlplane](#dataprovider-controlplane)
    + [Dataprovider Dataplane](#dataprovider-dataplane)
    + [Dataprovider Submodels](#dataprovider-submodels)
    + [PG-Admin](#pg-admin)
    + [SSI Credential Issuer](#ssi-credential-issuer)
  * [Basyx](#basyx)

<!-- tocstop -->

## Prerequisites

- [Docker](https://docs.docker.com/engine/install/)
- [Minikube](https://kubernetes.io/de/docs/tasks/tools/install-minikube/)
- [Kubectl](https://kubernetes.io/docs/tasks/tools/)
- [Helm](https://helm.sh/docs/intro/install/)
- [OpenLens](https://github.com/MuhammedKalkan/OpenLens?tab=readme-ov-file) (optional)

### Post Setup

Make sure to enable following minikube addons:

```shell
minikube addons enable ingress
minikube addons enable ingress-dns
```

To enable your local system to resolve the exposed addresses listed under [Ingress](#ingress), please follow the
instructions of [Step 3](https://minikube.sigs.k8s.io/docs/handbook/addons/ingress-dns/)

Alternatively add following to your /etc/hosts (macOs /private/etc/hosts file:

```shell
minikube ip
```

```
<your minikube ip> centralidp.tx.test
<your minikube ip> sharedidp.tx.test
<your minikube ip> portal.tx.test
<your minikube ip> portal-backend.tx.test
<your minikube ip> semantics.tx.test
<your minikube ip> sdfactory.tx.test
<your minikube ip> ssi-credential-issuer.tx.test
<your minikube ip> dataconsumer-1-dataplane.tx.test
<your minikube ip> dataconsumer-1-controlplane.tx.test
<your minikube ip> dataprovider-dataplane.tx.test
<your minikube ip> dataprovider-controlplane.tx.test
<your minikube ip> dataprovider-submodelserver.tx.test
<your minikube ip> dataconsumer-2-dataplane.tx.test
<your minikube ip> dataconsumer-2-controlplane.tx.test
<your minikube ip> bdrs-server.tx.test
<your minikube ip> business-partners.tx.test
<your minikube ip> pgadmin4.tx.test
<your minikube ip> ssi-dim-wallet-stub.tx.test
<your minikube ip> smtp.tx.test

# Basyx

<your minikube ip> basyx-web-ui.test
<your minikube ip> basyx-aas-registry.test
<your minikube ip> basyx-submodel-registry.test
<your minikube ip> basyx-aas-env.test
```

## Usage

Before installing the helm chart make sure to update dependencies:

```shell
helm dependency update
```

Installing the helm chart with the release name **stay-in-sync**:

```shell
helm install stay-in-sync ./stay-in-sync-chart/
```

Uninstalling the helm chart using its release name:

```shell
helm uninstall stay-in-sync
```

## Ingress

Make sure that you successfully configured the steps described in the [Post Setup Section](#post-setup)

In the current state the helm chart exposes following addresses:

### [Tractus-x](https://github.com/eclipse-tractusx)

#### Consumer-1 Controlplane

- http://dataconsumer-1-controlplane.tx.test/api
- http://dataconsumer-1-controlplane.tx.test/management
- http://dataconsumer-1-controlplane.tx.test/api/v1/dsp

#### Consumer-1 Dataplane

- http://dataconsumer-1-dataplane.tx.test/api
- http://dataconsumer-1-dataplane.tx.test/api/public

#### Dataprovider Controlplane

- http://dataprovider-controlplane.tx.test/api
- http://dataprovider-controlplane.tx.test/management
- http://dataprovider-controlplane.tx.test/api/v1/dsp

#### Dataprovider Dataplane

- http://dataprovider-dataplane.tx.test/api
- http://dataprovider-controlplane.tx.test/management
- http://dataprovider-dataplane.tx.test/api/public

#### Dataprovider Submodels

- http://dataprovider-submodelserver.tx.test/

#### PG-Admin

- http://pgadmin4.tx.test/

#### SSI Credential Issuer

- http://ssi-credential-issuer.tx.test/

### [Basyx](https://github.com/eclipse-basyx/basyx-java-server-sdk)

- http://basyx-web-ui.test
- http://basyx-aas-registry.test
- http://basyx-submodel-registry.test
- http://basyx-aas-env.test
