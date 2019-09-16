package infra.aws.lambda

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.regions.Regions
import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.AWSLambdaClientBuilder

object GenericCredentialsProvider : AWSCredentialsProvider {
    override fun getCredentials(): AWSCredentials {
        return BasicAWSCredentials("some access key id","some secret key")
    }

    override fun refresh() {

    }
}

enum class LocalStackLambdaTestParams(val param: String) {
    LAMBDA_ENDPOINT("http://localhost:4574" ),
    LAMBDA_FUNC_NAME("gatewayProxyTest"),
    API_PROXY_VERB("POST"),
    API_STAGE("api_proxy_testing"),
    API_PATH_PART("proxy_test_path" )
}

val jsonModelSchema = """
{
   "${'$'}schema": "http://json-schema.org/draft-04/schema#",
    "title": "testmodel",
    "type":"object",
    "properties":{
        "input":{"type":"string"}
    }
}

""".trimIndent()


object SdkClients {

    fun localStackLambdaClient(): AWSLambda {
        return AWSLambdaClientBuilder.standard()
            .withCredentials(credentials())
            .withEndpointConfiguration(endpointResolver(LocalStackLambdaTestParams.LAMBDA_ENDPOINT)).build()
    }

    fun awsLambdaClient(): AWSLambda {
        return AWSLambdaClientBuilder.standard()
            .withCredentials(DefaultAWSCredentialsProviderChain()).withRegion(Regions.US_EAST_2).build()

    }

    private fun endpointResolver(endPoint: LocalStackLambdaTestParams): AwsClientBuilder.EndpointConfiguration {
        return AwsClientBuilder.EndpointConfiguration(endPoint.param, Regions.US_EAST_2.getName())
    }

    private fun credentials(): AWSCredentialsProvider {
        return GenericCredentialsProvider
    }
}
