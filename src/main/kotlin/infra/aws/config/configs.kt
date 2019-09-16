package infra.aws.config

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.regions.Regions
import com.amazonaws.services.apigateway.AmazonApiGateway
import com.amazonaws.services.apigateway.AmazonApiGatewayClientBuilder

object GenericCredentialsProvider : AWSCredentialsProvider {
    override fun getCredentials(): AWSCredentials {
        return BasicAWSCredentials("some access key id","some secret key")
    }

    override fun refresh() {

    }
}

enum class LocalStackAPITestParams(val param: String) {
    API_HTTP_NAME("api_local_http__test" ),  API_LAMBDA_PROXY_NAME("api_local_proxy__test" ), API_PATH_PART("test_path" ), API_VERB("GET" ), API_AUTH_TYPE("NONE" ),
    API_HTTP_CODE("200"), API_REST_ENDPOINT_URI("https://jsonplaceholder.typicode.com/todos/1"), API_STAGE("api_testing"),
    API_GATEWAY_ENDPOINT("http://localhost:4567" )
}

object SdkClients {

    fun apiGateWay(): AmazonApiGateway {
        return AmazonApiGatewayClientBuilder.standard()
            .withCredentials(DefaultAWSCredentialsProviderChain())
            .withRegion(Regions.US_EAST_2).build()
    }

     fun localApiGateWay(): AmazonApiGateway {
         return AmazonApiGatewayClientBuilder.standard()
             .withCredentials(credentials())
             .withEndpointConfiguration(endpointResolver(LocalStackAPITestParams.API_GATEWAY_ENDPOINT)).build()
     }

    private fun endpointResolver(endPoint: LocalStackAPITestParams): AwsClientBuilder.EndpointConfiguration {
        return AwsClientBuilder.EndpointConfiguration(endPoint.param, Regions.US_EAST_2.getName())
    }

   private fun credentials(): AWSCredentialsProvider {
        return GenericCredentialsProvider
    }
}






