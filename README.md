# FileSystemService
File System Service

# how to use
## run server locally
gradle run

## call getFileInfo API from local terminal
```
curl -X POST -H "Content-Type: application/json" -d '{

    "jsonrpc": "2.0",

    "method": "getFileInfo",

    "params": ["/path/to/file"],

    "id": 1

}' http://localhost:8080/jsonrpc

```

