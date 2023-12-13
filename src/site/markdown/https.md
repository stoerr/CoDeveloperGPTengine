# Making the toolbench available via https

When the toolbench is used from within ChatGPT, it has to be reachable from ChatGPT's servers. Therefore it has to be
reachable from the internet using HTTPS. Thus you have to solve 2 problems:

1. Have a fixed public hostname (even if you're using DSL) and export the programs https port through the firewall, if
   present
2. Create and use an appropriate certificate

## Fixed hostname and exported https port

If you already are on a host that has a fixed public DNS name and is reachable from the internet, you can skip this
step.

### Local Fritz.box using DSL or similar

If you have a dialup account from home, that obviously depends on your local setup. If you have a DSL connection and a
Fritz.Box like I do, you could try the following:

### Serveo

https://serveo.net/ should work if you create a certificate with letsencrypt.

### LocalTunnel

[LocalTunnel](https://theboroer.github.io/localtunnel-www/) may or may not able to give you the same URL each time; you
need to handle your own certificate. [src](https://github.com/TheBoroer/localtunnel-server)

## Get a https certificate



## Using paid services

There are some paid services that allow you to forward your port to the internet and also handle https. That
scenario is not yet currently supported, only handling your own certificate - please 
[contact me](http://www.stoerr.net/contact.html) if you want that.

[ngrok.com](https://ngrok.com/) also provides a way to export your toolbench port via https that also works when you're
in a network that allows internet access but does not allow you to forward ports easily. The nice thing is it handles
the https and certificate part for you, but to have a fixed URL you need a paid accound (at least $8/month)

https://pagekite.net/ allows setting up a tunnel so that your server is reachable via https, similarily to ngrok, >
=$3/month

[inlets](https://inlets.dev/) would likely work, too, but is $19/month .
