package com.devaeon.mapbox.extensions

import android.content.Context
import android.widget.Toast


fun Context.showDebugToast(message:Int,duration:Int=Toast.LENGTH_SHORT){
    Toast.makeText(this, message, duration).show()
}

fun Context.showDebugToast(message:String,duration:Int=Toast.LENGTH_SHORT){
    Toast.makeText(this, message, duration).show()
}