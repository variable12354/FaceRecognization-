// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.kappt) apply false
    alias(libs.plugins.parcelize) apply false
//    id("com.google.gms.google-services") version "4.3.15" apply false
}