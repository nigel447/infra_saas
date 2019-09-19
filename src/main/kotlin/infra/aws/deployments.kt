package infra.aws

import com.amazonaws.services.lambda.model.FunctionConfiguration
import com.amazonaws.services.lambda.model.PublishLayerVersionResult
import infra.aws.config.LocalStackAPITestParams
import infra.aws.lambda.LambdaProcessor
import infra.aws.lambda.LocalStackLambdaTestParams
import jlambda.ApiGatewayHandler
import jlambda.GatewayReportHandler
import org.apache.commons.logging.LogFactory

val appLogger = LogFactory.getLog("infra")

/**
 *  create a http proxy for https://jsonplaceholder.typicode.com/todos/1
 *  test in browser with https://restApiId.execute-api.us-east-2.amazonaws.com/<stage>/<path>/
 */
fun deployHttpApiGateway(isLocalTest: Boolean) {

    // run on aws not localstack
    ApiGateway.bootStrap(isLocalTest)

    // create the http api
    val createRet =
        ApiGateway.createApPI(LocalStackAPITestParams.API_HTTP_NAME.param, "this is a simple test for http integration")
    val restApiId = createRet.id
    appLogger.info("rest_api_id $restApiId")

    // set the initial path, every path part is a "resource" and will have an associated id
    val setUpPathPartRet = ApiGateway.setUpBaseResource(restApiId, LocalStackAPITestParams.API_PATH_PART.param)
    val resId = setUpPathPartRet.second
    appLogger.info("create-resource  rootResource.id ${setUpPathPartRet.first}  resourceId $resId")

    // set the rest verb for the path
    val setUpApiMethodRet = ApiGateway.setUpApiMethod(
        resId, restApiId, LocalStackAPITestParams.API_VERB.param,
        LocalStackAPITestParams.API_AUTH_TYPE.param
    )
    appLogger.info("put-method httpMethod ${setUpApiMethodRet.httpMethod} authorizationType ${setUpApiMethodRet.authorizationType} ")

    // set the response
    val putMethodRet = ApiGateway.putMethodResponse(
        "",
        resId, restApiId, LocalStackAPITestParams.API_VERB.param,
        LocalStackAPITestParams.API_HTTP_CODE.param
    )
    appLogger.info("put-method-response ${putMethodRet.statusCode}")

    // set the redirect to the actual code that generates the response body,
    val putHttpIntegRet = ApiGateway.httpIntegration(
        resId, restApiId, LocalStackAPITestParams.API_VERB.param,
        LocalStackAPITestParams.API_REST_ENDPOINT_URI.param
    )
    appLogger.info("put-integration httpMethod ${putHttpIntegRet.httpMethod}  uri ${putHttpIntegRet.uri}")

    // set the response from  the redirect t
    val putIntegrationResponseRequest = ApiGateway.putIntegrationResponse(
        resId, restApiId, LocalStackAPITestParams.API_VERB.param,
        LocalStackAPITestParams.API_HTTP_CODE.param,
        ""
    )
    appLogger.info("put-integration-response statusCode ${putIntegrationResponseRequest.statusCode}")

    ApiGateway.createDeployment(restApiId, LocalStackAPITestParams.API_STAGE.param)
    appLogger.info("test uri")
    appLogger.info(
        makeTestUri(
            restApiId,
            LocalStackAPITestParams.API_STAGE.param,
            LocalStackAPITestParams.API_PATH_PART.param
        )
    )

}

private fun makeTestUri(apiId: String, stage: String, path: String): String {
    //  https://restApiId.execute-api.us-east-2.amazonaws.com/<stage>/<path>/
    return "https://$apiId.execute-api.us-east-2.amazonaws.com/$stage/$path"

}


/**
 *  test for proxy
 *  headers
 *  accept":application/json
 *
 * Request Body
 * {"input":"this is a test","token":"pj'_$<D'c:dX*x%b*H4fgi{;Ct{oCy|*8L^$Z1rd`rvaSeD#<N)s`IS8%o4k0Ot@"}
 *
 * test with curl with uri https://<restApiId>.execute-api.us-east-2.amazonaws.com/<stage>/<path>/
 *
 * EG
 * curl -H "Content-Type: application/json" -X POST \
 * https://wbhwr7j3zf.execute-api.us-east-2.amazonaws.com/test/api_test_endpoint -d @test.json \
 * where test.json
 *
 *
 */
