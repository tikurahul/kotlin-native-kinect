package sample

import kinect.*
import kotlinx.cinterop.*

@ExperimentalUnsignedTypes
fun helloKinect() {
    when (val deviceCount = k4a_device_get_installed_count()) {
        0.toUInt() -> println("No devices connected")
        else -> {
            println("No of devices connected = ($deviceCount)")
            memScoped {
                val device = alloc<k4a_device_tVar>()
                val imuSample = alloc<k4a_imu_sample_t>()
                val config = alloc<_k4a_device_configuration_t>()
                config.color_format = K4A_IMAGE_FORMAT_COLOR_MJPG
                config.camera_fps = K4A_FRAMES_PER_SECOND_30
                config.color_resolution = K4A_COLOR_RESOLUTION_720P
                config.depth_mode = K4A_DEPTH_MODE_NFOV_UNBINNED
                config.synchronized_images_only = true
                config.wired_sync_mode = k4a_wired_sync_mode_t.K4A_WIRED_SYNC_MODE_STANDALONE

                device.usePinned {
                    var result = k4a_device_open(K4A_DEVICE_DEFAULT, device.ptr)
                    if (result != K4A_RESULT_SUCCEEDED) {
                        println("Unable to connect to Kinect.")
                        return
                    }

                    result = k4a_device_start_cameras(device.value, config.ptr)
                    if (result != K4A_RESULT_SUCCEEDED) {
                        println("Started Cameras.")
                        return
                    }

                    result = k4a_device_start_imu(device.value)
                    if (result != K4A_RESULT_SUCCEEDED) {
                        println("Started IMU.")
                        return
                    }

                    loop@ for (i in 0 until 10) {
                        result = k4a_device_get_imu_sample(
                            device.value,
                            imu_sample = imuSample.ptr,
                            timeout_in_ms = K4A_WAIT_INFINITE
                        )
                        when (result) {
                            K4A_WAIT_RESULT_SUCCEEDED -> {
                                val acceleration = imuSample.acc_sample.xyz
                                val accelerationTimeStamp = imuSample.acc_timestamp_usec
                                val gyro = imuSample.gyro_sample.xyz
                                val gyroTimestamp = imuSample.gyro_timestamp_usec
                                val temperature = imuSample.temperature
                                println("Acceleration (X - ${acceleration.x}), (Y - ${acceleration.y}), (Z - ${acceleration.z}) at $accelerationTimeStamp")
                                println("Gyro (X - ${gyro.x}), (Y - ${gyro.y}), (Z - ${gyro.z}) at $gyroTimestamp")
                                println("Temperature $temperature")
                                println()
                            }
                            K4A_WAIT_RESULT_TIMEOUT -> {
                                println("Operation timed out")
                                break@loop
                            }
                            K4A_WAIT_RESULT_TIMEOUT -> {
                                println("FFailed to collect IMU Samples")
                                break@loop
                            }
                        }
                    }
                    println("Stopping Cameras and IMU.")
                    k4a_device_stop_cameras(device.value)
                    k4a_device_stop_imu(device.value)

                    println("Closing Device.")
                    k4a_device_close(device.value)
                }
            }
        }
    }
}

@ExperimentalUnsignedTypes
fun main() = helloKinect()
