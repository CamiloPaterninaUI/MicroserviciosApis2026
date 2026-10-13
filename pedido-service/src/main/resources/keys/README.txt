Este directorio debe contener la llave PÚBLICA RSA de demostración:

  public.pem -> llave pública (X.509 PEM), pareja de la llave privada del auth-service

Genérela ejecutando desde la raíz del proyecto:

  sh scripts/generate-rsa-keys.sh

El script copia aquí ÚNICAMENTE la llave pública: pedido-service solo
valida firmas JWT y nunca debe conocer la llave privada. El archivo .pem
está ignorado por Git y NO debe publicarse.
