(ns jupiter.core
  (:import [java.nio ByteBuffer]
           [org.lwjgl.glfw GLFWErrorCallback GLFWKeyCallback GLFWKeyCallbackI Callbacks GLFW]
           [org.lwjgl.opengl GL GL41]
           [org.lwjgl.system MemoryStack MemoryUtil])
  (:require [jupiter.shaders :as shaders])
  (:gen-class))

(def window (atom nil))
(def render-objects (atom []))

(def vertices (float-array [0.5 -0.5 0.0 1.0 0.0 0.0
                            -0.5 -0.5 0.0 0.0 1.0 0.0
                            0.0 0.5 0.0 0.0 0.0 1.0]))

(def indices (int-array [0 1 2]))

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
    ;(GL41/glBindBuffer GL41/GL_ELEMENT_ARRAY_BUFFER ebo)
    ;(GL41/glBufferData GL41/GL_ELEMENT_ARRAY_BUFFER indices GL41/GL_STATIC_DRAW)
    (GL41/glVertexAttribPointer 0 3 GL41/GL_FLOAT false (* 6 Float/BYTES) 0)
    (GL41/glEnableVertexAttribArray 0)
    (GL41/glVertexAttribPointer 1 3 GL41/GL_FLOAT false (* 6 Float/BYTES) (* 3 Float/BYTES))
    (GL41/glEnableVertexAttribArray 1)
    vao))

(defn create-triangle []
  (let [vao (push-vertex-data vertices indices)
        program (shaders/make-program! "tricolor.vs" "tricolor.fs")]
    (swap! render-objects conj [program vao])))

(defn render-triangle [program vertex-array]
  (let [time-value (GLFW/glfwGetTime)
        green-value (+ 0.5 (/ (Math/sin time-value) 2.0))]
    (GL41/glUseProgram program)
    (shaders/set-uniform! program "ourColor" green-value))
  (GL41/glBindVertexArray vertex-array)
  (GL41/glDrawArrays GL41/GL_TRIANGLES 0 3))

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
