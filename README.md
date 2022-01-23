# open-pdf-sign

CLI application for signing PDF files.

## Requirements
* Maven (https://maven.apache.org/)
* JDK 8

## Build

```bash
mvn package
```

## Usage

```
java -jar open-pdf-sign.jar -i input.pdf -o output.pdf -c certificate.crt -k keyfile.pem -p key_passphrase --page -1 --locale de-AT
```

## License

TBD, mostly Apache2
