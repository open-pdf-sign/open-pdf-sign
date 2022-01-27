# open-pdf-sign

CLI application for signing PDF files.
## Features
* Visible PDF signature in PDF (multi language support)
* Supported Signatures:
* NGINX support to serve all files digitally signed

## Get Started

### Requirements
* Maven (https://maven.apache.org/)
* JDK 8

### Run
```
java -jar open-pdf-sign.jar -i input.pdf -o output.pdf -c certificate.crt -k keyfile.pem -p key_passphrase --page -1 --locale de-AT
```

## Development
### Build

```bash
mvn package
```

## License

TBD, mostly Apache2
