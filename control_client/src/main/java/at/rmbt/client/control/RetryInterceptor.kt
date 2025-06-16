package at.rmbt.client.control

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class RetryInterceptor(private val maxRetryCount: Int = 3) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            throw IOException("Initial request failed: ${e.message}")
        }
        var retryCount = 0
        while (!response.isSuccessful && retryCount < maxRetryCount) {
            retryCount++
            response.close()
            Thread.sleep(1000 * retryCount.toLong())
            response = try {
                chain.proceed(request)
            } catch (e: Exception) {
                throw IOException("Request failed on retry $retryCount: ${e.message}")
            }
        }
        if (!response.isSuccessful){
            throw IOException("Request failed after $retryCount attempts")
        }
        return response
    }
}