###Stay-in-Sync Monitoring Backend

This module provides the backend for central monitoring. 
It is based on Quarkus and integrates tools such as Prometheus, Grafana, ELK-Stack and RabbitMQ.

###Local Development Start

Prerequisites: 

- Make sure Docker Desktop is running before starting any services.

Start Quarkus in Development Mode:

cd stay-in-sync-monitoring/monitoring-backend

docker compose up -d

./mvnw quarkus:dev

Once the Quarkus dev server is up, the Swagger UI is available at:

http://localhost:8093/q/swagger-ui/#/

Prometheus is available at : 	http://localhost:9090/query / Prometheus target status: http://localhost:9090/targets

Grafana monitoring dashboards Login credentials	 Username: admin | Password: admin: 	http://localhost:3000 

Once the containers are running, Prometheus will start scraping the configured targets and Grafana will visualize available metrics accordingly.

###Start ELK Stack & RabbitMQ

To start the ELK stack and RabbitMQ, navigate to the following directory and run:

cd stay-in-sync-monitoring/src/main/docker/elk

docker compose up -d

Service Access Points

Kibana:  	http://localhost:5601

Kibana homepage: http://localhost:5601/app/home#/

Elasticsearch: 	http://localhost:9200

RabbitMQ: http://localhost:15672/#/

Once started, Kibana will connect to Elasticsearch and visualize logs sent from your services. RabbitMQ is available for processing asynchronous monitoring and logging events.