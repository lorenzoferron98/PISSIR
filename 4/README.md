# API REST

Al fine di realizzare un esercizio il più realistico possibile si è deciso di sfruttare un database su file, in particolare
[SQLite]. \
Il server mette a disposizione un log dettagliato di tutte le richieste che pervengono con relative risposte. Per poter
provare rapidamente le API definite, collegarsi al _path_ [/swagger]. Attraverso la comoda interfaccia grafica è possibile 
testare le API ed esaminarne le risposte, ma anche avere esempi di risposte o richieste in entrambi i formati XML o JSON. \
Infine si riporta la query DDL dello _schema_ creato anche presente nel codice:
```sqlite
create table student (
    student_id INTEGER primary key autoincrement,
    name       TEXT    not null,
    surname    TEXT    not null,
    dob        INTEGER not null,
    cdl        TEXT    not null,
    aa         TEXT    not null,
    gender     TEXT default 'M' not null,
    check (length(gender) == 1)
);
```

### Requisiti

* Java SE 8 o superiore

### Demo

Dopo aver [compilato](../README.md#compila-da-sorgente), eseguire in una finestra di terminale:
```bash
java -jar target/api-rest.jar [db] [-h] [-p] [-v]
```
dove:
* `db` rappresenta il percorso di un file contenente un DB SQLite. Si osserva che tale argomento non è indispensabile
  per l'avvio del server, questo perché SQLite ha la possibilità di avere [In-Memory DB], ovvero creare DB in memoria.
  Se si dovessero creare DB in memoria questi saranno salvati automaticamente nella cartella temporanea del SO corrente
  al termine dell'esecuzione del server. In particolare sarà stampato il percorso di questo file prima della terminazione.
  Viene così fornita la possibilità di esportare ovunque il DB. \
  Si fa notare che il server non salva periodicamente il DB per tale motivo un crash del server avrebbe la conseguenza
  di una perdita totale dei dati aggiunti.
* `-p | --port` indica la porta su cui far eseguire il server, qualora tale opzione non sia fornita la porta di default è 
  la 9000.
* `-h | --help` e `-v | --version` tali opzioni sono autoesplicative.

[//]: # (sitografia)
[SQLite]: https://sqlite.org
[/swagger]: https://swagger.io
[In-Memory DB]: https://www.sqlite.org/inmemorydb.html