package jlambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.commons.codec.digest.DigestUtils;

public class ApiGatewayHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        String token =null;
        String result =null;
        LambdaLogger log = context.getLogger();
        String data = input.getBody();
        log.log("handleRequest input="+data);
        Json j = Json.read(data);

        if (j.has("token")) {

            token= j.at("token").asString();
        }

        String  md5HexString = DigestUtils.md5Hex(token);

        if(md5HexString.equals(Krypto.DIGEST.getartifact())) {
            result = "token authenticated ok";
        } else {
            result = "token not authentic";
        }
        log.log("handleRequest result="+result);

        String tokenRet ="Is secure token? "+result;


        return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody(tokenRet );
    }



}
