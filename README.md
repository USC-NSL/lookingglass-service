#lookingglass-service

lookingglass-service is a thin Java [JSON-RPC](http://en.wikipedia.org/wiki/JSON-RPC) interface on top of the [lookingglass library](https://github.com/USC-NSL/lookingglass). It runs as a stand alone Java application.

##Client Examples

Java
```

```

Python
```
>>> import jsonrpclib
>>> lg = jsonrpclib.Server('http://host:port/lg')
>>> response = lg.submit();
```
