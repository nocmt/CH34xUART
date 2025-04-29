package cn.wch.wchuartdemo;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogManager {
    private static LogManager instance;
    private FileWriter fileWriter;
    private String currentFileName;

    private LogManager() {}

    public static LogManager getInstance() {
        if (instance == null) {
            synchronized (LogManager.class) {
                if (instance == null) {
                    instance = new LogManager();
                }
            }
        }
        return instance;
    }

    public void initLogFile(Context context) {
        try {
            // 获取下载目录路径
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }

            // 使用年月日作为文件名
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            String fileName = "UARTLog_" + sdf.format(new Date()) + ".txt";
            currentFileName = fileName;

            File logFile = new File(downloadDir, fileName);
            fileWriter = new FileWriter(logFile, true);

            // 写入日志头部信息
            SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String header = "\n=== 日志记录开始 " + timeFormat.format(new Date()) + " ===\n";
            fileWriter.write(header);
            fileWriter.flush();
        } catch (IOException e) {
            LogUtil.d("初始化日志文件失败: " + e.getMessage());
        }
    }

    public void logData(String type, int serialNumber, byte[] data, int length) {
        if (fileWriter == null) return;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
            String time = sdf.format(new Date());
            String hexData = bytesToHexString(data, length);
            String utf8Data = new String(data, 0, length, StandardCharsets.UTF_8);
            
            String logEntry = String.format("[%s] %s - 串口%d:\nHEX: %s\nUTF-8: %s\n", time, type, serialNumber, hexData, utf8Data);
            fileWriter.write(logEntry);
            fileWriter.flush();
        } catch (IOException e) {
            LogUtil.d("写入日志失败: " + e.getMessage());
        }
    }

    public void closeLogFile() {
        if (fileWriter != null) {
            try {
                SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                String footer = "\n=== 日志记录结束 " + timeFormat.format(new Date()) + " ===\n";
                fileWriter.write(footer);
                fileWriter.flush();
                fileWriter.close();
                fileWriter = null;
            } catch (IOException e) {
                LogUtil.d("关闭日志文件失败: " + e.getMessage());
            }
        }
    }

    private String bytesToHexString(byte[] buffer, int length) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            String hex = Integer.toHexString(buffer[i] & 0xFF);
            if (hex.length() == 1) {
                stringBuilder.append('0');
            }
            stringBuilder.append(hex.toUpperCase());
            stringBuilder.append(' ');
        }
        return stringBuilder.toString().trim();
    }

    public String getCurrentFileName() {
        return currentFileName;
    }
}