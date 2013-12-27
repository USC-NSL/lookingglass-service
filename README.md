#lookingglass-service

Lookingglass-service is a thin Java [JSON-RPC](http://en.wikipedia.org/wiki/JSON-RPC) interface on top of the [lookingglass library](https://github.com/USC-NSL/lookingglass) that runs as a stand-alone Java application.

##Functionality

- Provides an interoperable API for issuing traceroute requests to Looking Glass servers.
- High-level abstraction for issuing traceroutes.
- Efficiently rate control the number of queries being issued to a particular domain of Looking Glass servers.

##Available Methods

__Submits a traceroute request__
```json
submit(request)
```

The request object must have three fields
- lgName: the name of the looking glass to issue this traceroute to
- target: the target for the traceroute. Note: This should be an ip. Hostnames often fail for looking glasses.
- type: can be "http", "telnet", or empty string. Empty string will select the type for you.

The response is an measurement id which can be used to retrieve status and results of traceroutes.

__Returns a list of all active Looking Glass servers__
```json
active()
```

__Returns a list of active Looking Glass servers within the specified asn__
```json
active(asn)
```

##Client Examples

####Java
```java
JsonRpcHttpClient client = new JsonRpcHttpClient(new URL("http://host:port/lg"));

TracerouteService remoteService = ProxyUtil.createClientProxy(
  getClass().getClassLoader(),
  TracerouteService.class,
  client);

Request request = new Request("HURRICANE-AS6939-core1.zrh1.he.net", "8.8.8.8", "http"); 
boolean result = remoteService.submit(request);
```

####Python
```python
>>> import jsonrpclib
>>> lg = jsonrpclib.Server('http://host:port/lg')
>>> req = request.Request('HURRICANE-AS6939-core1.zrh1.he.net', '8.8.8.8', 'http')
>>> response = lg.submit(req);
```

##Build and Run

```bash
$ mvn clean compile assembly:single
```

```bash
$ java -jar target/LookingGlassService-1.0-SNAPSHOT-jar-with-dependencies.jar -config /path/to/lookingglass.conf
```

##Configuration

####Service

Here is a sample configuration file
```
db.url=jdbc:mysql://localhost:3306/lg.db
db.user=looking
db.password=glass
db.driver=com.mysql.jdbc.Driver

#more to come...
```

####Logging
Lookingglass-service uses log4j. By default, logging is configured for most verbose to stdout. To configure logging, specify the location of your log4j configuration file and pass it to the JVM as system property.
```bash
$java -Dlog4j.configuration=file:/my/folder/log4j.properties ...
```
