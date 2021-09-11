package lib.http4k

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.http4k.core.Body
import org.http4k.core.BodyMode
import org.http4k.core.Status
import java.time.Duration
import okhttp3.Response as OkHttpResponse
import org.http4k.format.Jackson.auto
import java.io.IOException
import java.lang.Exception

/**
 * Utility class for Http4k client which internally uses okHttp3.
 *
 * @author Prashant.Singh
 */

data class OkHttp3ErrorResponseDetails(
    val resource: String,
    val fieldErr: String,
    override val message: String,
    override val cause: Throwable? = null
) : Exception()

open class Http4kClient {

    enum class Http4kRequestType {
        GET, POST
    }

    /**
     * This function converts the OkHttp Response into Http4k Response.
     *
     * @return org.http4k.core.Response
     */
    fun OkHttpResponse.asHttp4k(bodyMode: BodyMode): org.http4k.core.Response {
        val init = org.http4k.core.Response(Status(code, message))
        val headers = headers.toMultimap().flatMap { it.value.map { headerValue -> it.key to headerValue } }
        return (body?.let { init.body(bodyMode(it.byteStream())) } ?: init).headers(headers)
    }

    fun httpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .readTimeout(Duration.ofSeconds(60))
            .callTimeout(Duration.ofSeconds(60))
            .build()

    /**
     * Utility to call http4k client based on request Type<Http4kRequestType>.
     *
     * @param http4kRequestType: <Http4kRequestType>: HttpRequest type like GET, POST, DELETE etc.
     * @param url: <String> client url.
     * @param resource: name of the resource(can be class, method name) which can throw the exception.
     * @param errMsg: Error message to print on console for debugging purpose.
     * @param requestBody: <String> Body of the request.
     */
    internal inline fun <reified T : Any> callHttp4kClient(
        http4kRequestType: Http4kRequestType,
        url: String,
        resource: String, errMsg: String, requestBody: String = ""
    ): T {
        val responseLens = Body.auto<T>().toLens()
        val request = when (http4kRequestType) {
            Http4kRequestType.GET -> okhttp3.Request.Builder()
                .header("content-type", "application/json")
                .url(url)
                .build()
            Http4kRequestType.POST -> okhttp3.Request.Builder()
                .header("content-type", "application/json")
                .url(url)
                .post(requestBody.toRequestBody())
                .build()
            else -> throw Exception("Unsupported Http4kRequestType: $http4kRequestType")
        }
        val response = httpClient().newCall(request).execute()
        val successResponse = handleOkHttp3ResponseException(response, resource, errMsg)
        return responseLens(successResponse.asHttp4k(BodyMode.Stream))
    }

    /**
     * This method use to handle the okHttp3 Response exception. If the exception occur,
     * it extract the required error information and close the stream, otherwise connection can still open and cause leakage.
     *
     * @param response: <okhttp3.Response>
     * @param resource: <String> Resource name which create the exception
     * @param errMsg: <String> Message which need to use for trace purpose
     *
     * @return okhttp3.Response
     *
     */
    fun handleOkHttp3ResponseException(response: okhttp3.Response, resource: String, errMsg: String): okhttp3.Response {
        if (!response.isSuccessful) {
            val responseErr = response.body!!.string()
            response.body!!.close()
            response.close()
            throw OkHttp3ErrorResponseDetails(
                resource,
                response.code.toString(),
                errMsg,
                IOException(String.format(responseErr))
            )
        }
        return response
    }

}