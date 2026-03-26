package com.zhou.service;

import org.springframework.stereotype.Service;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class NativeDialogService {

    public String pickDirectory(String title) {
        if (!GraphicsEnvironment.isHeadless()) {
            return runOnEdt(() -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle(title == null || title.isBlank() ? "选择文件夹" : title);
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setAcceptAllFileFilterUsed(false);
                int result = chooser.showOpenDialog(null);
                if (result != JFileChooser.APPROVE_OPTION) {
                    return "";
                }
                File selected = chooser.getSelectedFile();
                return selected == null ? "" : selected.getAbsolutePath();
            });
        }
        if (isWindows()) {
            String safeTitle = escapeForPowerShell(title == null || title.isBlank() ? "Select Folder" : title);
            String script = String.join(";",
                    "$OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)",
                    "Add-Type -AssemblyName System.Windows.Forms",
                    "Add-Type -AssemblyName System.Drawing",
                    "$owner = New-Object System.Windows.Forms.Form",
                    "$owner.TopMost = $true",
                    "$owner.ShowInTaskbar = $false",
                    "$owner.FormBorderStyle = [System.Windows.Forms.FormBorderStyle]::FixedToolWindow",
                    "$owner.StartPosition = [System.Windows.Forms.FormStartPosition]::CenterScreen",
                    "$owner.Size = New-Object System.Drawing.Size(1,1)",
                    "$owner.Opacity = 0.01",
                    "$owner.Show()",
                    "$owner.Activate()",
                    "$owner.BringToFront()",
                    "[System.Windows.Forms.Application]::DoEvents()",
                    "Start-Sleep -Milliseconds 60",
                    "$d = New-Object System.Windows.Forms.FolderBrowserDialog",
                    "$d.Description = '" + safeTitle + "'",
                    "$result = $d.ShowDialog($owner)",
                    "$owner.Close()",
                    "if($result -eq [System.Windows.Forms.DialogResult]::OK){ Write-Output $d.SelectedPath }"
            );
            return runPowerShellDialog(script);
        }
        throw new IllegalStateException("当前运行环境无图形界面，无法打开本地文件选择器");
    }

    public String pickFile(String title, List<String> extensions) {
        if (!GraphicsEnvironment.isHeadless()) {
            return runOnEdt(() -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle(title == null || title.isBlank() ? "选择文件" : title);
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                List<String> cleaned = normalizeExtensions(extensions);
                if (!cleaned.isEmpty()) {
                    chooser.setFileFilter(new FileNameExtensionFilter("Files", cleaned.toArray(new String[0])));
                }
                int result = chooser.showOpenDialog(null);
                if (result != JFileChooser.APPROVE_OPTION) {
                    return "";
                }
                File selected = chooser.getSelectedFile();
                return selected == null ? "" : selected.getAbsolutePath();
            });
        }
        if (isWindows()) {
            String safeTitle = escapeForPowerShell(title == null || title.isBlank() ? "Select File" : title);
            List<String> cleaned = normalizeExtensions(extensions);
            String filter = cleaned.isEmpty()
                    ? "All Files|*.*"
                    : "Target Files|" + cleaned.stream().map(ext -> "*." + ext).collect(Collectors.joining(";")) + "|All Files|*.*";
            String script = String.join(";",
                    "$OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)",
                    "Add-Type -AssemblyName System.Windows.Forms",
                    "Add-Type -AssemblyName System.Drawing",
                    "$owner = New-Object System.Windows.Forms.Form",
                    "$owner.TopMost = $true",
                    "$owner.ShowInTaskbar = $false",
                    "$owner.FormBorderStyle = [System.Windows.Forms.FormBorderStyle]::FixedToolWindow",
                    "$owner.StartPosition = [System.Windows.Forms.FormStartPosition]::CenterScreen",
                    "$owner.Size = New-Object System.Drawing.Size(1,1)",
                    "$owner.Opacity = 0.01",
                    "$owner.Show()",
                    "$owner.Activate()",
                    "$owner.BringToFront()",
                    "[System.Windows.Forms.Application]::DoEvents()",
                    "Start-Sleep -Milliseconds 60",
                    "$d = New-Object System.Windows.Forms.OpenFileDialog",
                    "$d.Title = '" + safeTitle + "'",
                    "$d.Filter = '" + escapeForPowerShell(filter) + "'",
                    "$result = $d.ShowDialog($owner)",
                    "$owner.Close()",
                    "if($result -eq [System.Windows.Forms.DialogResult]::OK){ Write-Output $d.FileName }"
            );
            return runPowerShellDialog(script);
        }
        throw new IllegalStateException("当前运行环境无图形界面，无法打开本地文件选择器");
    }

    private List<String> normalizeExtensions(List<String> extensions) {
        return extensions == null
                ? List.of()
                : extensions.stream()
                .map(ext -> ext == null ? "" : ext.trim().replaceFirst("^\\.", "").toLowerCase(Locale.ROOT))
                .filter(ext -> !ext.isBlank())
                .toList();
    }

    private String runPowerShellDialog(String script) {
        Process process;
        try {
            process = new ProcessBuilder("powershell.exe", "-NoProfile", "-STA", "-ExecutionPolicy", "Bypass", "-Command", script)
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException ex) {
            throw new IllegalStateException("启动 PowerShell 文件选择器失败", ex);
        }

        boolean finished;
        try {
            finished = process.waitFor(120, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("文件选择器被中断", ex);
        }

        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("文件选择器超时，请重试");
        }

        String output = readAll(process.getInputStream()).trim();
        if (process.exitValue() != 0) {
            throw new IllegalStateException("文件选择器调用失败: " + output);
        }
        return extractSelectedPath(output);
    }

    private String readAll(InputStream input) {
        try {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }

    private String extractSelectedPath(String output) {
        if (output == null || output.isBlank()) {
            return "";
        }
        String[] lines = output.replace("\r", "").split("\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i] == null ? "" : lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.matches("^[a-zA-Z]:\\\\.*") || line.startsWith("\\\\")) {
                return line;
            }
        }
        return "";
    }

    private boolean isWindows() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return osName.contains("win");
    }

    private String escapeForPowerShell(String text) {
        return text == null ? "" : text.replace("'", "''");
    }

    private String runOnEdt(DialogAction action) {
        AtomicReference<String> result = new AtomicReference<>("");
        AtomicReference<RuntimeException> error = new AtomicReference<>();
        Runnable task = () -> {
            try {
                result.set(action.run());
            } catch (RuntimeException ex) {
                error.set(ex);
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(task);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("文件选择器被中断", ex);
            } catch (InvocationTargetException ex) {
                throw new IllegalStateException("文件选择器调用失败", ex.getTargetException());
            }
        }

        if (error.get() != null) {
            throw error.get();
        }
        return result.get();
    }

    @FunctionalInterface
    private interface DialogAction {
        String run();
    }
}

