package org.camunda.bsb;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zeebe.client.ClientProperties;
import io.zeebe.client.ZeebeClient;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ZeebeDeployFunction implements RequestHandler<ApiGatewayProxyEvent, ApiGatewayResponse> {

    private static final Logger LOG = Logger.getLogger(ZeebeDeployFunction.class);

    @Override
    public ApiGatewayResponse handleRequest(ApiGatewayProxyEvent event, Context context){

        final String BROKER_ADDRESS = "ec2-52-58-170-237.eu-central-1.compute.amazonaws.com:51015";

            ObjectMapper mapper = new ObjectMapper();
            Request request = new Request();

        try {
            request = mapper.readValue(event.getBody(), Request.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String url = request.getUrl();

        LOG.debug("Getting BPMN model from " + url);

        // TODO get bpmn file from github

        LOG.debug("Building Zeebe client ...");

        Properties clientProperties = new Properties();

        clientProperties.put(ClientProperties.BROKER_CONTACTPOINT, BROKER_ADDRESS);

        ZeebeClient client = ZeebeClient.create(clientProperties);

        LOG.debug("Connecting to Zeebe broker on " + BROKER_ADDRESS);

        client.connect();


        LOG.debug("Disconnecting from Zeebe broker on " + BROKER_ADDRESS);

        client.disconnect();

        // Build the GatewayResponse

        Response responseBody = new Response("Zeebe deploy from " + url +" finished successfully");
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Powered-By", "AWS Lambda & Serverless");
        headers.put("Content-Type", "application/json");

        return ApiGatewayResponse.builder()
                .setStatusCode(200)
                .setObjectBody(responseBody)
                .setHeaders(headers)
                .build();
    }

}