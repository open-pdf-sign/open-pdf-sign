# open-pdf-sign

The `open-pdf-sign` CLI application allows to
easily sign PDF files from the commandline. Signatures
can be invisible or visible, and visible signatures can be 
customized. 

## Features
* Visible PDF signature in PDF (multi language support)
* Invoke via CLI or via starting a server
* Supported Signature type: PAdES
* Supported Signature profiles: 
  * BASELINE-B
  * BASELINE-T
  * To be evaluated: BASELINE-LT, BASELINE-LTA

## Get Started

### With Nix

Since this pull request, the project is available through [Nix][nix website].

Nix can be summarized as a cross-platform universal package manager that utilizes a
purely functional deployment model where software is installed into unique 
directories generated through cryptographic hashes.

You can either create a temporary development shell where the `open-pdf-sign`
command will be available by doing:

```shell
nix-shell -p open-pdf-sign
open-pdf-sign -i input.pdf -o output.pdf -c certificate.crt -k keyfile.pem -p key_passphrase --page -1 --locale de-AT
```

As an alternative and if you enable the upcoming [`flake feature`][nix flake feature],
you can remotely run `open-pdf-sign` by doing:

```shell
nix run github:Nixos/nixpkgs#open-pdf-sign -- -i input.pdf -o output.pdf -c certificate.crt -k keyfile.pem -p key_passphrase --page -1 --locale de-AT
```

### Without Nix

Download the latest jar archive from the [GitHub releases page](https://github.com/open-pdf-sign/open-pdf-sign/releases) or in your terminal:

```bash
curl -L https://github.com/open-pdf-sign/open-pdf-sign/releases/latest/download/open-pdf-sign.jar -o open-pdf-sign.jar
```

Make sure that Java is installed in at least version 8.

### Run
```
java -jar open-pdf-sign.jar -i input.pdf -o output.pdf -c certificate.crt -k keyfile.pem -p key_passphrase --page -1 --locale de-AT
```

Usage:
```
  Options:
    -b, --binary
      binary output of PDF
      Default: false
    -c, --certificate
      certificate (chain) to be used
    --config
      use a configuration file
    --hint
      text to be displayed in signature field
    --host
      run as server with the given hostname
    --image
      Image to be placed in signature block
    -i, --input
      input pdf file
    -k, --key
      signature key file or keystore
    --left
      X coordinate of the signature block in cm
      Default: 1.0
    -l, --locale
      Locale, e.g. de-AT
    -o, --output
      output pdf file
    --page
      Page where the signature block should be placed. [-1] for last page
    -p, --passphrase
      passphrase for the signature key or keystore
    --port
      run as server with the given port
      Default: 8090
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
    --width
      width of the signature block in cm
      Default: 10.0
```

### Usage with Let's Encrypt certificates

PDFs can also be signed using your existing Let's Encrypt certificate.

```bash
java -jar open-pdf-sign.jar -i input.pdf -o output.pdf \
  -c /etc/letsencrypt/live/openpdfsign.org/fullchain.pem \
  -k /etc/letsencrypt/live/openpdfsign.org/privkey.pem
```

### Visible signatures

If the `page` parameter is specified, a visible signature
will be placed on the specified page. For example, running

```bash
java -jar open-pdf-sign.jar -i input.pdf -o output.pdf \
     -c certificate.crt \
     -k key.pem \
     --page -1 --logo mylogo.png \
     --hint "You can check the validity at signaturpruefung.gv.at"
```

will place a visible signature looking similar to the image below
on the last page (`-1`) of the PDF document.

![signature image](https://www.openpdfsign.org/images/signature.png)


### Usage in server mode

You can also run open-pdf-sign as a server application in order to
only load certificates once and easily integrate it in applications where
CLI invocations are not possible. Simply add the `--port` or `--hostname`
parameters, e.g.

```bash
java -jar open-pdf-sign.jar -i input.pdf -o output.pdf \
  -c /etc/letsencrypt/live/openpdfsign.org/fullchain.pem \
  -k /etc/letsencrypt/live/openpdfsign.org/privkey.pem
  --port 8090 --hostname 127.0.0.1
```

Then, PDFs can be signed via the [specified](src/main/resources/openapi.yml) `/pdf`
endpoint:

```bash
curl --location --request POST 'http://localhost:8090/' \
--header 'Content-Type: application/json' \
--data-raw '{
  "input": "/path/to/pdf.pdf"
}'
```

### Using a config file

Instead of CLI parameters, you can also submit a configuration file with
the same parameters and the possibility to lead multiple
keys, as shown in [this example](src/test/resources/test-config.yml)

```bash
java -jar open-pdf-sign.jar --config /path/to/config.yaml
```

## Development

### Requirements
* Maven (https://maven.apache.org/)
* JDK 8


### Build

```bash
mvn package
```

## License

This project is licensed under the Apache 2.0-License.  
The code contained in the [org/openpdfsign/dss subfolder](https://github.com/open-pdf-sign/open-pdf-sign/tree/master/src/main/java/org/openpdfsign/dss)
extends and modifies code from the [dss project](https://github.com/esig/dss/) which is licensed under the [LGPL-2.1 license](https://github.com/esig/dss/blob/master/LICENSE).

[nix website]: https://nixos.org/
[nix flake feature]: https://nixos.wiki/wiki/Flakes
