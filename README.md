#lookingglass-service

Lookingglass-service is a thin Java [JSON-RPC](http://en.wikipedia.org/wiki/JSON-RPC) interface on top of the [lookingglass library](https://github.com/USC-NSL/lookingglass) that runs as a stand-alone Java application.

##Functionality

- Provides an interoperable API for issuing traceroute requests to Looking Glass servers.
- High-level abstraction for issuing traceroutes.
- Efficiently rate control the number of queries being issued to a particular domain of Looking Glass servers.

##Available Methods

__Submits a traceroute request__
```
submit(lgName, target, type)
```

Arguments:
- lgName (string): the name of the looking glass to issue this traceroute to
- target (string): the target for the traceroute. Note: This should be an ip. Hostnames often fail for looking glasses.
- type (string): can be "http", "telnet", or empty string. Empty string will select the type for you.

Returns:
- measurement_id (integer): an id which can be used to retrieve status and results of traceroutes

__Retrieve all active Looking Glass servers__
```
active()
```

Arguments:
None

Returns:
- lg_list (list of strings): a list of the active looking glass servers

__Retrieve active Looking Glass servers within the specified asn__
```
active(asn)
```

Arguments:
- asn (integer): an autonomous system number

Returns:
- lg_list (list of strings): a list of the active looking glass servers in the specified AS


__Retrieve the ASes with active Looking Glass servers__
```
ases()
```

Arguments:
None

Returns:
- as_list (list of integers): a list of the ASes with active Looking Glass servers

__Find status of measurement__

```
status(measurement_id)
```

Arguments:
- measurement_id (integer): an identifier for a measurement

Returns:
- status_value (string): May be one of, "processing", "not found", "unfinished", "finished", "failed" or "some failed".


__Retrieve results for measurement_id__

If status is unfinished or processing then this may return an empty or incomplete list of results. Be sure to check that the status is "finished" before requesting results.
```
results(measurement_id)
```

Arguments:
- measurement_id (integer): an identifier for a measurement

Returns:
- result_list (list of lists):

##Client Examples

####Java
```java
JsonRpcHttpClient client = new JsonRpcHttpClient(new URL("http://host:port/lg"));

TracerouteService remoteService = ProxyUtil.createClientProxy(
  getClass().getClassLoader(),
  TracerouteService.class,
  client);

int measurementId = remoteService.submit("HURRICANE-AS6939-core1.zrh1.he.net", "8.8.8.8", "http");
```

####Python
```python
>>> import jsonrpclib
>>> lg = jsonrpclib.Server('http://host:port/lg')
>>> measurement_id = lg.submit('HURRICANE-AS6939-core1.zrh1.he.net', '8.8.8.8', 'http')
```

##Build and Run

```bash
$ mvn clean compile assembly:single
```

```bash
$ java -jar target/lookingglass-service-1.0-SNAPSHOT-jar-with-dependencies.jar -config /path/to/lookingglass.conf
```

##Configuration

####Server

Here is a sample configuration file
```
db.url=jdbc:mysql://localhost:3306/lg.db
db.user=looking
db.password=glass
db.driver=com.mysql.jdbc.Driver

server.port=1420
server.maxHttpThreads=10
server.maxTracerouteThreads=20
server.domainQueryInterval=300000
server.queryCheckInterval=1000

#more to come...
```

####Logging
Lookingglass-service uses log4j. By default, logging is configured for most verbose to stdout. To configure logging, specify the location of your log4j configuration file and pass it to the JVM as system property.
```bash
$java -Dlog4j.configuration=file:/my/folder/log4j.properties ...
```
