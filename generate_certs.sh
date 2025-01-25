# This file generates server-side self-signed certificates
ENVIRONMENT="testlocal"

mkdir $ENVIRONMENT && cd $ENVIRONMENT

openssl req -x509 \
            -sha256 -days 3650 \
            -nodes \
            -newkey rsa:2048 \
            -subj "/CN=device-grpc.$ENVIRONMENT/C=US/L=Chicago" \
            -keyout rootCA.key -out rootCA.crt

openssl genrsa -out server.key.rsa 2048
openssl pkcs8 -topk8 -in server.key.rsa -out server.key -nocrypt
openssl req -new -key server.key -out server.csr -subj "/CN=device-grpc.$ENVIRONMENT/C=US/L=Chicago"

cat > csr.conf <<EOF
[ req ]
default_bits = 2048
prompt = no
default_md = sha256
req_extensions = req_ext
distinguished_name = dn

[ dn ]
C = US
ST = Illinois
L = Chicago
O = Foo
OU = Foo SWS
CN = device-grpc.$ENVIRONMENT

[ req_ext ]
subjectAltName = @alt_names

[ alt_names ]
DNS.1 = device-grpc.$ENVIRONMENT
DNS.2 = www.device-grpc.$ENVIRONMENT
IP.1 = 111.111.111.111
EOF

openssl req -new -key server.key -out server.csr -config csr.conf

cat > cert.conf <<EOF
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage = digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment
subjectAltName = @alt_names

[alt_names]
DNS.1 = device-grpc.$ENVIRONMENT
EOF

openssl x509 -req \
    -in server.csr \
    -CA rootCA.crt -CAkey rootCA.key \
    -CAcreateserial -out server.crt \
    -days 3650 \
    -sha256 -extfile cert.conf