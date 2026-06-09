#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include "addons/TokenHelper.h"

/* ===== CONFIGURACION ===== */
#define WIFI_SSID      "INFINITUM5599"
#define WIFI_PASSWORD  "K9TcupNeAn"

#define API_KEY        "AIzaSyB4U131RkqvYYUSeC6s85wuoto8W8f5fP0"
#define DATABASE_URL   "https://comdomoticaapp-default-rtdb.firebaseio.com"

#define USER_EMAIL     "polvoraul118@gmail.com"
#define USER_PASSWORD  "2mN+nTfZBdUZr@S" 

#define DEVICE_ID      "esp32_01"
#define LED_PIN        2
/* =========================== */

FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

unsigned long tRead = 0, tOnline = 0;
const int READ_MS = 1000;
const int ONLINE_MS = 30000;
int lastLed = -1;
bool authOK = false;

void setup() {
    Serial.begin(115200);
    delay(500);
    Serial.println("\n\n=== ESP32 LED CONTROLLER ===");

    pinMode(LED_PIN, OUTPUT);
    digitalWrite(LED_PIN, LOW);
    Serial.println("GPIO " + String(LED_PIN) + " = OUTPUT LOW");

    // Prueba de hardware: 3 parpadeos para verificar LED
    Serial.println("[TEST] Probando LED fisico...");
    for (int i = 0; i < 3; i++) {
        digitalWrite(LED_PIN, HIGH);
        delay(300);
        digitalWrite(LED_PIN, LOW);
        delay(300);
    }
    Serial.println("[TEST] Prueba LED completada");

    conectarWiFi();
    configurarFirebase();
}

void loop() {
    // ---- 1. WiFi ----
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("[WiFi] Desconectado, reconectando...");
        conectarWiFi();
        return;
    }

    // ---- 2. Firebase listo? ----
    if (!Firebase.ready()) {
        static unsigned long t = 0;
        if (millis() - t > 5000) {
            t = millis();
            Serial.println("[Firebase] Esperando autenticacion...");
        }
        delay(100);
        return;
    }

    if (!authOK) {
        authOK = true;
        Serial.println("[Firebase] Autenticacion OK, token recibido");
    }

    unsigned long ahora = millis();

    // ---- 3. Escribir online cada 30s ----
    if (ahora - tOnline > ONLINE_MS) {
        tOnline = ahora;
        String path = String("/devices/") + DEVICE_ID + "/online";
        if (Firebase.RTDB.setBool(&fbdo, path, true)) {
            Serial.println("[Firebase] online = true OK");
        } else {
            Serial.println("[Firebase] ERROR escribiendo online:");
            Serial.println("   " + fbdo.errorReason());
        }
    }

    // ---- 4. Leer led cada 1s ----
    if (ahora - tRead > READ_MS) {
        tRead = ahora;
        String path = String("/devices/") + DEVICE_ID + "/led";

        if (Firebase.RTDB.getInt(&fbdo, path)) {
            if (fbdo.dataType() == "int") {
                int v = fbdo.intData();
                if (v != lastLed) {
                    lastLed = v;
                    digitalWrite(LED_PIN, v == 1 ? HIGH : LOW);
                    Serial.print("[LED] Valor leido = ");
                    Serial.print(v);
                    Serial.print(" -> GPIO ");
                    Serial.print(LED_PIN);
                    Serial.println(v == 1 ? " HIGH (ENCENDIDO)" : " LOW (APAGADO)");
                }
            } else {
                Serial.println("[LED] El nodo NO es un entero. Tipo: " + fbdo.dataType());
            }
        } else {
            Serial.println("[Firebase] ERROR leyendo led:");
            Serial.println("   Ruta: " + path);
            Serial.println("   Error: " + fbdo.errorReason());

            // Si el error es "path not exist", intentar crear el nodo
            if (fbdo.errorReason().indexOf("PATH_NOT_EXIST") >= 0 ||
                fbdo.errorReason().indexOf("path") >= 0) {
                Serial.println("[Firebase] Creando nodo led = 0 ...");
                Firebase.RTDB.setInt(&fbdo, path, 0);
            }
        }
    }
}

// ===== FUNCIONES =====

void conectarWiFi() {
    Serial.print("[WiFi] Conectando a ");
    Serial.print(WIFI_SSID);
    WiFi.mode(WIFI_STA);
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

    int intentos = 0;
    while (WiFi.status() != WL_CONNECTED && intentos < 60) {
        delay(500);
        Serial.print(".");
        intentos++;
    }

    if (WiFi.status() == WL_CONNECTED) {
        Serial.println();
        Serial.println("[WiFi] CONECTADO");
        Serial.print("   IP: ");
        Serial.println(WiFi.localIP());
        Serial.print("   RSSI: ");
        Serial.print(WiFi.RSSI());
        Serial.println(" dBm");
    } else {
        Serial.println();
        Serial.println("[WiFi] ERROR: No se pudo conectar");
        Serial.println("[WiFi] Reiniciando ESP32 en 5 segundos...");
        delay(5000);
        ESP.restart();
    }
}

void configurarFirebase() {
    Serial.println("[Firebase] Configurando...");
    Serial.print("   URL: ");
    Serial.println(DATABASE_URL);
    Serial.print("   User: ");
    Serial.println(USER_EMAIL);
    Serial.print("   Device: ");
    Serial.println(DEVICE_ID);

    config.api_key = API_KEY;
    config.database_url = DATABASE_URL;

    auth.user.email = USER_EMAIL;
    auth.user.password = USER_PASSWORD;

    fbdo.setResponseSize(4096);
    config.token_status_callback = tokenStatusCallback;

    Firebase.reconnectNetwork(true);
    Firebase.begin(&config, &auth);

    Serial.println("[Firebase] Inicializado. Esperando autenticacion...");
}
