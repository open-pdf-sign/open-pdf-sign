# open-pdf-sign

The `open-pdf-sign` CLI application allows to easily sign PDF files from the command line. 
Signatures can be invisible (default) or visible (can be customized). 

## Features

* Visible PDF signature in PDF (multi language support)
* Invoke via CLI or via starting a server
* Supported signature type: PAdES
* Supported signature profiles: 
  * BASELINE-B
  * BASELINE-T
  * To be evaluated: BASELINE-LT, BASELINE-LTA

## Get Started

Download the latest JAR from the [GitHub releases page](https://github.com/open-pdf-sign/open-pdf-sign/releases) or in your terminal:

```shell
curl --location --output open-pdf-sign.jar \
  https://github.com/open-pdf-sign/open-pdf-sign/releases/latest/download/open-pdf-sign.jar
```

Alternatively, open-pdf-sign is also available on [nix](https://github.com/NixOS/nixpkgs/tree/master/pkgs/tools/misc/open-pdf-sign),
a wrapper is available on [npm](https://www.npmjs.com/package/open-pdf-sign), and alongside a installer for [nginx](https://github.com/open-pdf-sign/open-pdf-sign-configurator).

Make sure that Java is installed in at least version 8.

### Run

```shell
java -jar open-pdf-sign.jar \
  --input input.pdf --output output.pdf \
  --certificate certificate.crt --key keyfile.pem --passphrase key_passphrase \
  --page -1 --locale de-AT
```

Usage:

```text
Options:
  -b, --binary
    binary output of PDF
    Default: false
  -c, --certificate
    certificate (chain) to be used
  --config
    use a configuration file
  -h, --help
    prints this page
  --hint
    text to be displayed in signature field
  --host
    run as server with the given hostname
  --image
    Image to be placed in signature block
  --just-image
	  Just insert the signature image to the PDF file (the PDF will not be digitally signed). Use --left, --top and --width to place the image.
  -i, --input
    input pdf file
  -k, --key
    signature key file or keystore
  --label-hint
    label for the 'hint' row
  --label-signee
    label for the 'signee' row
  --label-timestamp
    label for the 'timestamp' row
  --left
    X coordinate of the signature block in cm
    Default: 1.0
  -l, --locale
    Locale, e.g. de-AT
  --no-hint
    don't display a hint row
  -o, --output
    output pdf file
  --page
    Page where the signature block should be placed. [-1] for last page
  -p, --passphrase
    passphrase for the signature key or keystore
  --port
    run as server with the given port
  --timestamp
    include signed timestamp
    Default: false
  --timezone
    use specific timezone for time info, e.g. Europe/Vienna
  --top
    Y coordinate of the signature block in cm
    Default: 1.0
  --tsa
    use specific time stamping authority as source (if multiple given, will
    be used in given order as fallback)
    Default: []
  --version
    prints version of this program
  --width
    width of the signature block in cm
    Default: 10.0
```

### Usage with Let's Encrypt certificates

PDFs can also be signed using your existing Let's Encrypt certificate.

```shell
java -jar open-pdf-sign.jar --input input.pdf --output output.pdf \
  --certificate /etc/letsencrypt/live/openpdfsign.org/fullchain.pem \
  --key /etc/letsencrypt/live/openpdfsign.org/privkey.pem
```

### Visible signatures

If the `page` parameter is specified, a visible signature will be placed on the specified page. 
For example, running

```shell
java -jar open-pdf-sign.jar --input input.pdf --output output.pdf \
     --certificate certificate.crt \
     --key key.pem \
     --page -1 --image mylogo.png \
     --hint "You can check the validity at https://www.signaturpruefung.gv.at"
```

will place a visible signature looking similar to the image below on the last page (`-1`) of the PDF document.

![signature image](https://www.openpdfsign.org/images/signature.png)


### Usage in server mode

You can also run open-pdf-sign as a server application in order to only load certificates once and easily integrate it in applications where CLI invocations are not possible. 
Simply add the `port` or `host` parameters, e.g.

```shell
java -jar open-pdf-sign.jar --input input.pdf --output output.pdf \
  --certificate /etc/letsencrypt/live/openpdfsign.org/fullchain.pem \
  --key /etc/letsencrypt/live/openpdfsign.org/privkey.pem
  --port 8090 --host 127.0.0.1
```

Then, PDFs can be signed via the [specified](src/main/resources/openapi.yml) POST request:

```shell
curl --location 'http://localhost:8090/' \
  --header 'Content-Type: application/json' \
  --data-raw '{"input":"/path/to/pdf.pdf"}'
```

### Using a config file

Instead of specifying everything via CLI parameters, you can also use a configuration file (e.g. [this one](src/test/resources/test-config.yml)):

```shell
java -jar open-pdf-sign.jar --config /path/to/config.yaml
```

This way, you could also configure multiple (virtual) hosts.

## Development

### Requirements

* [Maven](https://maven.apache.org/)
* JDK 8

### Build

```shell
mvn package
```

## License

This project is licensed under the [Apache 2.0-License](LICENSE).  
The code contained in the [org/openpdfsign/dss subfolder](https://github.com/open-pdf-sign/open-pdf-sign/tree/master/src/main/java/org/openpdfsign/dss)
extends and modifies code from the [dss project](https://github.com/esig/dss/) which is licensed under the [LGPL-2.1 license](https://github.com/esig/dss/blob/master/LICENSE).  

This project received financial support from [netidee](https://www.netidee.at/open-pdf-sign).
