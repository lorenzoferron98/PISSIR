# REST su RabbitMQ e MQTT

Le due versioni dell'esercizio sono state sviluppate facendo riferimento all'architettura ARCH2 e con supporto al _thread safe_.
Si precisa che la singola chiamata viene gestita in maniera sincrona, questo garantisce un unico RTT per inviare la 
richiesta e ottenerne la risposta. \
Le operazioni remote messe a disposizione del client sono state definite nell'interfaccia <code>[JsonService]</code>,
questa permette di istanziare, all'interno del client, oggetti dai quali invocare il metodo remoto opportuno.
L'implementazione concreta della classe sopra citata si trova in <code>[RestRpc]</code>. \
Successivamente all'invocazione del metodo remoto, il client creerà la richiesta in [JSON-RPC] (versione 1.1) che verrà 
trasmessa al service, secondo le modalità del protocollo (AMQP o MQTT). \
Giunta al service, che funge da proxy, la richiesta sarà interpretata al fine di richiamare il metodo corretto nell'interfaccia
<code>[StudentBasicApi]</code>, nella quale si è fatto uso della libreria 
[Retrofit]. Ottenuta la risposta in JSON dall'API REST, dell'[esercizio precedente](../4), essa viaggerà fino al client 
incapsulata come JSON nel _payload_ di un messaggio JSON-RPC. 

In entrambe le versioni è messa a disposizione un'interfaccia testuale (CLI) per poter effettuare delle sperimentazioni,
si veda <code>[GUI](src/main/java/rpc/common/GUI.java)</code>.

### Requisiti

* Java SE 8 o superiore

### Demo

