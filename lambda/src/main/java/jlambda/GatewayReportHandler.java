package jlambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import jlambda.layer.ReportGenerator;

import java.io.IOException;

public class GatewayReportHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

   private  LambdaLogger log;

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        String token =null;
        log = context.getLogger();

        Json j = Json.read(input.getBody());
        if (j.has("token")) {

            token= j.at("token").asString();
        }
        Boolean authenticToken = Krypto.INSTANCE.authenticateToken(token);

        if(authenticToken) {
            try {
                handleReport(j);
            } catch (IOException e) {
                log.log("handleReport  IOException "+e.getLocalizedMessage() );

            }
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("Is secure token? "+authenticToken);
        } else {
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("Is secure token? "+authenticToken);
        }

    }

    private void handleReport(Json j) throws IOException {

        String ret = ReportGenerator.INSTANCE .runSimpleReport("Lambda Report");
        if(ret == null) {
            log.log("handleReport  s3 issues exist "+ret);
        } else {
            log.log("handleReport made successful S3 put "+ret);
        }
    }





}
