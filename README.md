# Used External Dependency and Tool

- Redis-server
- Maven
- Jedis (Redis-cli Java Interface)
- JUnit (Only for testing)

# How to use it:

> brew upgrade & brew install redis-server (for MacOS)
> redis-server --port 9000

> cd aggregation\ system
> mvn clean install
> mvn clean compile

start aggregation server with database, load balancer together:

> mvn --projects server exec:java@agg-server

start content server:

> mvn --projects content exec:java@content-server

start client server:

> mvn --projects client exec:java@client-group

The output is a bit mess because it is handling multithreads. To check clear output, you can modify the following files:

> client/resource/config.properties
> content/resource/config.properties
> server/resource/config.properties





























