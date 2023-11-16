# Prerequisites

- Docker & Docker-Compose
- Maven & Java
- An Azure Event Hub
- A Confluent Cloud cluster

# How to configure

Configure these files, replacing "{TO_COMPLETE}" by the current value before starting
``` log
./env
./connect/aze.properties
./connect/cc.properties
./kafka-client/src/main/resources/config-aze.properties
./kafka-client/src/main/resources/config-ccloud.properties
./translator/src/main/resources/config-source.properties
./translator/src/main/resources/config-sink.properties
```

# How to start ? 

``` bash
./start.sh
```

# How to stop ? 

``` bash
./stop.sh
```