package org.camunda.bsb;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zeebe.client.ClientProperties;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.event.DeploymentEvent;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ZeebeDeployFunctionTest {

    private static final Logger LOG = Logger.getLogger(ZeebeDeployFunctionTest.class);


    @Test
    public void testDeployFunction(){

        try {

            final String BROKER_ADDRESS = "ec2-52-57-80-74.eu-central-1.compute.amazonaws.com:51015";

            String url = "https://raw.githubusercontent.com/bullshit-bingo/aws-lambda-deploy-function/master/process.bpmn";

            LOG.debug("Getting BPMN model from " + url);

            // Get BPMN file from URL resource

            URL githubUrl = new URL(url);
            HttpURLConnection httpURLConnection = (HttpURLConnection) githubUrl.openConnection();
            Map<String, List<String>> githubHeaders = httpURLConnection.getHeaderFields();

            // If URL is getting 301 and 302 redirection HTTP code then get new URL link.
            for (String header : githubHeaders.get(null)) {
                if (header.contains(" 302 ") || header.contains(" 301 ")) {
                    url = githubHeaders.get("Location").get(0);
                    githubUrl = new URL(url);
                    httpURLConnection = (HttpURLConnection) githubUrl.openConnection();
                    githubHeaders = httpURLConnection.getHeaderFields();
                }
            }
            InputStream inputStream = httpURLConnection.getInputStream();


            LOG.debug("Building Zeebe client ...");

            Properties clientProperties = new Properties();

            clientProperties.put(ClientProperties.BROKER_CONTACTPOINT, BROKER_ADDRESS);

            ZeebeClient client = ZeebeClient.create(clientProperties);

            LOG.debug("Connecting to Zeebe broker on " + BROKER_ADDRESS);

            client.connect();

            // Deploy to Zeebe

            LOG.debug("Deploying BPMN file to Zeebe");

            final DeploymentEvent deployment = client.workflows()
                    .deploy("default-topic")
                    .resourceStream(inputStream)
                    .execute();


            LOG.debug("Disconnecting from Zeebe broker on " + BROKER_ADDRESS);

            client.disconnect();
        }catch (Exception e ){
            e.printStackTrace();

        }

    }


}
