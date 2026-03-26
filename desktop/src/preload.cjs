const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("desktopBridge", {
  pickDirectory: () => ipcRenderer.invoke("dialog:pick-directory"),
  pickFile: (options) => ipcRenderer.invoke("dialog:pick-file", options || {})
});
