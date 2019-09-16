package infra.aws.lambda

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.AWSLambdaClientBuilder
import com.amazonaws.services.lambda.model.*
import org.apache.commons.logging.LogFactory
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

val lambdaLogger = LogFactory.getLog("infra")

abstract class LambdaProcessorBase  {
    val projectBasePath = Paths.get("")
    val shaddowJar = projectBasePath.resolve("lambda/build/libs/lambda-1-all.jar")
    val lambdaRoleARN = "arn:aws:iam::XXXXXXXXX:role/some_iam_role"

    fun functionCodeAsByteBuffer(jarPath: Path): ByteBuffer {
        val zf = ZipFile(jarPath.toString())
        val zipOut = ByteArrayOutputStream()
        val zipOutStream = ZipOutputStream(zipOut as OutputStream?)
        val entries = zf.entries().iterator()
        while (entries.hasNext()) {
            val entry = entries.next()
            // if needed can filter here
            if (entry != null) {
                val zipEntry = ZipEntry(entry.name)
                zipOutStream.putNextEntry(zipEntry)
                // zip file just knows how to get the stream magic
                zf.getInputStream(entry).copyTo(zipOutStream, 1024)
                zipOutStream.closeEntry()
            }
        }

        zipOutStream.close()
        zipOut.close()


        val buf = ByteBuffer.wrap(zipOut.toByteArray())
        return buf
    }

    fun createLayerCodeFromShadowJar(jarPath: Path): ByteBuffer {

        val jf = FileInputStream(jarPath.toString())

        val zipOut = ByteArrayOutputStream()

        val zipOutStream = ZipOutputStream(zipOut)
        var zipEntry = ZipEntry("java/")
        zipOutStream.putNextEntry(zipEntry)
        zipEntry = ZipEntry("java/lib/")
        zipOutStream.putNextEntry(zipEntry)
        zipEntry = ZipEntry("java/lib/${jarPath.fileName}")
        zipOutStream.putNextEntry(zipEntry)
        jf.copyTo(zipOutStream, 1024)
        zipOutStream.closeEntry()
        zipOutStream.close()
        zipOut.close()
        // debug
//        val zipFleOut = FileOutputStream(projectBasePath.resolve("deploy.zip").toFile())
//        zipFleOut.write(zipOut.toByteArray())

        val buf = ByteBuffer.wrap(zipOut.toByteArray())
        return buf
    }

    fun createFunctionCodeFromShadowJar(jarPath: Path): FunctionCode {
        val code = FunctionCode()
        val buf = functionCodeAsByteBuffer(jarPath)
        code.zipFile = buf
        return code
    }

    fun updateFunctionCodeFromShadowJar(jarPath: Path): ByteBuffer {
        return functionCodeAsByteBuffer(jarPath)
    }
}


object LambdaProcessor : LambdaProcessorBase() {

    lateinit var lambda: AWSLambda

    fun bootStrap(isLocalTest:Boolean) {
        if(isLocalTest) {
            lambda = SdkClients.localStackLambdaClient()
            lambdaLogger.info("LambdaProcessor running as a localstack test=$isLocalTest")
        } else {
            lambda = SdkClients.awsLambdaClient()
            lambdaLogger.info("LambdaProcessor running as a localstack test=$isLocalTest")
        }
    }

    // need to run shadow task in lambda module first
    fun createFromShadowJar(name: String, handlerClassName: String, jarPath: Path): String {
        var request = makeFunctionRequest(name, handlerClassName, jarPath)
        val createFunctionResult: CreateFunctionResult = lambda.createFunction(request)
        return createFunctionResult.functionArn
    }
    // need to run shadow task in lambda module first
    fun updateFunctionCodeFromShadowJar(name: String, jarPath: Path): String {
        var requestU = UpdateFunctionCodeRequest()
        requestU.functionName = name
        requestU.publish = true
        requestU.zipFile = updateFunctionCodeFromShadowJar(jarPath)

        val updateFunctionResult: UpdateFunctionCodeResult = lambda.updateFunctionCode(requestU)
        return updateFunctionResult.functionArn
    }

    fun deleteLambda(name: String): Boolean {
        val func = lambda.getFunction(GetFunctionRequest().withFunctionName(name))
        if (func != null) {
            val deleteFunctionResult = lambda.deleteFunction(DeleteFunctionRequest().withFunctionName(name))
            if (deleteFunctionResult != null) {
                return true
            }
        }
        return false
    }

    fun createLayer(name: String, jarPath: Path): PublishLayerVersionResult {

        val layer = PublishLayerVersionRequest()
        layer.layerName = name
        layer.compatibleRuntimes.add("java8")
        val content = LayerVersionContentInput()
        content.withZipFile(createLayerCodeFromShadowJar(jarPath))
        layer.withContent(content)

        return lambda.publishLayerVersion(layer)
    }


    fun lambdaConfig(name: String): FunctionConfiguration {
        val func = lambda.getFunction(GetFunctionRequest().withFunctionName(name))
        return func.configuration
    }

    fun makeFunctionRequest(name: String, className: String, jarPath: Path): CreateFunctionRequest {
        var request = CreateFunctionRequest()
        request.functionName = name
        request.setRuntime(Runtime.Java8)
        request.role = lambdaRoleARN
        request.setCode(createFunctionCodeFromShadowJar(jarPath))
        request.setHandler(className)

        val funs: ListFunctionsResult = lambda.listFunctions()
        funs.getFunctions().forEach {
            if (it.functionName.equals(name)) {
                val delF = DeleteFunctionRequest()
                delF.functionName = name
                lambda.deleteFunction(delF)
            }
        }
        return request
    }


}

