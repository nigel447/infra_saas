package infra.aws

import infra.aws.config.LocalStackAPITestParams
import org.junit.jupiter.api.Test


// https://jsonplaceholder.typicode.com/
class ApiGatewayTests {

    @Test
    fun `run deploy api gateway code`() {
        ApiGateway.bootStrap(true)
        val createRet =
            ApiGateway.createApPI(LocalStackAPITestParams.API_HTTP_NAME.param, "this is a simple test for http integration")
        val restApiId = createRet.id
        appLogger.info("rest_api_id $restApiId")

        val setUpPathPartRet = ApiGateway.setUpBaseResource(restApiId)
        val resId = setUpPathPartRet.second
        appLogger.info("create-resource  rootResource.id ${setUpPathPartRet.first}  resourceId $resId")

        val setUpApiMethodRet = ApiGateway.setUpApiMethod(
            resId,
            restApiId,
            LocalStackAPITestParams.API_VERB.param,
            LocalStackAPITestParams.API_AUTH_TYPE.param
        )
        appLogger.info("put-method httpMethod ${setUpApiMethodRet.httpMethod} authorizationType ${setUpApiMethodRet.authorizationType} ")

        val putMethodRet = ApiGateway.putMethodResponse(
            "",
            resId,
            restApiId,
            LocalStackAPITestParams.API_VERB.param,
            LocalStackAPITestParams.API_HTTP_CODE.param
        )
        appLogger.info("put-method-response ${putMethodRet.statusCode}")

        val putHttpIntegRet = ApiGateway.httpIntegration(
            resId,
            restApiId,
            LocalStackAPITestParams.API_VERB.param,
            LocalStackAPITestParams.API_REST_ENDPOINT_URI.param
        )

        appLogger.info("put-integration httpMethod ${putHttpIntegRet.httpMethod}  uri ${putHttpIntegRet.uri}")

        val putIntegrationResponseRequest = ApiGateway.putIntegrationResponse(
            resId,
            restApiId,
            LocalStackAPITestParams.API_VERB.param,
            LocalStackAPITestParams.API_HTTP_CODE.param,
            ""
        )

        appLogger.info("put-integration-response statusCode ${putIntegrationResponseRequest.statusCode}")

        val createDeploymentResult = ApiGateway.createDeployment(restApiId, LocalStackAPITestParams.API_STAGE.param)

        appLogger.info("create-deployment apiSummary ${createDeploymentResult.apiSummary}")

    }
}