fun deployApiGatewayProxy(isLocalTest: Boolean, config: FunctionConfiguration) {

    // run on aws not localstack
    ApiGateway.bootStrap(isLocalTest)

    // create the api
    val createRet =
        ApiGateway.createApPI(
            LocalStackAPITestParams.API_LAMBDA_PROXY_NAME.param,
            "this is a simple test for lambda proxy integration"
        )
    val restApiId = createRet.id
    appLogger.info("rest_api_id $restApiId")

    // set the initial path, every path part is a "resource" and will have an associated id
    val setUpPathPartRet = ApiGateway.setUpBaseResource(restApiId, LocalStackLambdaTestParams.API_PATH_PART.param)
    val resId = setUpPathPartRet.second
    appLogger.info("create-resource  rootResource.id ${setUpPathPartRet.first}  resourceId $resId")

    // ApiGateway.createModel(jsonModelSchema, "testmodel", restApiId)
    // set the rest verb for the path
    val setUpApiMethodRet = ApiGateway.setUpApiMethod(
        resId,
        restApiId,
        LocalStackLambdaTestParams.API_PROXY_VERB.param,
        LocalStackAPITestParams.API_AUTH_TYPE.param
    )
    appLogger.info("put-method httpMethod ${setUpApiMethodRet.httpMethod} authorizationType ${setUpApiMethodRet.authorizationType} ")

    // set the response
    val putMethodRet = ApiGateway.putMethodResponse(
        "",
        resId,
        restApiId,
        LocalStackLambdaTestParams.API_PROXY_VERB.param,
        LocalStackAPITestParams.API_HTTP_CODE.param
    )
    appLogger.info("put-method-response ${putMethodRet.statusCode}")

    // set the redirect to the Lambda code that generates the response body,
    val putProxyIntegRet = ApiGateway.proxyIntegration(config, resId, restApiId)
    appLogger.info("put-integration proxy httpMethod ${putProxyIntegRet.httpMethod}  proxy uri ${putProxyIntegRet.uri}")

    // deploy so we can call from
    //   https://restApiId.execute-api.us-east-2.amazonaws.com/<stage>/<path>/
    ApiGateway.createDeployment(restApiId, LocalStackLambdaTestParams.API_STAGE.param)
    appLogger.info("test uri")
    appLogger.info(
        makeTestUri(
            restApiId,
            LocalStackLambdaTestParams.API_STAGE.param,
            LocalStackLambdaTestParams.API_PATH_PART.param
        )
    )
}


private fun deployLambda(isLocalTest: Boolean) {

    LambdaProcessor.bootStrap(isLocalTest)
    appLogger.info("run deploy Lambda")
    val arn = LambdaProcessor.createFromShadowJar(
        LocalStackLambdaTestParams.LAMBDA_FUNC_NAME.param,
        ApiGatewayHandler::class.java.getName(),
        LambdaProcessor.shaddowJar
    )
    appLogger.info("deploy Lambda ok function arn=$arn")
}

fun deployLambdaForProxy(isLocalTest: Boolean): FunctionConfiguration {
    deployLambda(isLocalTest)
    return LambdaProcessor.lambdaConfig(LocalStackLambdaTestParams.LAMBDA_FUNC_NAME.param)
}

// need to run shadow task in lambda=layer module first
private fun deployLambdaLayer(): String {
    appLogger.info("run lambda layer test")
    val ret: PublishLayerVersionResult = LambdaProcessor.createLayer("reportTestLayer", LambdaProcessor.shaddowLayerJar)
    return "${ret.layerArn}:${ret.version}"
}

// currently localstack has no support for layers
fun deployLambdaWithLayerForProxy(isLocalTest: Boolean ): FunctionConfiguration {

    LambdaProcessor.bootStrap(isLocalTest)

    if(isLocalTest) {
        appLogger.info("run deploy Lambda")
        val arn = LambdaProcessor.createFromShadowJar(
            LocalStackLambdaTestParams.LAMBDA_WITH_LAYER_FUNC_NAME.param,
            GatewayReportHandler::class.java.getName(),
            LambdaProcessor.shaddowJar
        )
        appLogger.info("deploy Lambda ok function arn=$arn")
        return LambdaProcessor.lambdaConfig(LocalStackLambdaTestParams.LAMBDA_WITH_LAYER_FUNC_NAME.param)

    } else {
        val arnWithVersion = deployLambdaLayer()
        LambdaProcessor.createFromShadowJarWithLayer(LocalStackLambdaTestParams.LAMBDA_WITH_LAYER_FUNC_NAME.param,
            GatewayReportHandler::class.java.getName(), LambdaProcessor.shaddowJar , arnWithVersion)
        return LambdaProcessor.lambdaConfig(LocalStackLambdaTestParams.LAMBDA_WITH_LAYER_FUNC_NAME.param)
    }


}


/**
 *  true ==> localstack test
 *  deployHttpApiGateway sets up a http gateway proxies a json service
 *  deployApiGatewayProxy sets up a lambda gateway proxy
 *    |->  deployLambdaForProxy sets up the lambda function for deployApiGatewayProxy
 */
fun main() {
    // deployHttpApiGateway(false)

    // localstack not support layers
    // add the layer after create function code
    // val fConf = deployLambdaForProxy(true)

    val fConf = deployLambdaWithLayerForProxy(false)

     deployApiGatewayProxy(false, fConf)


}