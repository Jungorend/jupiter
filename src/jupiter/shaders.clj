(ns jupiter.shaders
  (:import [org.lwjgl.opengl GL41]))

(def shaders-location "resources/shaders/")

(defn _make-shader! [source type]
  (let [shader (GL41/glCreateShader type)]
    (GL41/glShaderSource shader source)
    (GL41/glCompileShader shader)
    (when (= GL41/GL_FALSE (GL41/glGetShaderi shader GL41/GL_COMPILE_STATUS))
      (throw (Exception. (GL41/glGetShaderInfoLog shader 1024))))
    shader))

(defn make-program! [vertex-path fragment-path]
  (let [vertex-shader (_make-shader! (slurp (str shaders-location vertex-path)) GL41/GL_VERTEX_SHADER)
        fragment-shader (_make-shader! (slurp (str shaders-location fragment-path)) GL41/GL_FRAGMENT_SHADER)
        program (GL41/glCreateProgram)]
    (GL41/glAttachShader program vertex-shader)
    (GL41/glAttachShader program fragment-shader)
    (GL41/glLinkProgram program)
    (when (= GL41/GL_FALSE (GL41/glGetProgrami program GL41/GL_LINK_STATUS))
      (throw (Exception. (GL41/glGetProgramInfoLog program 1024))))
    (GL41/glDeleteShader vertex-shader)
    (GL41/glDeleteShader fragment-shader)
    program))

(defn set-uniform! [id name value]
  (GL41/glUniform1i (GL41/glGetUniformLocation id name) value))

(defn free-program! [id]
  (GL41/glDeleteProgram id))
