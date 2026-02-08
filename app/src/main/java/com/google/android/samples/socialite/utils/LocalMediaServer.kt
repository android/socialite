package com.google.android.samples.socialite.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LocalMediaServer"

/**
 * Serves local media (content:// or file://) to the Google Cast receiver.
 * The Cast receiver operates as a web app and cannot read Android device paths securely.
 * This local proxy streams bytes seamlessly over HTTP to the receiver.
 */
@Singleton
class LocalMediaServer @Inject constructor(
    @ApplicationContext private val context: Context,
) : NanoHTTPD(0) {

    /**
     * The single URI that is currently authorized to be streamed.
     * Prevents arbitrary path traversal and unauthorized data access on the local network.
     */
    @Volatile
    var currentSharedUri: String? = null

    /**
     * Handles incoming HTTP requests from the Cast receiver.
     *
     * Verifies the requested URI matches the authorized [currentSharedUri], then
     * seamlessly resolves the local file or content stream and chunks it over HTTP.
     *
     * @param session The inbound HTTP request session.
     * @return The HTTP response containing the chunked media stream or an error status.
     */
    override fun serve(session: IHTTPSession): Response {
        val uriString = session.uri.substringAfter("/")
        Log.d(TAG, "Request: $uriString")

        if (uriString != currentSharedUri) {
            Log.w(TAG, "Blocked unauthorized request: $uriString")
            return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden")
        }

        return try {
            val isContentUri = uriString.startsWith("${ContentResolver.SCHEME_CONTENT}://")
            val androidUri = if (isContentUri) Uri.parse(uriString) else null

            val inputStream = if (isContentUri) {
                androidUri?.let { context.contentResolver.openInputStream(it) }
            } else {
                File(uriString).takeIf { it.isFile }?.inputStream()
            }

            if (inputStream != null) {
                val mime = if (isContentUri) {
                    androidUri?.let { context.contentResolver.getType(it) } ?: getMimeType(uriString)
                } else {
                    getMimeType(uriString)
                }
                newChunkedResponse(Response.Status.OK, mime, inputStream)
            } else {
                Log.w(TAG, "Content not found: $uriString")
                newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Content not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error serving file", e)
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal Error")
        }
    }

    /**
     * Determines the most appropriate local IPv4 address of the device to bind the HTTP server.
     * Filters out loopbacks, virtual interfaces, and inactive connections.
     *
     * @return The local IPv4 address as a string, or null if no valid address is found.
     */
    fun getLocalIpAddress(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .filter { !it.isLoopback && it.isUp && !it.isVirtual }
                .flatMap { it.inetAddresses.asSequence() }
                .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
                ?.hostAddress
        } catch (ex: SocketException) {
            Log.e(TAG, "Error getting IP address", ex)
            null
        }
    }

    companion object {
        /**
         * Infers the MIME type of a file based on its extension using the native Android [MimeTypeMap].
         *
         * @param url The file path or URI string.
         * @return The inferred MIME type (e.g. "video/mp4"), defaulting to "text/plain".
         */
        private fun getMimeType(url: String): String {
            val extension = MimeTypeMap.getFileExtensionFromUrl(url)
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: MIME_PLAINTEXT
        }
    }
}
