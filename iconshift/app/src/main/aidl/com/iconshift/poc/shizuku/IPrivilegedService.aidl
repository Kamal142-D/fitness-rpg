package com.iconshift.poc.shizuku;

import android.os.Bundle;
import android.os.ParcelFileDescriptor;

// Runs inside a Shizuku user service process as the shell user (uid 2000).
interface IPrivilegedService {
    // Reserved by Shizuku: called when the user service is being destroyed.
    void destroy() = 16777114;

    // Runs `sh -c command`. Returns {exit:int, out:String, err:String}.
    Bundle exec(String command) = 1;

    // Opens [path] read-only as shell, or null when it cannot be opened.
    ParcelFileDescriptor openRead(String path) = 2;

    // Copies [source] to [path], creating parents. Returns null on success, else an error message.
    String writeFrom(String path, in ParcelFileDescriptor source) = 3;

    // Registers + enables a shell-owned fabricated overlay that points each of [resourceNames]
    // ("pkg:type/name") at the PNG read from [png]. Returns null on success, else an error message.
    String registerIconOverlay(String overlayName, String targetPackage, in String[] resourceNames,
            in ParcelFileDescriptor png, int userId) = 4;

    // Unregisters the fabricated overlay. Returns null on success, else an error message.
    String unregisterIconOverlay(String overlayName, int userId) = 5;

    int uid() = 6;
}
