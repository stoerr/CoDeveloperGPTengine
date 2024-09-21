# Configuration

There are two places where configuration can be stored.

First, the tool looks for a global configuration file in
`$HOME/.cgptcodeveloperglobal/config.properties` with properties that are likely identical wherever you call the tool.
<!-- FIXME insert after next release: 
For special cases (such as running the tool in a docker container that has no access to your home directory)
that can be overridden by a file `.cgptcodeveloper/config.properties` in the directory the engine is started in. 
-->

Second, you can store shell scripts in the directory `.cgptcodeveloper` that can be used as actions that can be
executed by ChatGPT - for instance triggering a build. See below.

## The config.properties

In the simplest case like in [quickstart](quickstart.md) where you are using 
the engine within an OpenAI GPT and with a [https tunnel](https.md), then you will only need the property
*`gptsecret`* with a secret you choose that OpenAI authentication will use to authenticate itself to your engine.
It's use is described in the [GPT setup](gpt.md). Example `config.properties`:

    gptsecret=kreuU7la9+fewk4)x.Q!

Mostly obsolete: if you use it as a ChatGPT plugin, ChatGPT will give you an OpenAI token during plugin registration
*`openaitoken`* that should be put there.

If you run it directly with [https using your own certificate](https.md) instead of using a https tunnel, there are
properties `httpsport` for the
HTTPS port the engine should use, `keystorepath` and `keystorepassword` for the keystore, or `keystorepasswordpath`
with a file containing the password, and the `domain` the engine is reachable with at port 443. So the
`config.properties` could look like this:

    httpsport=3003
    keystorepath=https/keystore.p12
    keystorepasswordpath=https/keystore.p12.password
    domain=yourhostname.freeddns.org
    gptsecret=848KkaaASSDAkkD7/k(f
    openaitoken=84573967bd28c28578488277ff732834

## The scripts in `.cgptcodeveloper/`

Any shell script called `*.sh` in the directory `.cgptcodeveloper/` can be called by name as action from ChatGPT.
This gives you the possibility to easily extend the functionality of the engine by adding new actions.
Please see [external actions](externalActions.md) for more information.
