package com.yperess.stackoverflow

import android.content.Context
import android.opengl.GLES20
import androidx.annotation.RawRes
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Read a raw resource as a string.
 *
 * @param context The context to access the resource.
 * @param resourceId The raw resource's ID.
 * @return A string with the resource's content, or null if an error occurred.
 */
fun readRawResource(context: Context, @RawRes resourceId: Int): String? {
    val inputStream = context.resources.openRawResource(resourceId)
    val inputStreamReader = InputStreamReader(inputStream)
    val bufferedReader = BufferedReader(inputStreamReader)

    var nextLine: String? = null
    val body = StringBuilder()
    try {
        do {
            nextLine = bufferedReader.readLine()?.also { line ->
                body.append(line)
                        .append('\n')
            }
        } while (nextLine != null)
    } catch (ex: IOException) {
        return null
    }
    return body.toString()
}

/**
 * Compile a shader script.
 *
 * @param shaderType The type of shader script (usually either [GLES20.GL_VERTEX_SHADER]
 *   or [GLES20.GL_FRAGMENT_SHADER]
 * @param shaderSource The source to compile.
 * @return A handle to the compiled shader.
 * @throws RuntimeException If the shader could not be compiled.
 */
@Throws(RuntimeException::class)
fun compileShader(shaderType: Int, shaderSource: String): Int {
    var shaderHandle = GLES20.glCreateShader(shaderType)
    if (shaderHandle != 0) {
        // Pass in the shader source
        GLES20.glShaderSource(shaderHandle, shaderSource)

        // Compile the shader
        GLES20.glCompileShader(shaderHandle)

        // Check that the compilation succeeded
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            Timber.e("Error compiling shader: %s",
                    GLES20.glGetShaderInfoLog(shaderHandle))
            GLES20.glDeleteShader(shaderHandle)
            shaderHandle = 0
        }
    }
    if (shaderHandle == 0) {
        throw RuntimeException("Error creating shader")
    }
    return shaderHandle
}

/**
 * Creates and links a GLES20 program.
 *
 * @param vertexShaderHandle The GLES20 handle for the vertex shader.
 * @param fragmentShaderHandle The GLES20 handle for the fragment shader.
 * @return The newly created program handle
 * @throws RuntimeException If the program could not be created.
 */
@Throws(RuntimeException::class)
fun createAndLinkProgram(vertexShaderHandle: Int, fragmentShaderHandle: Int): Int {
    var programHandle = GLES20.glCreateProgram()
    if (programHandle != 0) {
        // Attach the shaders
        GLES20.glAttachShader(programHandle, vertexShaderHandle)
        GLES20.glAttachShader(programHandle, fragmentShaderHandle)

        // Link the two shaders together into the program
        GLES20.glLinkProgram(programHandle)

        // Check that the linking was successful
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Timber.e("Error compiling program: %s",
                    GLES20.glGetProgramInfoLog(programHandle))
            GLES20.glDeleteProgram(programHandle)
            programHandle = 0
        }
    }

    if (programHandle == 0) {
        throw RuntimeException("Error creating program")
    }
    return programHandle;
}