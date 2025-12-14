# Stay-in-Sync

The stay-in-sync project aims to provide a solution to synchronizing data between various REST-APIs while integrating support for the [asset-administratio-shell](https://www.plattform-i40.de/IP/Redaktion/DE/Downloads/Publikation/AAS-ReadingGuide_202201.pdf?__blob=publicationFile&v=1) and the [tractus-x eclipse dataspace connector](https://github.com/eclipse-tractusx/tractusx-edc)


### What is inside of the repository?

The repository currently consits of five quarkus apps, two angular ui's and a helmchart for deployment. 

* [stay-in-sync-core](../stay-in-sync-core) consists of the three quarkus services
  * [core-management](../stay-in-sync-core/core-management) Enables configuration of the application and dispatches necessary jobs for synchronization
  * [core-polling-node](../stay-in-sync-core/core-polling-node) Polls data & provides data from source systems
  * [core-sync-node](../stay-in-sync-core/core-sync-node) Executes transformation script and writes data to a target system
  * [core-graph-engine](../stay-in-sync-core/core-graph-engine) Provides visual graph-based conditional synchronization logic and change detection
* [stay-in-sync-configurator-ui](../stay-in-sync-configurator-ui) Frontend which exposes the api of the core-management to the user
* [stay-in-sync-monitoring](../stay-in-sync-monitoring) Provides debugging functionality for transformations and monitoring data
  * [monitoring-ui](../stay-in-sync-monitoring/monitoring-ui) Application Metrics, Logs and further data
  * [monitoring-backend](../stay-in-sync-monitoring/monitoring-backend) builds state for monitoring-ui
* [stay-in-sync-chart](../stay-in-sync-chart) Helmchart to deploy the project onto Kubernetes


### Local setup for development

Following system requirements have to be met in order to start the application in development mode: 

* Java 21
* Maven 
* Nodejs
* rabbitmq
* mariadb
* Docker

Following commands can be used to setup docker containers for rabbitmq and mariadb

#### RabbitMQ
```shell
docker run --detach -it --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4-management
```


#### MariaDB
```shell
docker run --detach -p 3306:3306 --name mariadb --env MARIADB_ROOT_PASSWORD=root  mariadb:latest
```
Add a stayinsync_core database post setup. 


#### Starting apps in dev mode: 

Please build the whole project once before starting a service with the database available like so: 

```shell
mvn clean install
```


To start a quarkus app and its corresponding ui run in the folder of the quarkus-app
```shell
mvn quarkus:dev
```
