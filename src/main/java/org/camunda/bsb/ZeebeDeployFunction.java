package org.camunda.bsb;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zeebe.client.ClientProperties;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.event.DeploymentEvent;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ZeebeDeployFunction implements RequestHandler<ApiGatewayProxyEvent, ApiGatewayResponse> {

    private static final Logger LOG = Logger.getLogger(ZeebeDeployFunction.class);

    @Override
    public ApiGatewayResponse handleRequest(ApiGatewayProxyEvent event, Context context) {

        try {

            final String BROKER_ADDRESS = "ec2-52-58-170-237.eu-central-1.compute.amazonaws.com:51015";

            ObjectMapper mapper = new ObjectMapper();
            Request request = new Request();

            request = mapper.readValue(event.getBody(), Request.class);

            String url = request.getUrl();

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

            final DeploymentEvent deployment = client.workflows()
                    .deploy("default-topic")
                    .resourceStream(inputStream)
                    .execute();


            LOG.debug("Disconnecting from Zeebe broker on " + BROKER_ADDRESS);

            client.disconnect();

            // Build the GatewayResponse

            Response responseBody = new Response("Zeebe deploy from " + url + " finished successfully");
            Map<String, String> headers = new HashMap<>();
            headers.put("X-Powered-By", "AWS Lambda & Serverless");
            headers.put("Content-Type", "application/json");

            return ApiGatewayResponse.builder()
                    .setStatusCode(200)
                    .setObjectBody(responseBody)
                    .setHeaders(headers)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();

            return ApiGatewayResponse.builder()
                    .setStatusCode(500)
                    .build();
        }

    }

}