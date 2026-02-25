# **Stereogram Lab (OpenGL 3.3.0 - OpenCV 4.9.0)**

Este proyecto es un entorno de experimentación para la generación y decodificación de Estereogramas, aplicando las tecnicas de **RDS (Random Dot Stereograms)** y **SIS (Single Image Stereograms)**. El sistema utiliza la GPU para la extracción de mapas de profundidad precisos y OpenCV para la reconstrucción geométrica.

## Funcionalidades Principales

* Extracción de Profundidad: Renderizado de modelos ".obj" con cálculo de profundidad lineal mediante shaders personalizados.

* Generador de Estereogramas: Implementación del algoritmo **RDS** y **SIS** para la generacion de Estereogramas, basados en 
informacion de un mapa de profundidad.

* Decodificador de Relieve: Modulo encargado de Reconstruir la geometría 3D a partir de un estereograma utilizando Block Matching.

* Test Gamificado: Módulo interactivo diseñado para entrenar la agudeza visual necesaria para percibir estereogramas a través de niveles progresivos.

## Decisiones de Diseño

* Formato de Objetos 3D: La entrada de objetos 3D esta limitada a archivos **".obj"** unicamente.

* Profundidad Lineal: Se optó por utilizar la matriz Model-View (MV) en lugar de la matriz de Proyección para el cálculo de la variable distCamara. Esto garantiza que el relieve capturado sea correcto y no sufra la distorsión de la perspectiva.

* Texturas Float (RGBA32F): Para evitar el efecto de escalonado en el relieve, se configuró el Framebuffer con precisión de 32 bits. Esto permite que el gradiente de profundidad sea continuo.

* Gamificación: Se estructuraron 10 niveles de dificultad progresiva para el test de agudeza visual, balanceando la curva de aprendizaje del usuario.

* Formatos de Imagen: Para la entrada texturas y Estereograma a decodificar se optó por permitir
los formatos de **".png", ".jpg", ".jpeg" y ".bmp".**

* Formatos de Salida: Con el fin de simplificar la salida, se decidio que siempre seria **".png"**.

## Controles

* Rotación: Click Izquierdo + Drag

* Zoom: Rueda del Mouse

* Traslación: Click Derecho + Drag

## Librerías y Dependencias

* Kotlin: Lenguaje principal de la lógica de aplicación.

* OpenGL (LWJGL / OpenGL): Motor de renderizado y cálculo de profundidad.

* OpenCV: Procesamiento de imágenes, filtrado y decodificación de disparidad.

# **Autores**

*Desarrollado para la cátedra de Procesamiento Digital de Imagenes. 
Universidad Central de Venezuela (UCV). Por **Bryan Silva** y  **Oriana Arellano**, 2026.*
