package net.stoerr.chatgpt.devtoolbench;

import static spark.Spark.*;

public class DevToolbench {

    public static void main(String[] args) {
        get("/", (req, res) -> "Hello World!");
    }

}