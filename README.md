# open-pdf-sign

CLI application for signing PDF files.

## Build

```bash
mvn package
```

## Usage

```
java -jar open-pdf-sign.jar -i input.pdf -o output.pdf -c certificate.crt -k keyfile.pem -p key_passphrase --page -1
```

## License

TBD, mostly Apache2