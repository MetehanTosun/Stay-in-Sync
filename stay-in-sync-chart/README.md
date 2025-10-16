# Stay-in-Sync Helm Chart

The stay-in-sync helm chart is supposed to be used for development purposes and testing.

<!-- toc -->

- [Prerequisites](#prerequisites)
  * [Post Setup](#post-setup)
- [Usage](#usage)
  * [Docker images](#docker-images)
  * [Helm chart](#helm-chart)
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
  * [Stay-in-Sync](#stay-in-sync)

<!-- tocstop -->

## Prerequisites

- [Docker](https://docs.docker.com/engine/install/)
- [Minikube](https://kubernetes.io/de/docs/tasks/tools/install-minikube/)
- [Kubectl](https://kubernetes.io/docs/tasks/tools/)
- [Helm](https://helm.sh/docs/intro/install/)
- [OpenLens](https://github.com/MuhammedKalkan/OpenLens?tab=readme-ov-file) (optional)

For more information on the minikube cluster and network setup required for the edc please visit: 
- https://github.com/eclipse-tractusx/tractus-x-umbrella/blob/main/docs/user/setup/README.md
- https://github.com/eclipse-tractusx/tractus-x-umbrella/blob/main/docs/user/network/README.md


### Post Setup

Create umbrella namespace since the edc dependency seems to rely on it: 

```shell
kubectl create namespace umbrella
```


Make sure to enable following minikube addons:

```shell
minikube addons enable ingress
minikube addons enable ingress-dns
```
And add following to your hosts file: 

<span style="color:red">On mac please use 127.0.0.1 instead of minikube ip</span>
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
<your minikube ip> stay-in-sync-management.test
<your minikube ip> stay-in-sync-monitoring.test
<your minikube ip> grafana.stayinsync.local
```

If you are having issues with resolving ingresses after successfully deploying the chart, please try following the
instructions of [Step 3](https://minikube.sigs.k8s.io/docs/handbook/addons/ingress-dns/)

## Usage

### Docker images


The docker images of the stay-in-sync services need to be available to your kubernetes environment to deploy the chart.
To build the images run following command:

```shell
mvn clean install -DskipTests -Dquarkus.container-image.build=true
```

When using **minikube** you could prior to building the images configure to use the docker-env of the minikube to avoid having to load the images:

```shell
eval $(minikube docker-env)
```

Load images into a minikube using:

```shell
minikube image load core-management:latest
```
And remove them with:
```shell
minikube image remove core-management:latest
```

### Helm chart

Before installing the helm chart make sure to update dependencies, only use this command within the charts folder:

```shell
helm dependency update
```

Installing the helm chart from within its folder with the release name **stay-in-sync**:

<span style="color:red">Since this helmchart makes use of the edc umbrella chart we currently recommend installing in the chart umbrella namespace
  in order to avoid issues with the edc setup</span>
```shell
helm install test ./ --namespace umbrella
```

Uninstalling the helm chart using its release name:

```shell
helm uninstall test  --namespace umbrella
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

### Stay-in-Sync

- http://stay-in-sync-management.test/sync-rules
