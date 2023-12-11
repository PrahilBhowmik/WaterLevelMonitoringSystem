#include <dummy.h>
#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <ESP8266WebServer.h>
#include <ThingSpeak.h>

int TRIGGER = D3;
int ECHO = D2;
int redLED = D8;
int greenLED = D7;
int blueLED = D6;


const char *ssid = "###########";
const char *password = "******";
WiFiClient client;


const char *thingSpeakApiKey = "##########";
const char *readAPIKey = "############";
unsigned long lastUploadTime = 0;
const unsigned long uploadInterval = 8000;

ESP8266WebServer server(80);

String page = "";
int data;
int distance;
int onOff = 0;

void setup(void) {
  pinMode(TRIGGER, OUTPUT);
  pinMode(ECHO, INPUT);
  pinMode(redLED, OUTPUT);
  pinMode(greenLED, OUTPUT);
  pinMode(blueLED, OUTPUT);

  delay(1000);
  Serial.begin(115200);
  WiFi.begin(ssid, password);
  Serial.println("");

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("");
  Serial.print("Connected to ");
  Serial.println(ssid);
  Serial.print("IP address: ");
  Serial.println(WiFi.localIP());
  server.on("/", []() {
    page = "<head><meta http-equiv=\"refresh\" content=\"3\"></head><center><h1>Web based Water Level monitor</h1><h3>Current water level is :-</h3> <h4>" + String(data) + "</h4></center>";
    server.send(200, "text/html", page);
  });
  server.begin();
  Serial.println("Web server started!");

  ThingSpeak.begin(client);
}

void loop(void) {
  int i;
  long duration = 0;
  for (i = 0; i < 150; i++) {
    digitalWrite(TRIGGER, LOW);
    delayMicroseconds(2);
    digitalWrite(TRIGGER, HIGH);
    delayMicroseconds(10);
    digitalWrite(TRIGGER, LOW);
    duration += pulseIn(ECHO, HIGH);
    delayMicroseconds(30);
  }
  duration /= 150;
  distance = (duration / 2) / 29.09;
  data = ((20 - distance) * 100) / 20;
  data = max(data, 0);

  server.handleClient();


  unsigned long currentTime = millis();
  if (currentTime - lastUploadTime >= uploadInterval) {
    uploadToThingSpeak();
    readFromThingSpeak();
    lastUploadTime = currentTime;
  }
}

void uploadToThingSpeak() {

  String dataString = String(data);
  Serial.print("Sensor data: ");
  Serial.print(dataString);
  Serial.println("%");
  if (data > 90) {
    digitalWrite(greenLED, HIGH);
    digitalWrite(redLED, LOW);
  } else if (data < 20) {
    digitalWrite(redLED, HIGH);
    digitalWrite(greenLED, LOW);
  } else {
    digitalWrite(redLED, LOW);
    digitalWrite(greenLED, LOW);
  }


  int httpCode = ThingSpeak.writeField(2281285, 1, dataString, thingSpeakApiKey);

  if (httpCode == 200) {
    Serial.println("ThingSpeak update successful");
  } else {
    Serial.println("Error updating ThingSpeak");
  }
}

void readFromThingSpeak() {
  onOff = ThingSpeak.readIntField(2286514, 1, readAPIKey);
  if (onOff == 1) {
    digitalWrite(blueLED, HIGH);
  } else {
    digitalWrite(blueLED, LOW);
  }
}
