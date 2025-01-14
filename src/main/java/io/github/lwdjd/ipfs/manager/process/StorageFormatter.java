package io.github.lwdjd.ipfs.manager.process;

import java.util.Objects;

public class StorageFormatter {

    private static final double[] UNITS = new double[] {
            1, // Bytes
            1024, // Kilobytes (KiB)
            1024 * 1024, // Megabytes (MiB)
            1024 * 1024 * 1024, // Gigabytes (GiB)
            1024L * 1024 * 1024 * 1024, // Terabytes (TiB)
            1024L * 1024 * 1024 * 1024 * 1024, // Petabytes (PiB)
            1024L * 1024 * 1024 * 1024 * 1024 * 1024 // Exabytes (EiB)
    };
    private static final String[] UNIT_NAMES = new String[] {
            "B", // Bytes
            "KiB", // Kilobytes
            "MiB", // Megabytes
            "GiB", // Gigabytes
            "TiB", // Terabytes
            "PiB", // Petabytes
            "EiB" // Exabytes
    };

    public static String formatBytes(long bytes) {

        double size = bytes;
        int unitIndex = 0;

        while (size >= 1024 && unitIndex < UNITS.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", size, UNIT_NAMES[unitIndex]);
    }

    public static void main(String[] args) {
        long bytes = 1024 * 1024 * 1024; // long类型的最大值
        System.out.println(formatBytes(bytes)); // 输出格式化后的存储大小
    }
}
