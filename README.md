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
  * BASELINE-LT
  * BASELINE-LTA

## Get Started

Download the latest JAR from the [GitHub releases page](https://github.com/open-pdf-sign/open-pdf-sign/releases) or in your terminal:

```shell
curl --location --output open-pdf-sign.jar \
  https://github.com/open-pdf-sign/open-pdf-sign/releases/latest/download/open-pdf-sign.jar
```

Alternatively, open-pdf-sign is also available on [nix](https://github.com/NixOS/nixpkgs/tree/master/pkgs/tools/misc/open-pdf-sign),
a wrapper is available on [npm](https://www.npmjs.com/package/open-pdf-sign), and alongside a installer for [nginx](https://github.com/open-pdf-sign/open-pdf-sign-configurator).

Make sure that Java is installed in at least version 11.

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
  --add-page
    add a blank page to the end of the document before signing
  --baseline-lt
    use PAdES profile with long-term validation material
  --baseline-lta
    use PAdES profile with long term availability and integrity of validation material
  -b, --binary
    binary output of PDF
    Default: false
  -c, --certificate
    certificate (chain) to be used
  --certification
      Quality of signature certification (DocMDP) and allowed changes after
      signing
      Default: certified-minimal-changes-permitted
      Possible Values: [not-certified, certified-no-change-permitted, certified-minimal-changes-permitted, certified-changes-permitted]
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
  --image-only
    Only use the image as signature content
    Default: false
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
  --pdf-passphrase
    Password required for reading a password-protected PDF input file
  --port
    run as server with the given port
  --signature-contact
    Contact information of the signer
  --signature-location
    The signer's location
  --signature-reason
    The signature creation reason
  --timestamp
    include signed timestamp
    Default: false
  --timezone
    use specific timezone for time info, e.g. Europe/Vienna
  --top
    Y coordinate of the signature block in cm
    Default: 1.0
  --tsa
    use specific time stamping authority (TSA) as source (if multiple given, will
    be used in given order as fallback)
    Default: []
  --tsa-username
    username for TSA server
  --tsa-password
    password for TSA server
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

### Signing documents with long-term validation info (PAdES-LT)

Sign documents with signatures that provides the long-term availability 
of the validation material by incorporating all the material 
or references to material required for validating the signature.  
For this, using a timestamp is needed.

```shell
java -jar open-pdf-sign.jar --input input.pdf --output output.pdf \
  --certificate /etc/letsencrypt/live/openpdfsign.org/fullchain.pem \
  --key /etc/letsencrypt/live/openpdfsign.org/privkey.pem \
  --timestamp --tsa http://timestamp.digicert.com
  --baseline-lt
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
* JDK 11

### Build

```shell
mvn package
```

## License

This project is licensed under the [Apache 2.0-License](LICENSE).  
The code contained in the [org/openpdfsign/dss subfolder](https://github.com/open-pdf-sign/open-pdf-sign/tree/master/src/main/java/org/openpdfsign/dss)
extends and modifies code from the [dss project](https://github.com/esig/dss/) which is licensed under the [LGPL-2.1 license](https://github.com/esig/dss/blob/master/LICENSE).  

This project received financial support from [netidee](https://www.netidee.at/open-pdf-sign).
