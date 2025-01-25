# Grpc Stream Service
This service is responsible for receiving and sending data to devices through the **Exchange** method inside
the grpc_stream protobuf. It also exposes a **SendMessage** method that can be called by other services to send cloud to device messages
through the Exchange method.

## Requirements

- Redis: it uses Redis to store a map of devices that are connected to the server through the Exchange method.
    - The name of the map is **CONNECTIONS**, with the following structure:
        - key: the device identifier;
        - value: the pod name which the device is connected -- this information is used by OMS to call the right
          instance when sending a message to a device.