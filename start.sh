#!/bin/bash

source ./env
CONNECTOR_DEPLOYMENT='{
  "connector.class": "io.confluent.connect.azure.eventhubs.EventHubsSourceConnector",
  "kafka.topic": "mock-topic",
  "azure.eventhubs.sas.keyname": "RootManageSharedAccessKey",
  "azure.eventhubs.sas.key": "'"$AZURE_EVENTHUBS_SAS_KEY"'",
  "azure.eventhubs.namespace": "slegouellec",
  "azure.eventhubs.hub.name": "mock-topic",
  "azure.eventhubs.consumer.group": "$Default",
  "azure.eventhubs.partition.starting.position": "START_OF_STREAM",
  "azure.eventhubs.transport.type": "AMQP",
  "azure.eventhubs.offset.type": "OFFSET",
  "max.events": "50",
  "tasks.max": "1",
  "value.converter": "org.apache.kafka.connect.storage.StringConverter",
  "key.converter": "org.apache.kafka.connect.storage.StringConverter"
}';

echo "👷‍♂️ Build connect image ..."
docker build \
    --build-arg="CONNECTOR_OWNER=$CONNECTOR_OWNER" \
    --build-arg="CONNECTOR_NAME=$CONNECTOR_NAME" \
    --build-arg="CONNECTOR_VERSION=$CONNECTOR_VERSION" \
    -f connect/Dockerfile \
    -t local_connect_aze_cc .

echo "👷‍♂️ Build translator binaries .."
mvn -f translator/pom.xml clean package

echo "👷‍♂️ Build kafka client image ..."
docker build \
    -f kafka-client/Dockerfile \
    -t local_kafka_client ./kafka-client

docker-compose --env-file ./env -f docker-compose.yml up -d producer-aze consumer-aze connect

MAX_WAIT=480
CUR_WAIT=0
echo "⌛ Waiting up to $MAX_WAIT seconds for connect to start"
docker container logs connect > /tmp/out.txt 2>&1
while [[ ! $(cat /tmp/out.txt) =~ "Finished starting connectors and tasks" ]]; do
    sleep 3
    docker container logs connect > /tmp/out.txt 2>&1
    CUR_WAIT=$(( CUR_WAIT+3 ))
    if [[ "$CUR_WAIT" -gt "$MAX_WAIT" ]]; then
      echo "ERROR: The logs in connect container do not show <Finished starting connectors and tasks> after $MAX_WAIT seconds. Please troubleshoot with <docker container ps> and 'docker container logs"
      exit 1
    fi
done
echo "🚦 connect is started!"

# Deploy Azure Event Source Connector
echo $CONNECTOR_DEPLOYMENT | curl -X PUT http://localhost:8083/connectors/AzureEventHubsSourceConnector_0/config -H "Content-Type: application/json" --data-binary @-

echo "⏳ Wait 2 minutes to produce and consume messages in Azure Event Hubs"
sleep 120

echo "🛑 Stop properly the AZE consumer"
docker-compose --env-file ./env -f docker-compose.yml stop consumer-aze

echo "👷‍♂️ Running the offset translator ..."
java -jar translator/target/Translator-1.0-SNAPSHOT-jar-with-dependencies.jar migration-application-group mock-topic

echo "🟩 Running the CC consumer with the last offset committed"
docker-compose --env-file ./env -f docker-compose.yml up -d consumer-ccloud

docker logs -f consumer-ccloud | grep "Setting offset for partition"