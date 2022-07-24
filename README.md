# open-pdf-sign

CLI application for signing PDF files.

## Features
* Visible PDF signature in PDF (multi language support)
* Supported Signature type: PAdES
* Supported Signature profiles: 
  * BASELINE-B
  * BASELINE-T
  * To be evaluated: BASELINE-LT, BASELINE-LTA

## Get Started

Download the latest jar archive from the [GitHub releases page](https://github.com/open-pdf-sign/open-pdf-sign/releases)

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
    --hint
      text to be displayed in signature field
    --image
      Image to be placed in signature block
  * -i, --input
      input pdf file
  * -k, --key
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
    -v, --verbose
      verbose output
      Default: false
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
will be placed on the specified page.

## Development

### Requirements
* Maven (https://maven.apache.org/)
* JDK 8


### Build

```bash
mvn package
```

## License

TBD, mostly Apache2
