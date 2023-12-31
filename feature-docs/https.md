# Using with https

We try to use https using a certificate of letsencrypt , so that it'd work from ChatGPT GPTs or plugin store.

## Install [certbot](https://certbot.eff.org/)

MacOS with homebrew : brew install certbot
Testing: https://letsencrypt.org/docs/staging-environment/ argument --test-cert for certbot

## Basic idea

We assume that the plugin is running on the local machine in various directories and that it's port is either 
accessible from the public internet or that there is port forwarding through a router configured with DynDNS, or a 
reverse proxy or a firewhal etc.  In any case, the plugin is accessible from the public internet.

Since the plugin is running on the local machine in various directories, yet the https configuration is specific to 
the whole machine, we store the certificates and configuration in a directory ~/.cgptcodeveloperglobal/https/ .

[HTTP-01](https://letsencrypt.org/docs/challenge-types/) challenge needs putting the challenge to
http://<YOUR_DOMAIN>/.well-known/acme-challenge/<TOKEN>
and thus needs to be served at port 80 of the domain. That's not compatible with the work of the plugin, so we let the
certificate be created another way and just use it.

## How to create certificate with certbot , setting with a Fritz Box or other router that supports port forwarding

For creating the certificate, we run certbot at port 3005, not as root and save the files in ~/.letsencrypt/certbot/ .
The router is temporarily set up to forward port 80 to port 3005 of the machine running the plugin. 

That needs the following certbot command:

certbot certonly --standalone --preferred-challenges http --http-01-port CERTBOTPORT -d YOURDOMAIN \
    --config-dir ~/.letsencrypt/certbot/config --work-dir ~/.letsencrypt/certbot/work \
    --logs-dir ~/.letsencrypt/certbot/logs --non-interactive --agree-tos --email YOUREMAIL

The certificates are stored in ~/.letsencrypt/certbot/config/live/YOURDOMAIN/ .

For Jetty we need a keystore. This requires a password being in ~/.cgptcodeveloperglobal/https/keystore.p12.password .

openssl pkcs12 -export -in ~/.letsencrypt/certbot/config/live/stoerr.freeddns.org/fullchain.pem \
    -inkey ~/.letsencrypt/certbot/config/live/stoerr.freeddns.org/privkey.pem \
    -out ~/.cgptcodeveloperglobal/https/keystore.p12 -name jetty \
    -CAfile ~/.letsencrypt/certbot/config/live/stoerr.freeddns.org/chain.pem -caname root \
    -password file:$HOME/.cgptcodeveloperglobal/https/keystore.p12.password

## Security of the plugin

Since the plugin is now accessed from the internet, we need some kind of authentication. TODO: how to do that? 

## Extending the plugin for SSL

We need a common storage place for the configuration that is accessible from all the directories where the plugin is 
run - ~/.cgptcodeveloperglobal/ . We read a https.properties from there that contains the following properties:
httpsport=3003
keystorepath=keystore.p12
keystorepasswordpath=keystore.p12.password

https://chat.openai.com/share/b33c38e7-0ed0-4f5e-942a-01818fe2cc83

## User Level Auth

See https://www.hackwithgpt.com/blog/chatgpt-plugin-authentication-guide

This would be exactly the right thing, but doesn't seem to work, so we have to use service level auth:

    "auth": {
      "type": "user_http",
      "authorization_type": "bearer",
    },
