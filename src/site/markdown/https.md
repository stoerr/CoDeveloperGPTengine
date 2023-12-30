# Making the toolbench available via https

When the toolbench is used from within ChatGPT, it has to be reachable from ChatGPT's servers. That is, it has to be
reachable from the internet via https via the standard port 443 and has to have a fixed hostname.

There are various ways to do this, depending on your setup. The following sections describe some of them.
[Serveo](https://serveo.net/) is probably the easiest way to get started. For some reason I couldn't get it to work
as a ChatGPT plugin when using Serveo and LocalTunnel as a plugin, but they do work as a GPT, which seems to be
preferred by OpenAI anyway. With the Fritz.Box solution it works for me as a ChatGPT plugin, too.

## [Serveo](https://serveo.net/)

The donation based https://serveo.net/ allows exporting a port to the internet and handles the certificate for you.
Pick a domain prefix that is not common since everybody can use it.
Each time before using the toolbench you have to start the serveo tunnel:

    ssh -T -R your-desired-domain-prefix.serveo.net:80:localhost:3002 serveo.net

Your toolbench URL for ChatGPT will be https://your-desired-domain-prefix.serveo.net/ in this example. To check whether
it works you can call it up in a browser, after starting the developers toolbench.

### [LocalTunnel](https://theboroer.github.io/localtunnel-www/)

[LocalTunnel](https://theboroer.github.io/localtunnel-www/) is similar to serveo. You have to install a client, and then
set up a tunnel. Pick an uncommon prefix, since everybody can use it.

    lt -s your-desired-domain-prefix -p 3002

Your toolbench URL for ChatGPT will be your-desired-domain-prefix.loca.lt in this example.

## Using paid services

There are several paid services similar to Serveo and LocalTunnel. I haven't tried them, but they should work, too:
[ngrok.com](https://ngrok.com/) (at least $8/month), https://pagekite.net/ (>=$3/month), [inlets](https://inlets.dev/) (
$19/month).

## You work on a computer with a fixed hostname that is reachable from the internet

The problem is here that you have to make the toolbench reachable via port 443, and the toolbench cannot be run as root.
So you probably have to set up a reverse proxy or something like that that forwards the requests to the toolbench.
Please [contact me](https://www.stoerr.net/contact.html)
if you found a solution I could publish here. Also, you have to create a certificate for your hostname.
If you don't have an easy solution for these topics, you can still use one of the other ways.

## Local Fritz.box using DSL or similar

If you have a dialup account from home, that obviously depends on your local setup. If you have a DSL connection and a
Fritz.Box like I do, you could do the following:

- Set up a DynDNS account like [freeddns.org](https://freeddns.dynu.com/) and configure your Fritz.Box to use it.
- Configure your Fritz.Box to forward port 443 to your local machine with a suitable port, e.g. 3003 for the toolbench's
  mode

You need to get a certificate for your hostname, e.g. with https://letsencrypt.org/. I did it, but
since all that is a bit complicated, I'm only describe this if you ask me. :-)  One advantage is that you don't need to
remember to start and stop a tunnel.
