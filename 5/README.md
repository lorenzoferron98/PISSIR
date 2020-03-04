# REST su RabbitMQ e MQTT

Le due versioni dell'esercizio sono state sviluppate facendo riferimento all'architettura ARCH2 e con RPC sincrone. Le 
operazioni remote messe a disposizione del client sono state definite nell'interfaccia <code>[JsonService](src/main/java/rpc/common/JsonService.java)</code>,
questa permette di istanziare, all'interno del client, oggetti ed invocare il metodo remoto opportuno.
L'implementazione concreta della classe sopra citata si trova in <code>[RestRpc](src/main/java/rpc/common/RestRpc.java)</code>. <br>
Successivamente all'invocazione del metodo remoto, il client creerà la richiesta in [JSON-RPC](https://www.jsonrpc.org/) 
(versione 1.1) che verrà trasmessa al service, secondo le modalità del protocollo (AMQP o MQTT). <br>
Giunta al service, che funge da proxy, la richiesta sarà interpretata al fine di richiamare il metodo corretto nell'interfaccia
<code>[StudentBasicApi](src/main/java/rpc/common/StudentBasicApi.java)</code>, nella quale si è fatto uso della libreria 
[Retrofit](https://square.github.io/retrofit/). Ottenuta la risposta in JSON dall'API REST, dell'[esercizio precedente](../4), 
essa viaggerà fino al client incapsulata come JSON nel _payload_ di un messaggio JSON-RPC. 

In entrambe le versioni è messa a disposizione un'interfaccia testuale (CLI) per poter effettuare delle sperimentazioni,
si veda <code>[GUI](src/main/java/rpc/common/GUI.java)</code>.

## RabbitMQ

Per sperimentare, dopo aver [compilato](../README.md#compila-da-sorgente), eseguire in una finestra di terminale:
```bash
java -jar 5/target/rpc-1.0-SNAPSHOT-rabbitmq.jar URI [-s <URL>]
```
dove:
* `URI` indica l'**URI** per connettersi al broker RabbitMQ, necessario sia per il client sia per il service. \
   \
   Il formato è `[scheme]://[username]:[password]@[host]:[port]/[vhost]` in cui:
   * `scheme` indica il protocollo di connessione `amqp` o `amqps` (AMQP + SSL);
   * `vhost` indica il _virtual host_
   
* `URL` indica l'**URL** dell'API REST, avviato e raggiungibile. Questa opzione è necessaria qualora si voglia avviare il 
service di proxy. \
  \
  Il formato è `[scheme]://[host]:[port]`.
  
Al fine di raggiungere buone prestazioni nel servire più richieste da parte dei client mandare in esecuzione più service.

Le idee spiegate nell'introduzione (vedi sopra) qui sono realizzate attraverso due classi [JsonRpcClient](https://github.com/rabbitmq/rabbitmq-java-client/blob/master/src/main/java/com/rabbitmq/tools/jsonrpc/JsonRpcClient.java) e 
[JsonRpcServer](https://github.com/rabbitmq/rabbitmq-java-client/blob/master/src/main/java/com/rabbitmq/tools/jsonrpc/JsonRpcServer.java).
Le due classi sono implementate nella libreria di RabbitMQ. Nel seguito le illustreremo brevemente:
* `JsonRpcClient(Channel channel, String exchange, String routingKey, int timeout, JsonRpcMapper mapper)` dove:
    * `routingKey` coda sulla quale le richieste del client verranno consumate dal service;
    * `timeout` tempo massimo di attesa della risposta per una chiamata remota;
    * `mapper` classe concreta che implementa l'interfaccia [JsonRpcMapper](https://github.com/rabbitmq/rabbitmq-java-client/blob/master/src/main/java/com/rabbitmq/tools/jsonrpc/JsonRpcMapper.java) 
      per poter interpretare le richieste e generare le risposte. La classe concreta utilizzata è 
      [JacksonJsonRpcMapper](https://github.com/rabbitmq/rabbitmq-java-client/blob/master/src/main/java/com/rabbitmq/tools/jsonrpc/JacksonJsonRpcMapper.java). \
      \
      Per creare un oggetto dell'interfaccia `JsonService` si è utilizzato il metodo \
      `createProxy(Class<T> klass)` il quale, preso come parametro la metaclasse `JsonService.class` ne crea una sua 
      istanza. Tuttavia l'invocazione di un suo metodo viene intercettata dal metodo `invoke(Object proxy, Method method, Object[] args)`
      dell'interfaccia `InvocationHandler`, tale metodo utilizzando la tecnica di riflessione crea un `JsonRpcMapper.JsonRpcRequest`
      secondo le specifiche del JSON-RPC versione 1.1. La richiesta così creata viene pubblicata sulla coda.
      
* `JsonRpcServer(Channel channel, String queueName, Class<?> interfaceClass, Object interfaceInstance, JsonRpcMapper mapper)`,
molto simile alla precedente, le uniche differenze che si notano sono:
    * `queueName` è la coda sui cui il service attende le richieste;
    * `interfaceClass` metaclasse che rappresenta l'interfaccia per i metodi remoti, nel nostro caso `JsonService.class`;
    * `interfaceInstance` implementazione concreta della precedente interfaccia, nel nostro caso `RestRpc`;
    * `mapper` si veda il punto precedente.
    
Si noti infine il meccanismo adottato per stabilire a quale client inviare la risposta. Infatti RabbitMQ utilizza, al fine
di non avere alcun spreco di risorse  o subire crollo delle prestazioni, il [Direct Reply-to](https://www.rabbitmq.com/direct-reply-to.html).

## MQTT 
