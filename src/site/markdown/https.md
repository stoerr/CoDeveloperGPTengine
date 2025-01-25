# Making the engine available via https

When the engine is used from within ChatGPT, it has to be reachable from ChatGPT's servers. That is, it has to be
reachable from the internet via https via the standard port 443 and has to have a fixed hostname.

There are various ways to do this, depending on your setup. The following sections describe some of them.
[Serveo](https://serveo.net/) is probably the easiest way to get started. For some reason I couldn't get it to work
as a ChatGPT plugin when using Serveo and LocalTunnel as a plugin, but they do work as a GPT, which seems to be
preferred by OpenAI anyway. With the Fritz.Box solution it works for me as a ChatGPT plugin, too.

## [Serveo](https://serveo.net/)

The donation based https://serveo.net/ allows exporting a port to the internet and handles the certificate for you.
Pick a domain prefix that is not common since everybody can use it.
Each time before using the engine you have to start the serveo tunnel:

    ssh -T -R your-desired-domain-prefix.serveo.net:80:localhost:3002 serveo.net

Your engine URL for ChatGPT will be https://your-desired-domain-prefix.serveo.net/ in this example. To check whether
it works you can call it up in a browser, after starting the developers engine.

See
[examples/config/tunnel.sh](https://github.com/stoerr/CoDeveloperGPTengine/tree/develop/examples/config/tunnel.sh)
for an example how to start the tunnel automatically from the
[codeveloperengine](https://github.com/stoerr/CoDeveloperGPTengine/blob/develop/bin/codeveloperengine) script.

## Using paid services

There are several paid services similar to Serveo and LocalTunnel. I haven't tried them, but they should work, too:
[ngrok.com](https://ngrok.com/) (at least $8/month), https://pagekite.net/ (>=$3/month), [inlets](https://inlets.dev/) (
$19/month).

## You work on a computer with a fixed hostname that is reachable from the internet

The problem is here that you have to make the engine reachable via port 443, and the engine cannot be run as root.
So you probably have to set up a reverse proxy or something like that that forwards the requests to the engine.
Please [contact me](https://www.stoerr.net/contact.html)
if you found a solution I could publish here. Also, you have to create a certificate for your hostname.
If you don't have an easy solution for these topics, you can still use one of the other ways.

## Local Fritz.box using DSL or similar

If you have a dialup account from home, that obviously depends on your local setup. If you have a DSL connection and a
Fritz.Box like I do, you could do the following:

- Set up a DynDNS account like [freeddns.org](https://freeddns.dynu.com/) and configure your Fritz.Box to use it.
- Configure your Fritz.Box to forward port 443 to your local machine with a suitable port, e.g. 3003 for the engine's
  mode

You need to get a certificate for your hostname, e.g. with https://letsencrypt.org/. I did it, but
since all that is a bit complicated, I'm only going to describe this if you ask me. :-)  One advantage is that you 
don't need to remember to start and stop a tunnel.

## SSH port forwarding + Reverse proxy

If you have a host in the cloud that has a wildcard SSL certificate and a web server like apache that supports
creating virtual hosts and have set up passwordless login with ssh public key authentication, then you can:

1. set up a new virtual host with a reverse proxy that goes to your cloud host, e.g. port 3002
2. start ssh with forwarding that remote port to your local host, e.g.
   `ssh -T -N -R 3002:localhost:3002 you@yourcloudhost.whereever`

For the virtual host you'll probably need to set up a DNS CNAME record that maps that virtual host to your main host.
For Apache 2 a site setup would be e.g.

```
<VirtualHost *:443>
    ServerName yourvirtualhost.your.domain

    SSLEngine On
    SSLCertificateFile /etc/ssl/certs/your.domain_ssl_certificate.cer
    SSLCertificateKeyFile /etc/ssl/private/_.your.domain_private_key.key
    SSLCACertificateFile /etc/ssl/certs/_.your.domain_ssl_certificate_INTERMEDIATE.cer
    Include /etc/letsencrypt/options-ssl-apache.conf

    ProxyPreserveHost On
    ProxyPass / http://localhost:3002/
    ProxyPassReverse / http://localhost:3002/
</VirtualHost>
```

## Services that do (probably) not work

If you feel like experimenting: [LocalTunnel](https://theboroer.github.io/localtunnel-www/) might or might not
work - as of 1/2024 I couldn't get it to work anymore, it fails silently in ChatGPT without any error message in 
ChatGPT. Perhaps it'll work later again.
The command line that previously was working is this for an engine URL of your-desired-domain-prefix.loca.lt :

    lt --print-requests=true -s your-desired-domain-prefix -p 3002

<!-- 
The problem is probably that it requires entering a password if you use it
from a browser - except if the UserAgent header is set to something unusual. Not quite sure what ChatGPT sets as an
UserAgent, probably that isn't counted as unusual anymore. -->