Dopo aver [compilato](../README.md#compila-da-sorgente), eseguire in una finestra di terminale:
```bash
java -jar target/rpc.jar broker_URI req_location [-h] [-s <URL_API>] [-t <ms>]
```
dove
* `broker_URI` rappresenta l'**URI** per connettersi al broker. Richiesto sia per il client sia per il proxy. \
  Il formato è `[scheme]://[username]:[password]@[host]:[port]/[vhost]` in cui:
  * `scheme` indica il protocollo di connessione:
    * `amqp` o `amqps` (AMQP + SSL) per RabbitMQ.
    * `tcp` o `ssl` per MQTT. \
  Attraverso questa stringa è possibile conoscere a quale broker connettersi e di conseguenza eseguire gli opportuni
  metodi privati nella classe principale <code>[Rpc](src/main/java/Rpc.java)</code>.
  * `vhost` indica il _virtual host_ solo per RabbitMQ. I _path_ non sono permessi in MQTT.
  
* `req_location` per entrambe le versioni rappresenta il "luogo" dove pubblicare le richieste. In particolare per MQTT è 
  un topic, mentre per RabbitMQ è una coda **condivisa** tra proxy: infatti è possibile eseguire più istanze dello stesso. 
  Questo permetterà di avere un maggiore _throughput_ nel soddisfare le richieste.
  
* `URL_API` indica l'**URL** dell'API REST, avviato e raggiungibile. Questa opzione è necessaria qualora si voglia avviare 
  un proxy. Il formato è `[scheme]://[host]:[port]`.

## RabbitMQ

Le idee trattate nell'introduzione (vedi sopra) qui sono realizzate attraverso due classi <code>[JsonRpcClient]</code> 
e <code>[JsonRpcServer]</code>.
Le due classi sono implementate nella libreria di RabbitMQ. Nel seguito le illustreremo brevemente:
* `JsonRpcClient(Channel channel, String exchange, String routingKey, int timeout, JsonRpcMapper mapper)` dove:
    * `channel` connessione virtuale all'interno di una connessione TCP concreta.
    * `routingKey` coda sulla quale le richieste del client verranno consumate dal proxy.
    * `timeout` tempo massimo di attesa della risposta per una chiamata remota.
    * `mapper` classe concreta che implementa l'interfaccia <code>[JsonRpcMapper]</code> per poter interpretare le 
    richieste e codificare le risposte. La classe concreta utilizzata è <code>[JacksonJsonRpcMapper]</code>. \
     \
     Per creare un oggetto dell'interfaccia `JsonService` si è utilizzato il metodo `createProxy(Class<T> klass)` il
     quale, preso come parametro la metaclasse `JsonService.class` ne crea una sua istanza. Tuttavia l'invocazione di 
     un suo metodo viene intercettata dal metodo <code>[invoke(...)]</code> dell'interfaccia <code>[InvocationHandler]</code>, 
     tale metodo utilizzando la tecnica di riflessione crea un oggetto <code>[JsonRpcMapper.JsonRpcRequest]</code> secondo 
     le specifiche del JSON-RPC (versione 1.1). La richiesta così creata viene pubblicata sulla coda.
      
* `JsonRpcServer(Channel channel, String queueName, Class<?> interfaceClass, Object interfaceInstance, JsonRpcMapper mapper)`,
molto simile alla precedente, le uniche differenze che si notano sono:
    * `channel` si veda il punto precedente.
    * `queueName` è la coda sui cui il service attende le richieste.
    * `interfaceClass` metaclasse che rappresenta l'interfaccia per i metodi remoti, nel nostro caso `JsonService.class`.
    * `interfaceInstance` implementazione concreta della precedente interfaccia, nel nostro caso `RestRpc`.
    * `mapper` si veda il punto precedente.
    
Si noti infine il meccanismo adottato per stabilire a quale client inviare la risposta. Infatti RabbitMQ utilizza, al fine
di non avere alcun spreco di risorse  o subire crollo delle prestazioni, il [Direct Reply-to].

## MQTT 

Al fine di sfruttare un'unica interfaccia per i metodi remoti, `JsonService`, si è proceduto creando due versioni molto
simili di <code>[JsonRpcClient](src/main/java/rpc/json/JsonRpcClient.java)</code> e <code>[JsonRpcServer](src/main/java/rpc/json/JsonRpcServer.java)</code>.
Tali versioni sfruttano componenti della libreria di RabbitMQ quali <code>[JsonRpcMapper.JsonRpcRequest]</code>, 
<code>[JsonRpcMapper.JsonRpcResponse]</code>, <code>[BlockingCell]</code> e in parte <code>[JacksonJsonRpcMapper]</code>.
L'idea di base è quella di passare dalla logica di connessione e di comunicazione di RabbitMQ a quelle di MQTT. \
Si evidenzia inoltre che il numero di istanze del proxy contemporaneamente eseguibili è limitato a 1. Tale limitazione è 
dovuta al fatto che in MQTT, a differenza di AMQP, non è presente il concetto di "coda non esclusiva". Illustriamo quanto
detto con un esempio. \
Si immagini il seguente scenario due proxy e un client. In AMQP è possibile definire una coda condivisa tra _channel_, 
che raccolga tutte le richieste dei client. Tale coda viene consumata da un proxy alla volta, per tanto ogni proxy lavora
su una **sola** richiesta e complementarménte ogni client attende una risposta. \
In MQTT, come abbiamo già detto, non è presente il concetto di coda condivisa, per tanto qualora il client faccia una
richiesta essa sarà soddisfatta da entrambi i proxy, se liberi. Tale condizione pone uno spreco di risorse, con conseguente
diminuzione del _throughput_, anche se l'intento era proprio l'opposto. \
Per concludere si fa cenno ad un'altra possibile soluzione del problema: infatti sfruttando la versione più aggiornata del
protocollo MQTT, ovvero la 5, si può usufruire del [request/response pattern]. Tuttavia tale soluzione **attualmente**
ha alcune problematiche:
* la [libreria] usata supporta tale versione del protocollo solo in forma sperimentale e per questo necessita di una 
  compilazione manuale. Per ovviare al problema si potrebbe utilizzare un'[altra libreria].
* la versione di Mosquitto messa a disposizione dall'università non ha il supporto per tale versione: infatti la versione
  attualmente installata è la 1.4.10, mentre il supporto è stato aggiunto solo dalla [1.6] in poi.
  
Infine qualora si volesse sperimentare tale funzionalità si può utilizzare il comando <code>[mosquitto_rr]</code> disponibile 
dalla versione 1.6.9.

[//]: # (sitografia)
[JsonService]: src/main/java/rpc/JsonService.java
[RestRpc]: src/main/java/rpc/RestRpc.java
[JSON-RPC]: https://www.jsonrpc.org/
[StudentBasicApi]: src/main/java/rpc/StudentBasicApi.java
[Retrofit]: https://square.github.io/retrofit/
[JsonRpcClient]: https://github.com/rabbitmq/rabbitmq-java-client/blob/52c0643e3f0dd1d65fee7540410e7b611d239435/src/main/java/com/rabbitmq/tools/jsonrpc/JsonRpcClient.java
[JsonRpcServer]: https://github.com/rabbitmq/rabbitmq-java-client/blob/52c0643e3f0dd1d65fee7540410e7b611d239435/src/main/java/com/rabbitmq/tools/jsonrpc/JsonRpcServer.java
[JacksonJsonRpcMapper]: https://github.com/rabbitmq/rabbitmq-java-client/blob/52c0643e3f0dd1d65fee7540410e7b611d239435/src/main/java/com/rabbitmq/tools/jsonrpc/JacksonJsonRpcMapper.java
[JsonRpcMapper]: https://github.com/rabbitmq/rabbitmq-java-client/blob/52c0643e3f0dd1d65fee7540410e7b611d239435/src/main/java/com/rabbitmq/tools/jsonrpc/JsonRpcMapper.java
[JsonRpcMapper.JsonRpcRequest]: https://github.com/rabbitmq/rabbitmq-java-client/blob/52c0643e3f0dd1d65fee7540410e7b611d239435/src/main/java/com/rabbitmq/tools/jsonrpc/JsonRpcMapper.java#L52
[Direct Reply-to]: https://www.rabbitmq.com/direct-reply-to.html
[InvocationHandler]: https://docs.oracle.com/javase/8/docs/api/java/lang/reflect/InvocationHandler.html
[invoke(...)]: https://docs.oracle.com/javase/8/docs/api/java/lang/reflect/InvocationHandler.html#invoke-java.lang.Object-java.lang.reflect.Method-java.lang.Object:A-
[JsonRpcMapper.JsonRpcResponse]: https://github.com/rabbitmq/rabbitmq-java-client/blob/52c0643e3f0dd1d65fee7540410e7b611d239435/src/main/java/com/rabbitmq/tools/jsonrpc/JsonRpcMapper.java#L91
[BlockingCell]: https://github.com/rabbitmq/rabbitmq-java-client/blob/52c0643e3f0dd1d65fee7540410e7b611d239435/src/main/java/com/rabbitmq/utility/BlockingCell.java
[request/response pattern]: https://www.hivemq.com/blog/mqtt5-essentials-part9-request-response-pattern/
[mosquitto_rr]: https://mosquitto.org/man/mosquitto_rr-1.html
[libreria]: https://github.com/eclipse/paho.mqtt.java
[altra libreria]: https://github.com/hivemq/hivemq-mqtt-client
[1.6]: https://github.com/eclipse/mosquitto/blob/68c1e51035467ade10533c7bb88aa9765241c104/ChangeLog.txt#L296
 