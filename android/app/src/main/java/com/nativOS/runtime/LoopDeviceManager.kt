package com.nativOS.runtime

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Manages loop devices (losetup), disk images (.img with ext4/btrfs),
 * snapshots, and direct mounting of external storage (USB OTG, MicroSD).
 */
class LoopDeviceManager(private val context: Context) {

    companion object {
        private const val TAG = "NativOS.LoopDevice"
        const val DEFAULT_IMAGE_NAME = "rootfs.img"
        const val DEFAULT_IMAGE_SIZE_GB = 12
    }

    private val rootShell = RootShell(context)

    /**
     * Creates a sparse disk image file and formats it as ext4 or btrfs.
     * Sparse images allocate storage on-demand and don't occupy full capacity upfront.
     */
    fun createSparseImage(
        imageFile: File,
        sizeGb: Int = DEFAULT_IMAGE_SIZE_GB,
        fsType: String = "ext4"
    ): Boolean {
        if (!rootShell.hasRoot()) return false
        try {
            imageFile.parentFile?.mkdirs()
            val path = imageFile.absolutePath

            // 1. Create sparse image file with truncate
            val createCmd = "truncate -s ${sizeGb}G $path"
            rootShell.exec(createCmd)

            if (!imageFile.exists() || imageFile.length() <= 0) {
                Log.e(TAG, "Failed to create sparse image file: $path")
                return false
            }

            // 2. Format image with requested filesystem
            val formatCmd = if (fsType == "btrfs") {
                "(command -v mkfs.btrfs >/dev/null && mkfs.btrfs -f -m single $path) || " +
                "(mke2fs -t ext4 -F -O ^has_journal,extent,sparse_super $path)"
            } else {
                "mke2fs -t ext4 -F -O ^has_journal,extent,sparse_super $path || mkfs.ext4 -F $path"
            }

            val formatOutput = rootShell.exec(formatCmd)
            Log.i(TAG, "Formatted image $path as $fsType: $formatOutput")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating sparse image: ${e.message}", e)
            return false
        }
    }

    /**
     * Mounts a disk image to target directory using loop device.
     */
    fun mountImage(imageFile: File, targetDir: File, fsType: String = "ext4"): Boolean {
        if (!rootShell.hasRoot() || !imageFile.exists()) return false
        targetDir.mkdirs()

        val imgPath = imageFile.absolutePath
        val mountPath = targetDir.absolutePath

        if (isMounted(targetDir)) {
            Log.i(TAG, "Target directory $mountPath is already mounted")
            return true
        }

        try {
            val mountOptions = if (fsType == "btrfs") {
                "loop,compress=zstd:1,noatime,discard"
            } else {
                "loop,noatime,discard,errors=remount-ro"
            }

            val mountCmd = "mount -t $fsType -o $mountOptions $imgPath $mountPath"
            rootShell.exec(mountCmd)

            val success = isMounted(targetDir)
            if (success) {
                // Ensure write permissions on rootfs directory
                rootShell.exec("chmod 755 $mountPath")
                Log.i(TAG, "Successfully mounted image $imgPath to $mountPath ($fsType)")
            } else {
                Log.w(TAG, "Mount command completed but target directory is not mounted")
            }
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mount image $imgPath: ${e.message}", e)
            return false
        }
    }

    /**
     * Unmounts image from target directory and detaches associated loop device.
     */
    fun unmountImage(targetDir: File): Boolean {
        if (!rootShell.hasRoot()) return false
        val mountPath = targetDir.absolutePath

        try {
            if (!isMounted(targetDir)) return true

            // Unmount target directory
            rootShell.exec("umount -l $mountPath 2>/dev/null || true")

            // Clean up any dangling loop devices
            rootShell.exec("losetup -D 2>/dev/null || true")

            Log.i(TAG, "Unmounted image at $mountPath")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unmount image at $mountPath: ${e.message}")
            return false
        }
    }

    /**
     * Checks if directory is currently a mount point.
     */
    fun isMounted(targetDir: File): Boolean {
        if (!rootShell.hasRoot()) return false
        val mountPath = targetDir.absolutePath
        val mounts = rootShell.exec("cat /proc/mounts 2>/dev/null")
        return mounts.lines().any { line -> line.contains(" $mountPath ") }
    }

    // ── External Storage (USB OTG, MicroSD) ──

    /**
     * Scans for plugged-in external storage devices (/dev/block/sd*, /dev/block/mmcblk1*)
     * and mounts them directly into the chroot filesystem at /mnt/usb and /mnt/external.
     */
    fun mountExternalDrives(rootfsDir: File) {
        if (!rootShell.hasRoot()) return
        try {
            val rootfsMountPoint = rootfsDir.absolutePath
            val usbMountDir = "$rootfsMountPoint/mnt/usb"
            val externalMountDir = "$rootfsMountPoint/mnt/external"

            rootShell.exec("mkdir -p $usbMountDir $externalMountDir")

            // List available block partitions for external drives
            val blockDevices = rootShell.exec("ls /dev/block/sd* /dev/block/mmcblk1* 2>/dev/null")
                .lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.endsWith("sd") && !it.endsWith("mmcblk1") }

            if (blockDevices.isEmpty()) {
                Log.i(TAG, "No external USB/MicroSD block devices detected")
                return
            }

            for ((index, dev) in blockDevices.withIndex()) {
                val target = if (index == 0) usbMountDir else "$externalMountDir/disk$index"
                rootShell.exec("mkdir -p $target")

                val mounts = rootShell.exec("cat /proc/mounts")
                if (mounts.contains(" $target ")) continue

                // Attempt mount with auto-detected filesystem or popular filesystems
                val mountCmd = """
                    mount -o noatime,rw $dev $target 2>/dev/null ||
                    mount -t ext4 -o noatime,rw $dev $target 2>/dev/null ||
                    mount -t vfat -o rw,umask=000 $dev $target 2>/dev/null ||
                    mount -t exfat -o rw,umask=000 $dev $target 2>/dev/null ||
                    mount -t ntfs -o rw,umask=000 $dev $target 2>/dev/null || true
                """.trimIndent()
                rootShell.exec(mountCmd)

                rootShell.exec("chmod -R 0777 $target 2>/dev/null || true")
                Log.i(TAG, "Mounted external storage $dev → $target")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error mounting external drives: ${e.message}")
        }
    }

    /**
     * Safely unmounts all external USB and MicroSD drives inside rootfs.
     */
    fun unmountExternalDrives(rootfsDir: File) {
        if (!rootShell.hasRoot()) return
        try {
            val rootfsMountPoint = rootfsDir.absolutePath
            val usbMountDir = "$rootfsMountPoint/mnt/usb"
            val externalMountDir = "$rootfsMountPoint/mnt/external"

            rootShell.exec("umount -l $usbMountDir 2>/dev/null || true")
            rootShell.exec("umount -l $externalMountDir/* 2>/dev/null || true")
            rootShell.exec("umount -l $externalMountDir 2>/dev/null || true")
            Log.i(TAG, "Unmounted external storage drives")
        } catch (e: Exception) {
            Log.w(TAG, "Error unmounting external drives: ${e.message}")
        }
    }
}
