package net.stoerr.chatgpt.devtoolbench;

import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

public class DevToolbenchIT {

    @Test
    public void testServer() throws Exception {
        int port = new Random().nextInt(10000) + 10000;
        DevToolbench.main(new String[]{String.valueOf(port)});

        URL url = new URL("http://localhost:" + port + "/nothing");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        StringBuilder result = new StringBuilder();
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();

        Assert.assertEquals("Unknown request", result.toString());
    }
}
