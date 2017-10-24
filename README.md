# AndroidPing 
Ping util for Android platform.

## How 
```
//one
AndroidPing.PingAnswer answer = AndroidPing.simplePing(hostname,1,1);


//two
String pingCommand = AndroidPing.createSimplePingCommand(hostname,1,1);
String pingAnswerString = AndroidPing.ping(pingCommand);
AndroidPing.PingAnswer pingAnswer = AndroidPing.parsePingAnswerString(pingAnswerString);
```

