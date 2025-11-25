package com.ptit.iot

import java.util.UUID


object DeviceUUID {
    val DEVICE_SERVICE_UUID = (0x180D).toUUID()
    val DEVICE_CHARACTERISTIC_UUID = (0x2A37).toUUID()
    val CLIENT_CHARACTERISTIC_CONFIG_UUID = (0x2902).toUUID()
}

fun Int.toUUID(): UUID {
    val MSB = 0x0000000000001000L
    val LSB = -0x7fffff7fa064cb05L
    val value = (this and -0x1).toLong()
    return UUID(MSB or (value shl 32), LSB)
}
