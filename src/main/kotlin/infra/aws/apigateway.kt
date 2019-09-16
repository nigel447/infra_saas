package infra.aws

import com.amazonaws.services.apigateway.AmazonApiGateway
import com.amazonaws.services.apigateway.model.*
import com.amazonaws.services.lambda.model.FunctionConfiguration
import infra.aws.config.LocalStackAPITestParams
import infra.aws.config.SdkClients
import java.io.File

object ApiGateway{
    lateinit var apiGateway: AmazonApiGateway

    fun bootStrap(isLocalTest:Boolean) {
        if(isLocalTest) {
            apiGateway = SdkClients.localApiGateWay()
            appLogger.info("ApiGateway running as a localstack test=$isLocalTest")
        } else {
            apiGateway = SdkClients.apiGateWay()
            appLogger.info("ApiGateway running as a localstack test=$isLocalTest")
        }
    }

    // https://docs.aws.amazon.com/apigateway/latest/developerguide/create-api-using-awscli.html
    fun createApPI(apiName: String, description:String): CreateRestApiResult {
        val createRestApiRequest = CreateRestApiRequest()
        createRestApiRequest.name = apiName
        createRestApiRequest.description = description
        return apiGateway.createRestApi(createRestApiRequest)
    }

    fun setUpBaseResource(id: String, path:String): Pair<String, String> {
        // get the root part
        val resources = apiGateway
            .getResources(GetResourcesRequest().withRestApiId(id)).getItems()

        val rootResource = resources.asSequence().filter { it.path == File.separator }.first()

        val resourceId = apiGateway.createResource(
            CreateResourceRequest().withParentId(rootResource.id).withRestApiId(id)
                .withPathPart(path)
        ).getId()
        return Pair(rootResource.id, resourceId)

    }

    fun setUpApiMethod(resourceId: String, restApiId: String, verb: String, authType: String): PutMethodResult {

        return apiGateway.putMethod(
            PutMethodRequest()
                .withResourceId(resourceId)
                .withRestApiId(restApiId)
                .withAuthorizationType(authType)
                .withHttpMethod(verb)
        )
    }

    fun createModel(model: String, modelName: String, restApi: String): String {

        return apiGateway.createModel(
            CreateModelRequest()
                .withSchema(model)
                .withContentType("application/json")
                .withName(modelName)
                .withRestApiId(restApi)
        ).id

    }


    fun putMethodResponse(
        modelName: String,
        resourceId: String,
        restApi: String,
        verb: String,
        httpCode: String
    ): PutMethodResponseResult {
        return apiGateway.putMethodResponse(
            PutMethodResponseRequest()
                .withHttpMethod(verb)
                // .withResponseModels(mapOf("application/json" to modelName))
                .withRestApiId(restApi)
                .withResourceId(resourceId)
                .withStatusCode(httpCode)
        )
    }

    fun httpIntegration(resourceId: String, restApi: String, verb: String, uri: String): PutIntegrationResult {

        val req = PutIntegrationRequest()
            .withType("HTTP")
            .withIntegrationHttpMethod(verb)
            .withHttpMethod(verb)
            .withRestApiId(restApi)
            .withResourceId(resourceId)
            .withUri(uri)

        return apiGateway.putIntegration(req)

    }

    fun proxyIntegration(config: FunctionConfiguration, resourceId: String, restApi: String ): PutIntegrationResult {

        val INTERGRATION_URI =
            "arn:aws:apigateway:us-east-2:lambda:path/2015-03-31/functions/${config.functionArn}/invocations"

        val req = PutIntegrationRequest()
            .withCredentials(config.role)
            .withIntegrationHttpMethod("POST")
            .withHttpMethod("POST")
            .withType(IntegrationType.AWS_PROXY)
            .withRestApiId(restApi)
            .withResourceId(resourceId)
            .withPassthroughBehavior("WHEN_NO_MATCH")
            .withUri(INTERGRATION_URI)

        return apiGateway.putIntegration(req)
    }

    fun putIntegrationResponse(
        resourceId: String,
        restApi: String,
        verb: String,
        httpCode: String,
        pattern: String
    ): PutIntegrationResponseResult {
        val req = PutIntegrationResponseRequest()
            .withHttpMethod(verb)
            .withRestApiId(restApi)
            .withResourceId(resourceId)
            .withStatusCode(httpCode)
            .withSelectionPattern(pattern)
        return apiGateway.putIntegrationResponse(req)
    }

    fun createDeployment(restApiId:String, stageName:String) : CreateDeploymentResult {
        val createDeploymentRequest = CreateDeploymentRequest()
        createDeploymentRequest.restApiId = restApiId
        createDeploymentRequest.stageName=stageName
        return apiGateway.createDeployment(createDeploymentRequest)
    }
}