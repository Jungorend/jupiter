(ns jupiter.core
  (:import [java.nio ByteBuffer]
           [org.lwjgl.glfw GLFWErrorCallback GLFWKeyCallback GLFWKeyCallbackI Callbacks GLFW]
           [org.lwjgl.opengl GL GL41]
           [org.lwjgl.system MemoryStack MemoryUtil])
  (:gen-class))

(def window (atom nil))
(def render-objects (atom []))

(def vertex-source "#version 410 core
layout (location = 0) in vec3 aPos;

out vec4 vertexColor;

void main()
{
    gl_Position = vec4(aPos.x, aPos.y, aPos.z, 1.0);
    vertexColor = vec4(0.5, 0.0, 0.0, 1.0);
}")

(def fragment-source "#version 410 core
out vec4 FragColor;

in vec4 vertexColor;

void main()
{
    FragColor = vertexColor;
}")

(def vertices (float-array [0.5 0.5 0.0
                            0.5 0.0 0.0
                            0.0 0.5 0.0

                            -0.5 0.5 0.0
                            -0.5 0 0.0]))

(def indices (int-array [0 1 2
                         2 3 4]))

(defn make-shader [source type]
  (let [shader (GL41/glCreateShader type)]
    (GL41/glShaderSource shader source)
    (GL41/glCompileShader shader)
    (when (= GL41/GL_FALSE (GL41/glGetShaderi shader GL41/GL_COMPILE_STATUS))
      (throw (Exception. (GL41/glGetShaderInfoLog shader 1024))))
    shader))

(defn make-program [vertex-shader fragment-shader]
  (let [program (GL41/glCreateProgram)]
    (GL41/glAttachShader program vertex-shader)
    (GL41/glAttachShader program fragment-shader)
    (GL41/glLinkProgram program)
    (when (= GL41/GL_FALSE (GL41/glGetProgrami program GL41/GL_LINK_STATUS))
      (throw (Exception. (GL41/glGetProgramInfoLog program 1024))))
    (GL41/glDeleteShader vertex-shader)
    (GL41/glDeleteShader fragment-shader)
    program))

(defn free! [window]
  (Callbacks/glfwFreeCallbacks @window)
  (GLFW/glfwDestroyWindow @window)
  (GLFW/glfwTerminate)
  (reset! render-objects [])
  (reset! window nil)
  (.free (GLFW/glfwSetErrorCallback nil)))

(defn push-vertex-data [vertices indices]
  (let [vao (GL41/glGenVertexArrays)
        vbo (GL41/glGenBuffers)
        ebo (GL41/glGenBuffers)]
    (GL41/glBindVertexArray vao)
    (GL41/glBindBuffer GL41/GL_ARRAY_BUFFER vbo)
    (GL41/glBufferData GL41/GL_ARRAY_BUFFER vertices GL41/GL_STATIC_DRAW)
    (GL41/glBindBuffer GL41/GL_ELEMENT_ARRAY_BUFFER ebo)
    (GL41/glBufferData GL41/GL_ELEMENT_ARRAY_BUFFER indices GL41/GL_STATIC_DRAW)
    (GL41/glVertexAttribPointer 0 3 GL41/GL_FLOAT false (* 3 Float/BYTES) 0)
    (GL41/glEnableVertexAttribArray 0)
    vao))

(defn create-triangle []
  (let [vao (push-vertex-data vertices indices)
        program (make-program (make-shader vertex-source GL41/GL_VERTEX_SHADER)
                              (make-shader fragment-source GL41/GL_FRAGMENT_SHADER))]
    (swap! render-objects conj [program vao])))

(defn render-triangle [program vertex-array]
  (GL41/glUseProgram program)
  (GL41/glBindVertexArray vertex-array)
  (GL41/glDrawElements GL41/GL_TRIANGLES (count indices) GL41/GL_UNSIGNED_INT 0))

(defn loop-body [window]
  (GL41/glClear (bit-or GL41/GL_COLOR_BUFFER_BIT GL41/GL_DEPTH_BUFFER_BIT))
  (run! #(render-triangle (first %) (second %)) @render-objects)
  (GLFW/glfwSwapBuffers @window)
  (GLFW/glfwPollEvents))

(defn loop! [window]
  (GL41/glClearColor 0.2 0.3 0.3 1.0)
  (while (not (GLFW/glfwWindowShouldClose @window))
    (loop-body window)))

(defn init! [window]
  (.set (GLFWErrorCallback/createPrint System/err))
  (when (not (GLFW/glfwInit))
    (throw (IllegalStateException. "Unable to initialize GLFW")))

  (GLFW/glfwDefaultWindowHints)
  (GLFW/glfwWindowHint GLFW/GLFW_CONTEXT_VERSION_MAJOR 4)
  (GLFW/glfwWindowHint GLFW/GLFW_CONTEXT_VERSION_MINOR 1)
  (GLFW/glfwWindowHint GLFW/GLFW_VISIBLE GLFW/GLFW_FALSE)
  (GLFW/glfwWindowHint GLFW/GLFW_RESIZABLE GLFW/GLFW_TRUE)

  (reset! window (GLFW/glfwCreateWindow 1024 768 "First OpenGL Window" 0 0))
  (when (nil? @window)
    (throw (RuntimeException. "Failed to create GLFW window")))

  (GLFW/glfwSetKeyCallback @window (reify GLFWKeyCallbackI
                                     (invoke [this window key scancode action mods]
                                       (when (and (= key GLFW/GLFW_KEY_ESCAPE)
                                                  (= action GLFW/GLFW_RELEASE))
                                         (GLFW/glfwSetWindowShouldClose window true)))))

  (with-open [stack (MemoryStack/stackPush)]
    (let [p-width (.mallocInt stack 1)
          p-height (.mallocInt stack 1)
          vidmode (GLFW/glfwGetVideoMode (GLFW/glfwGetPrimaryMonitor))]

      (GLFW/glfwGetWindowSize @window p-width p-height)
      (GLFW/glfwSetWindowPos @window (/ (- (.width vidmode) (.get p-width 0)) 2)
                             (/ (- (.height vidmode) (.get p-height 0)) 2))))

  (GLFW/glfwMakeContextCurrent @window)
  (GL/createCapabilities)
  (GLFW/glfwSwapInterval 1)
  (GLFW/glfwShowWindow @window)
  (GL/createCapabilities)
  (create-triangle))

(defn game-run! []
  (init! window)
  (loop! window)
  (free! window))
