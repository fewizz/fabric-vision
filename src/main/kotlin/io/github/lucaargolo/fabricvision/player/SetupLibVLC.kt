package io.github.lucaargolo.fabricvision.player

import com.sun.jna.NativeLibrary
import com.sun.jna.Platform
import net.fabricmc.loader.api.FabricLoader
import uk.co.caprica.vlcj.binding.support.runtime.RuntimeUtil
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.factory.discovery.strategy.BaseNativeDiscoveryStrategy
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream
import kotlin.io.path.pathString

object SetupLibVLC {

    fun initialize() {
        try{
            MinecraftMediaPlayerHolder.FACTORY = MediaPlayerFactory(LibVLCDiscovery(), "--quiet")
        }catch (exception: UnsatisfiedLinkError) {
            exception.printStackTrace()
        }
    }

    class LibVLCDiscovery: NativeDiscovery() {

        private val strategiesField = NativeDiscovery::class.java.getDeclaredField("discoveryStrategies")
        private val findMethod = BaseNativeDiscoveryStrategy::class.java.getDeclaredMethod("find", String::class.java)
        init {
            strategiesField.isAccessible = true
            findMethod.isAccessible = true
        }

        override fun onNotFound() {
            println("Vlc not found in system")
            val path = FabricLoader.getInstance().gameDir.pathString + File.separator + ".libvlc"

            val strategies = strategiesField.get(this) as Array<*>

            var success = false
            strategies.forEach { strategy ->
                if(strategy is BaseNativeDiscoveryStrategy) {
                    if(findMethod.invoke(strategy, path) as Boolean) {
                       success = true
                       return@forEach
                    }
                }
            }

            if(!success) {

                println("Oops gotta download it")

                val vlcVersion = "3.0.18"
                val (vlcPlatform, vlcArchitecture) = if(Platform.isWindows() && Platform.isIntel()) {
                    if (Platform.is64Bit()) {
                        "win64" to "win64"
                    }else {
                        "win32" to "win32"}
                }else if(Platform.isMac() && Platform.is64Bit()){
                    if(Platform.isIntel()) {
                        "macosx" to "intel64" }
                    else if(Platform.isARM()) {
                        "macosx" to "arm64"}
                    else{
                        null to null
                    }
                }else {
                    null to null
                }

                if(vlcPlatform != null && vlcArchitecture != null) {
                    val vlcExtension = if(vlcPlatform.startsWith("mac")) "dmg" else "zip"

                    val downloadUrl = "https://download.videolan.org/pub/vlc/$vlcVersion/$vlcPlatform/vlc-$vlcVersion-$vlcArchitecture.$vlcExtension"

                    val downloadedFileName = downloadUrl.split("/").last()
                    val extractedFolderName = if(vlcPlatform.startsWith("mac")) {
                        "VLC media player" + File.separator + "VLC.app" + File.separator + "Contents" + File.separator + "MacOS"
                    }else{
                        downloadedFileName.split("-").filterNot { it.endsWith(".$vlcExtension") }.joinToString("-") { s -> s }
                    }

                    val folder = File(path)
                    folder.mkdirs()
                    val downloadedFile = File(folder, downloadedFileName)
                    println("Downloading file from: $downloadUrl")
                    URL(downloadUrl).openStream().use { input ->
                        FileOutputStream(downloadedFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    println("Download completed.")
                    println("Extracting file: ${downloadedFile.name}")
                    if(vlcExtension == "zip") {
                        ZipInputStream(downloadedFile.inputStream()).use { zipInputStream ->
                            while (true) {
                                val entry = zipInputStream.nextEntry ?: break
                                val entryFile = File(folder, entry.name)
                                if (entry.isDirectory) {
                                    entryFile.mkdirs()
                                } else {
                                    entryFile.parentFile.mkdirs()
                                    entryFile.outputStream().use { output ->
                                        zipInputStream.copyTo(output)
                                    }
                                }
                                zipInputStream.closeEntry()
                            }
                        }
                    }
                    println("Extraction completed.")
                    val extractedFolder = File(folder, extractedFolderName)
                    println("Copying files to destination folder.")
                    extractedFolder.walkTopDown().forEach { file ->
                        if(file.isFile && file.nameWithoutExtension == "libvlc" || file.nameWithoutExtension == "libvlccore") {
                            val destinationPath = File(folder, file.name)
                            file.copyTo(destinationPath, true)
                        }else if(file.isDirectory && file.name == "plugins"){
                            val destinationPath = File(folder, "plugins")
                            file.copyRecursively(destinationPath, true)
                        }else if(file.isDirectory && file.name == "lib") {
                            file.copyRecursively(folder, true)
                        }
                    }
                    println("Files copied to destination folder.")
                    println("Cleaning up temporary files.")
                    downloadedFile.delete()
                    extractedFolder.deleteRecursively()
                    println("Cleanup completed.")
                }else{
                    println("Unknown platform")
                }

            }else{
                println("Already downloaded")
            }

            NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), path)
            NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcCoreLibraryName(), path)
            NativeLibrary.getInstance(RuntimeUtil.getLibVlcCoreLibraryName())
        }

    }


}