#lookingglass-service

lookingglass-service is a thin Java [JSON-RPC](http://en.wikipedia.org/wiki/JSON-RPC) interface on top of the [lookingglass library](https://github.com/USC-NSL/lookingglass). It runs as a stand alone Java application.

##Available Methods

##Client Examples

Java
```
JsonRpcHttpClient client = new JsonRpcHttpClient(new URL("http://127.0.0.1:"+port+"/lg"));

TracerouteService remoteService = ProxyUtil.createClientProxy(
  getClass().getClassLoader(),
  TracerouteService.class,
  client);

Request request = new Request("Comcast", "www.google.com", "http"); 
boolean result = remoteService.submit(request);
```

Python
```
>>> import jsonrpclib
>>> lg = jsonrpclib.Server('http://host:port/lg')
>>> req = request.Request('Comcast', 'www.google.com', 'http')
>>> response = lg.submit(req);
```
