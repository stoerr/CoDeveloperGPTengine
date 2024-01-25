# Debugging hints

It's possible to run the application from the IDE when setting the working directory to whereever you want access - main
class is net.stoerr.chatgpt.codevengine.CoDeveloperEngine . You'll need to run the tunnel by hand, though -
normally $HOME/.cgptcodeveloper/tunnel.sh .

The requests that come in are logged to stdout; if there is a file .cgptcodeveloper/.requestlog.txt they are also logged
there. If a request goes wrong for some reason, it's possible to repeat that from the browser. There is however the
authentication issue - we require from ChatGPT an authentication token that isn't present for the browser. Therefore
there is a workaround in UserGlobalConfig.java that also allows using a cookie `CodevAccessToken` with the value you've
chosen for the gptsecret and put into your $HOME/.cgptcodeveloperglobal/config.properties . To set the cookie, you can
call up the /debugging/setauthcookie.html from the engine URL to set the cookie.
