# File firmato

Per lo scopo dell'esercizio si è realizzata una CA che firma il file in ingresso allo script. Il certificato inviato con
il messaggio è _self-signed_. Lo script allegato verifica inoltre che la procedura eseguita sia corretta.

Si fanno presenti alcuni parametri utili alla verifica della firma:
* il **digest** usato è sha-256;
* l'**algoritmo** usato per la firma è RSA.

Si precisa inoltre che al fine di inviare la firma via e-mail si è utilizzata la conversione in BASE64.

### Requisiti

* OpenSSL versione 1.1.1

### Demo

Eseguire in una finestra di terminale:
```bash
./script.sh file_da_firmare
```
dove:
* `file_da_firmare` percorso del file da firmare