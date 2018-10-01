// important to include in order to use rendered Android View to gl texture
#extension GL_OES_EGL_image_external : require

precision mediump float;

uniform samplerExternalOES u_Texture;    // The input texture.
varying vec2 v_TexCoord;

void main() {
    gl_FragColor = texture2D(u_Texture, v_TexCoord);
}
