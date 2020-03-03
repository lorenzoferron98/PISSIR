# REST su RabbitMQ e MQTT

Le due versioni dell'esercizio sono state sviluppate facendo riferimento all'architettura ARCH2 e con RPC sincrone. Le 
operazioni remote messe a disposizione del client sono state definite nell'interfaccia <code>[JsonService](src/main/java/rpc/common/JsonService.java)</code>,
questa permette di istanziare, all'interno del client, oggetti di tale classe ed invocare il metodo opportuno.
L'implementazione concreta della classe sopra citata si trova in <code>[RestRpc](src/main/java/rpc/common/RestRpc.java)</code>. <br>
Successivamente il client creerà la richiesta in [JSON-RPC](https://www.jsonrpc.org/) (versione 1.1) che verrà trasmessa
al service, secondo le modalità del protocollo (AMQP o MQTT). <br>
Giunta al service, che funge da proxy, la richiesta sarà interpretata al fine di richiamare il metodo corretto nell'interfaccia
<code>[StudentBasicApi](src/main/java/rpc/common/StudentBasicApi.java)</code>, nella quale si è fatto uso della libreria 
[Retrofit](https://square.github.io/retrofit/). Ottenuta la risposta dall'API REST, [dell'esecizio precedente](../4), essa
viaggerà fino al client incapsulata come JSON nel _payload_ di un messaggio JSON-RPC. 

In entrambe le versioni è messa a disposizione un'interfaccia testuale (CLI) per poter effettuare delle sperimentazioni,
si veda <code>[GUI](src/main/java/rpc/common/GUI.java)</code>.

## RabbitMQ


## MQTT 
