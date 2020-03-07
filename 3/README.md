# Valutazione delle prestazioni di Mosquitto

L'idea di base è creare un file JSON che descriva un ambiente di lavoro, qualora si avessero difficoltà eseguendo 
l'applicativo senza file JSON in ingresso il software ne fornirà uno di esempio pronto per essere editato; cominciamo
ora a trattare il contenuto di questo file. \
Innanzitutto si definiscono il tempo per cui l'ambiente deve
eseguirsi attraverso l'`int` `timeout` e il numero di ripetizioni impostando l'`int` `runCount`, per entrambi è previsto
un valore minimo rispettivamente 30 secondi e 1 _run_. Successivamente si definiscono gli attori all'interno dell'ambiente.
Un attore può essere un _publisher_ oppure un _subscriber_ a seconda che debba pubblicare oppure no un messaggio. 
Illustriamo quest'ultima configurazione con un esempio.

Si consideri il seguente semplice frammento di configurazione:
```json
{
  "runCount": 1,
  "timeout": 30,
  "configs": [
    {
      "size": 7,
      "qos": 2,
      "topic": "/foo",
      "payload": null
    },
    {
      "size": 20,
      "qos": 2,
      "topic": "/foo",
      "payload": "The quick brown fox jumps over the lazy dog."
    }
  ]
}
```
Analizziamone la sintassi e la semantica:
* `configs` è un array di attori siano essi _publisher_ o _subscriber_.
* `size` è un `int` >= 1 che indica il numero di attori con una data configurazione. Nell'esempio si indicano 7 
  _subscriber_ e 20 _publisher_.
* `qos` autoesplicativo.
* `topic` è una stringa che indica il _topic_ su cui pubblicare o sottoscriversi. È possibile anche usare i _wildcards_
  previsti da MQTT (`#` o `+`). Si noti inoltre che tutti i _topic_ cominciano con il carattere `/` e sono sottolivelli
  di `/f3RR0n`. Questo torna utile se si volesse esaminare dall'esterno i messaggi di test che transitano senza mischiarli 
  con altri.
* `payload` è una stringa che assume significati diversi a seconda del suo valore:
    * `null` indica che la configurazione è per un _subscriber_;
    * altrimenti in caso di stringa UTF-8, anche vuota, la configurazione si riferisce a un _publisher_.

Si noti inoltre che è possibile avere _publisher_ che pubblicano dove non ci sono _subscriber_ sottoscritti o viceversa 
ovvero _subscriber_ che si sottoscrivono dove nessun _publisher_ pubblica.

Passiamo ora a trattare l'output fornito dal software di benchmark. Esso è composto da 6 campi, esaminiamoli:
* `sendMsg` indica il numero totale di messaggi pubblicati dai _publisher_.
* `rcvMsg` indica il numero totale di messaggi ricevuti dai _subscriber_.
* `lostMsg*` indica il numero di messaggi persi. Si noti che il numero potrebbe essere non preciso qualora si utilizzano
  i _wildcards_. Il motivo di questa limitazione è da ricercarsi nell'implementazione: infatti per sapere quanti messaggi 
  un _subscriber_ non ha ricevuto bisogna conoscere quanti ne avrebbe dovuto ricevere, tale operazione risulta difficile 
  qualora vi siano degli _wildcards_.
* `averageRTT (ms)` indica il RTT medio in millisecondi. Per una maggiore precisione tale implementazione tiene in 
  considerazione dei QoS scelti: infatti con Qos 0 il RTT è calcolato come il tempo per passare dal livello applicativo a quello 
  fisico. Se facessimo una media unica andremmo incontro a dati che non rispecchiano le naturali aspettative: infatti 
  essendo il RTT per QoS 1 o 2 di molto superiore a QoS 0, esso potrebbe essere eccessivamente influenzato dagli altri.
* `averageElapsedTime (s)` indica il tempo medio in millisecondi delle esecuzioni. Tale misura è utile perché nonostante
  abbiamo manualmente impostato il tempo di esecuzione con l'`int` `runCount` esso non potrà essere ferreamente rispettato.
  Questa limitazione è dovuta principalmente al fatto che non tutti i thread sono eseguiti parallelamente, vi sono _swap-in_
  e _swap-out_ su disco, ma soprattutto non è conveniente interrompere improvvisamente e dall'esterno un thread.
* `speedRate (msg/s)` è autoesplicativa.

In un'ultima analisi si osservino alcuni risultati ottenuti durante i test. I risultati sono reperibili nel file compresso
"results.zip". In esso i file sono correlati: infatti per ogni ambiente "example#.json" esiste un file di risultati
nominato "results#.txt". Esaminiamo alcuni di questi file:
* in "results1.txt" si notano l'incremento dei tempi nei RTT a seconda dei QoS utilizzati, fatto già trattato precedentemente.
* confrontando "result2.txt" e "result3.txt" si nota immediatamente l'alto numero di messaggi persi in QoS 0 rispetto
  che QoS 2. Si fa inoltre presente che il numero di perdite in QoS 2 è significativamente elevato perché il numero di
  messaggi ricevuti è **solo** contato nel frangete di tempo impostato in `timeout`. In questo tempo vi sono anche i 
  _publisher_ che pubblicano messaggi. Tale scelta è stata fatta perché si vuole conoscere effettivamente le prestazioni
  di Mosquitto in un dato spazio temporale con molteplici attori in esecuzione.

Si conclude questa sperimentazione con un approfondimento: infatti leggendo la documentazione di Mosquitto si scopre che
l'obbiettivo di questo esercizio è già stato fornito dal broker. Esiste infatti, un topic "speciale" 
`$SYS/broker/load/messages/sent/1min` che permette di conoscere la media mobile esponenziale di messaggi spediti nell'ultimo
minuto dal broker. Tuttavia l'approccio usato dal broker risulta molto più preciso. Esso, infatti, deve tenere in considerazione
di tutti quei messaggi non utili, ma richiesti da MQTT, ne sono un esempio `PUBACK` (per QoS 1) e `PUBCOMP` (per QoS 2).

### Requisiti

* Java SE 8 o superiore

### Demo

Dopo aver [compilato](../README.md#compila-da-sorgente), eseguire in una finestra di terminale:
```bash
java -jar target/env-qtt.jar ENV_FILE URI [-h] [-V] [-v]
```
dove:
* `ENV_FILE` indica un file JSON che descrive l'ambiente di testing (si veda sopra l'introduzione).
* `URL` indica l'**URI** del broker da "stressare". Il formato è `[scheme]://[username:[password]@[host]:[port]` (per 
  maggiori dettagli si veda l'esercizio [5](../5/README.md#demo)).
* `-V | --verbose` mostra i dati di ogni prova intermedia prima dei risultati medi.
* `-h` e `-v` sono autoesplicativi