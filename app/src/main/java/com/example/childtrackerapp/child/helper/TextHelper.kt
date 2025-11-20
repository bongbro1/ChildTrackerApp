package com.example.childtrackerapp.child.helper

fun encodeKey(key: String): String {
    return key.replace(".", "_")
}
fun decodeKey(encodedKey: String): String {
    return encodedKey.replace("_", ".")
}